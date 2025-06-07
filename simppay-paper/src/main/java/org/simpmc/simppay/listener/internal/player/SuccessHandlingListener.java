package org.simpmc.simppay.listener.internal.player;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.CoinsConfig;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.data.card.CardPrice;
import org.simpmc.simppay.event.PaymentQueueSuccessEvent;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.model.detail.BankingDetail;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.SoundUtil;

import java.util.List;
import java.util.UUID;


// Handle all success event targeted to players

public class SuccessHandlingListener implements Listener {
    public SuccessHandlingListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void removeCaching(PaymentSuccessEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();

        plugin.getPaymentService().getPayments().remove(event.getPayment().getPaymentID());

        if (event.getPaymentType() == PaymentType.BANKING) {
            SPPlugin.getInstance().getPaymentService().clearPlayerBankCache(event.getPlayerUUID());
            Player player = Bukkit.getPlayer(event.getPlayerUUID());
            if (player != null) {
                player.updateInventory(); // update the fake qr map to normal item
            }

        }
    }

    // Dev: odd logic, success notification is sent regardless of the coin given is success or not :D
    @EventHandler
    public void notifyPlayer(PaymentSuccessEvent event) {
        // notify player
        SPPlugin plugin = SPPlugin.getInstance();
        MessageConfig config = ConfigManager.getInstance().getConfig(MessageConfig.class);
        String formattedAmount = String.format("%,.0f", event.getAmount());
        if (event.getPaymentType() == PaymentType.CARD) {
            if (event.isWrongPrice()) {
                MessageUtil.sendMessage(event.getPlayerUUID(), config.wrongPriceCard.replace("<amount>", formattedAmount));
            } else {
                MessageUtil.sendMessage(event.getPlayerUUID(), config.successPayment.replace("<amount>", formattedAmount));
            }
            SoundUtil.sendSound(event.getPlayerUUID(), config.soundEffect.get(PaymentStatus.SUCCESS).toSound());
        }
        if (event.getPaymentType() == PaymentType.BANKING) {
            MessageUtil.sendMessage(event.getPlayerUUID(), config.successPayment.replace("<amount>", formattedAmount));
            SoundUtil.sendSound(event.getPlayerUUID(), config.soundEffect.get(PaymentStatus.SUCCESS).toSound());
        }
    }

    @EventHandler
    public void giveCoins(PaymentSuccessEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();
        UUID playerUUID = event.getPlayerUUID();
        CoinsConfig coinsConfig = ConfigManager.getInstance().getConfig(CoinsConfig.class);
        long givenCoins = 0;
        if (event.getPaymentType() == PaymentType.CARD) {
            CardDetail cardDetail = (CardDetail) event.getPaymentDetail();
            long baseCoin = coinsConfig.cardToCoins.get(cardDetail.getPrice());
            if (baseCoin == 0) {
                return;
            }
            givenCoins = baseCoin + (long) (baseCoin * coinsConfig.getPromoRate());
        }
        if (event.getPaymentType() == PaymentType.BANKING) {
            BankingDetail bankDetail = (BankingDetail) event.getPaymentDetail();
            //  "- Số xu nhận được = Số xu tiêu chuẩn + Số xu được nhận thêm + (Số tiền nạp chuyển khoản / 1000) × Khuyến mãi)",
            //    "Trong đó:",
            //     "- Số xu tiêu chuẩn = (Số tiền nạp chuyển khoản / 1000) x Tỷ lệ tiêu chuẩn cho chuyển khoản",
            //     "- Số xu được nhận thêm = (Số tiền nạp chuyển khoản / 1000) × Tỷ lệ nhận thêm cho chuyển khoản",
            // Xu tiêu chuẩn sẽ được làm gốc để tính cho khuyến mãi và xu nhận thêm
            long baseCoin = (long) ((bankDetail.getAmount() / 1000) * coinsConfig.baseBankRate);
            long bonusCoin = (long) ((bankDetail.getAmount() / 1000) * coinsConfig.extraBankRate) + (long) ((bankDetail.getAmount() / 1000) * coinsConfig.getPromoRate());
            givenCoins = baseCoin + bonusCoin;
            if (baseCoin == 0 || baseCoin < 0) {
                return;
            }
        }
        long finalGivenCoins = givenCoins;
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            // TODO: Might need change to sync because some plugin will not support running async, for example: CoinsEngine
            plugin.getPaymentService().getHandlerRegistry().getCoinHandler().give(playerUUID, (int) finalGivenCoins);
        });
    }

    @EventHandler
    public void runCommand(PaymentSuccessEvent event) {
        // handle success
        SPPlugin plugin = SPPlugin.getInstance();
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
        MessageConfig config = ConfigManager.getInstance().getConfig(MessageConfig.class);

        if (event.getPaymentType() == PaymentType.CARD) {
            // TODO
            // run card commands
            if (event.isWrongPrice() && event.getPaymentDetail() instanceof CardDetail) {
                CardDetail cardDetail = (CardDetail) event.getPaymentDetail();
                // give real coins
                CardPrice price = cardDetail.getPrice();
                List<String> commands = plugin.getConfigManager().getConfig(CoinsConfig.class).cardToCommands.get(price);
                plugin.getFoliaLib().getScheduler().runLater(task -> {
                    commands.forEach(command -> { // TODO: Add support for player commands ? not sure if needed tbh
                        String parsed = PlaceholderAPI.setPlaceholders(player, command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                    });
                }, 1);
            }
        }
        if (event.getPaymentType() == PaymentType.BANKING) {
            // TODO: Add support for banking commands, havent figure out the logic for it yet
//            MessageUtil.sendMessage(player, config.successPayment.replace("<amount>", String.valueOf(event.getAmount())));
        }
    }

    @EventHandler
    public void notifyQueueSuccessPlayer(PaymentQueueSuccessEvent event) {

        MessageConfig config = ConfigManager.getInstance().getConfig(MessageConfig.class);
        // notify player
        if (event.getPaymentType() == PaymentType.CARD) {
            MessageUtil.sendMessage(event.getPlayerUUID(), config.pendingCard);
            SoundUtil.sendSound(event.getPlayerUUID(), config.soundEffect.get(PaymentStatus.PENDING).toSound());
        }
        if (event.getPaymentType() == PaymentType.BANKING) {
            // called when player receive the qr code and the task for checking api is running
            MessageUtil.sendMessage(event.getPlayerUUID(), config.pendingBank);
            SoundUtil.sendSound(event.getPlayerUUID(), config.soundEffect.get(PaymentStatus.PENDING).toSound());
        }

    }
}
