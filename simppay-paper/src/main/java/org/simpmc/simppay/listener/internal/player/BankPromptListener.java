package org.simpmc.simppay.listener.internal.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.event.PaymentBankPromptEvent;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.service.IService;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.qrcode.MapQR;
import org.simpmc.simppay.util.qrcode.vietqr.VietQr;

/**
 * Listener responsible for handling bank payment prompts
 */
public class BankPromptListener implements Listener, IService {
    private final SPPlugin plugin;

    public BankPromptListener(SPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void shutdown() {
        // Event handlers automatically unregistered on plugin disable
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
        String qrCode;
        if (bankingData.getQrString() != null) {
            qrCode = bankingData.getQrString();
        } else {
            qrCode = VietQr.getVietQr(
                    bankingData.bin,
                    bankingData.accountNumber,
                    String.valueOf(bankingData.amount),
                    bankingData.desc
            );
        }
        MessageUtil.debug("BankPrompt: " + qrCode);

        byte[] mapBytes = MapQR.encodeTextToMapBytes(qrCode);

        SPPlugin.getService(PaymentService.class).getPlayerBankQRCode().put(event.getPlayerUUID(), mapBytes);
        // PacketEvents
        // Forge a fake mapData
        MapQR.sendPacketQRMap(mapBytes, player);


    }
}
