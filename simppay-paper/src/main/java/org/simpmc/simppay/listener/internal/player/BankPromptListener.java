package org.simpmc.simppay.listener.internal.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.map.MapPalette;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.event.PaymentBankPromptEvent;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.MessageUtil;
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

public class BankPromptListener implements Listener {

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

        // Sending packet map to player

        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        if (player == null) {
            return;
        }

        // Priority: qrImageUrl (remote API) > qrString (local generation)
        if (bankingData.getQrImageUrl() != null) {
            // Fetch QR image from remote API asynchronously
            fetchAndDisplayQRImage(bankingData.getQrImageUrl(), player, event.getPlayerUUID());
        } else {
            MessageUtil.debug("[BankPrompt] No QR data available for player: " + player.getName());
        }
    }

    /**
     * Fetch QR image from remote URL and display it to the player
     */
    private void fetchAndDisplayQRImage(String imageUrl, Player player, java.util.UUID playerUUID) {
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

                // Convert image to map bytes (128x128)
                byte[] mapBytes = convertImageToMapBytes(image);

                // Store and send to player on main thread
                SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, sendTask -> {
                    SPPlugin.getService(PaymentService.class).getPlayerBankQRCode().put(playerUUID, mapBytes);
                    MapQR.sendPacketQRMap(mapBytes, player);
                    MessageUtil.debug("[BankPrompt] Sent remote QR image to player: " + player.getName());
                });

            } catch (Exception e) {
                MessageUtil.warn("[BankPrompt] Error fetching QR image: " + e.getMessage());
            }
        });
    }

    /**
     * Convert a BufferedImage to Minecraft map bytes (128x128)
     * Scales the image if necessary and maps colors to the Minecraft palette
     */
    private byte[] convertImageToMapBytes(BufferedImage original) {
        final int TARGET = 128;
        
        // Scale image to 128x128 if needed
        BufferedImage scaled;
        if (original.getWidth() != TARGET || original.getHeight() != TARGET) {
            scaled = new BufferedImage(TARGET, TARGET, BufferedImage.TYPE_INT_ARGB);
            var g = scaled.createGraphics();
            g.drawImage(original, 0, 0, TARGET, TARGET, null);
            g.dispose();
        } else {
            scaled = original;
        }

        // Convert to map palette bytes
        byte[] mapBytes = new byte[TARGET * TARGET];
        Arrays.fill(mapBytes, (byte) 0);

        for (int y = 0; y < TARGET; y++) {
            for (int x = 0; x < TARGET; x++) {
                int rgb = scaled.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int a = (rgb >> 24) & 0xFF;

                // If transparent, use white
                if (a < 128) {
                    r = 255;
                    g = 255;
                    b = 255;
                }

                //noinspection deprecation
                mapBytes[x + y * TARGET] = MapPalette.matchColor(r, g, b);
            }
        }

        return mapBytes;
    }
}
