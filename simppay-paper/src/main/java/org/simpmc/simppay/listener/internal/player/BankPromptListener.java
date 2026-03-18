package org.simpmc.simppay.listener.internal.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.BankingConfig;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.event.PaymentBankPromptEvent;
import org.simpmc.simppay.event.PaymentFailedEvent;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.qrcode.ItemFrameQR;
import org.simpmc.simppay.util.qrcode.MapQR;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BankPromptListener implements Listener {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ConcurrentHashMap<UUID, ItemFrameQR> activeItemFrames = new ConcurrentHashMap<>();

    public BankPromptListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void paymentPrompt(PaymentBankPromptEvent event) {
        MessageConfig config = ConfigManager.getInstance().getConfig(MessageConfig.class);
        BankingData bankingData = event.getBankingData();
        if (bankingData.getUrl() != null) {
            MessageUtil.sendMessage(event.getPlayerUUID(), config.promptPaymentLink.replace("<link>", bankingData.getUrl()));
        }

        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            return;
        }

        if (bankingData.getQrImageUrl() != null) {
            fetchAndDisplayQRImage(bankingData.getQrImageUrl(), player, event.getPlayerUUID());
        } else {
            MessageUtil.debug("[BankPrompt] No QR data available for player: " + player.getName());
        }
    }

    private void fetchAndDisplayQRImage(String imageUrl, Player player, UUID playerUUID) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(imageUrl))
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();

                HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() != 200) {
                    MessageUtil.warn("[BankPrompt] Failed to fetch QR image: HTTP " + response.statusCode());
                    return;
                }

                BufferedImage image = ImageIO.read(response.body());
                if (image == null) {
                    MessageUtil.warn("[BankPrompt] Failed to decode QR image from URL: " + imageUrl);
                    return;
                }

                byte[] mapBytes = convertImageToMapBytes(image);

                SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, sendTask -> {
                    BankingConfig bankingConfig = ConfigManager.getInstance().getConfig(BankingConfig.class);
                    SPPlugin.getService(PaymentService.class).getPlayerBankQRCode().put(playerUUID, mapBytes);

                    if (bankingConfig.showQrAsItemFrame) {
                        // Spawn a fake item frame entity visible only to this player
                        ItemFrameQR frame = new ItemFrameQR(player, mapBytes);
                        activeItemFrames.put(playerUUID, frame);
                        MessageUtil.debug("[BankPrompt] Spawned item frame QR for player: " + player.getName());
                    } else {
                        // Show QR map in player's hand (existing behaviour)
                        MapQR.sendPacketQRMap(mapBytes, player);
                        MessageUtil.debug("[BankPrompt] Sent hand QR map to player: " + player.getName());
                    }
                });

            } catch (Exception e) {
                MessageUtil.warn("[BankPrompt] Error fetching QR image: " + e.getMessage());
            }
        });
    }

    /**
     * Remove and destroy the item frame QR for a player.
     * Also restores the player's hand slot if not using item frame mode.
     */
    public static void removeItemFrame(UUID playerUUID) {
        ItemFrameQR frame = activeItemFrames.remove(playerUUID);
        if (frame != null) {
            frame.destroy();
        }
    }

    @EventHandler
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        if (event.getPaymentType() == PaymentType.BANKING) {
            removeItemFrame(event.getPlayerUUID());
        }
    }

    @EventHandler
    public void onPaymentFailed(PaymentFailedEvent event) {
        if (event.getPaymentType() == PaymentType.BANKING) {
            removeItemFrame(event.getPlayerUUID());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        removeItemFrame(event.getPlayer().getUniqueId());
    }

    /**
     * Re-send QR map when player closes their inventory.
     * Inventory close triggers a server inventory sync that overwrites the fake map slot,
     * so we re-send the QR packet to keep it visible.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID playerUUID = player.getUniqueId();
        BankingConfig bankingConfig = ConfigManager.getInstance().getConfig(BankingConfig.class);
        if (bankingConfig.showQrAsItemFrame) return; // item frame mode doesn't need resend
        if (!SPPlugin.getService(PaymentService.class).getPlayerBankingSessionPayment().containsKey(playerUUID)) return;
        byte[] mapBytes = SPPlugin.getService(PaymentService.class).getPlayerBankQRCode().get(playerUUID);
        if (mapBytes == null) return;
        // Schedule after 1 tick so the server sync completes before we re-send
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, sendTask ->
                MapQR.sendPacketQRMap(mapBytes, player)
        );
    }

    /**
     * Convert a BufferedImage to Minecraft map bytes (128x128).
     */
    @SuppressWarnings("deprecation")
    private byte[] convertImageToMapBytes(BufferedImage original) {
        final int TARGET = 128;

        BufferedImage scaled;
        if (original.getWidth() != TARGET || original.getHeight() != TARGET) {
            scaled = new BufferedImage(TARGET, TARGET, BufferedImage.TYPE_INT_ARGB);
            var g = scaled.createGraphics();
            g.drawImage(original, 0, 0, TARGET, TARGET, null);
            g.dispose();
        } else {
            scaled = original;
        }

        byte[] mapBytes = new byte[TARGET * TARGET];
        Arrays.fill(mapBytes, (byte) 0);

        for (int y = 0; y < TARGET; y++) {
            for (int x = 0; x < TARGET; x++) {
                int rgb = scaled.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int a = (rgb >> 24) & 0xFF;

                if (a < 128) {
                    r = 255;
                    g = 255;
                    b = 255;
                }

                mapBytes[x + y * TARGET] = org.bukkit.map.MapPalette.matchColor(r, g, b);
            }
        }

        return mapBytes;
    }
}
