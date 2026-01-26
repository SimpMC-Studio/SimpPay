package org.simpmc.simppay.listener.internal.payment;

import io.papermc.paper.util.Tick;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.BankingConfig;
import org.simpmc.simppay.config.types.MainConfig;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.event.PaymentFailedEvent;
import org.simpmc.simppay.event.PaymentQueueSuccessEvent;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.SoundUtil;

import java.time.Duration;

public class PaymentHandlingListener implements Listener {
    public PaymentHandlingListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onFailedPayment(PaymentFailedEvent event) {
        MessageConfig messageConfig = ConfigManager.getInstance().getConfig(MessageConfig.class);
        Player player = Bukkit.getPlayer(event.getPlayerUUID());

        // Log failure with payment details for troubleshooting
        String playerName = player != null ? player.getName() : "Unknown";
        MessageUtil.error("[PaymentFailed] Player: " + playerName +
                " | Amount: " + event.getPayment().getDetail().getAmount() +
                " | Type: " + event.getPayment().getPaymentType() +
                " | PaymentID: " + event.getPayment().getPaymentID());

        MessageUtil.sendMessage(player, messageConfig.failedCard);
        SoundUtil.sendSound(player, messageConfig.soundEffect.get(PaymentStatus.FAILED).toSound());
    }

    @EventHandler
    public void paymentQueue(PaymentQueueSuccessEvent event) {

        if (event.getPaymentType() == PaymentType.CARD) {
            addTaskChecking(event);
        }
        if (event.getPaymentType() == PaymentType.BANKING) {
            addTaskChecking(event);
        }
    }


    private void addTaskChecking(PaymentQueueSuccessEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();
        long interval = ConfigManager.getInstance().getConfig(MainConfig.class).intervalApiCall;
        int bankingTimeout = ConfigManager.getInstance().getConfig(BankingConfig.class).bankingTimeout;
        plugin.getFoliaLib().getScheduler().runTimerAsync(task -> {
            // self expires after configured timeout
            if (System.currentTimeMillis() < event.getPayment().getCreatedAt().getTime() + Duration.ofMinutes(bankingTimeout).toMillis()) {
                // if payment is created less than timeout minutes ago, continue checking
            } else {
                // if payment is created more than timeout minutes ago, cancel the task
                Player player = Bukkit.getPlayer(event.getPayment().getPlayerUUID());
                String playerName = player != null ? player.getName() : "Unknown";
                MessageUtil.error("[PaymentPoller] Payment expired | Player: " + playerName +
                        " | Amount: " + event.getPayment().getDetail().getAmount() +
                        " | Type: " + event.getPaymentType() +
                        " | PaymentID: " + event.getPaymentID() +
                        " | Timeout: " + bankingTimeout + " minutes");
                callEventSync(new PaymentFailedEvent(event.getPayment()));
                SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                task.cancel();
                return;
            }

            // check if payment is still in pollingPayments
            if (!SPPlugin.getService(PaymentService.class).getPollingPayments().containsKey(event.getPaymentID())) {
                MessageUtil.debug("[PaymentPoller] Payment removed from polling queue | PaymentID: " + event.getPaymentID());
                task.cancel();
                return;
            }
            // check card status
            PaymentStatus status = null;
            PaymentResult result = null;
            if (event.getPaymentType() == PaymentType.CARD) {
                result = SPPlugin.getService(PaymentService.class).getHandlerRegistry().getCardHandler().getTransactionResult(event.getPaymentDetail());
                status = SPPlugin.getService(PaymentService.class).getHandlerRegistry().getCardHandler().getTransactionResult(event.getPaymentDetail()).getStatus();
            }
            if (event.getPaymentType() == PaymentType.BANKING) {
                // check banking status
                result = SPPlugin.getService(PaymentService.class).getHandlerRegistry().getBankHandler().getTransactionResult(event.getPaymentDetail());
                status = SPPlugin.getService(PaymentService.class).getHandlerRegistry().getBankHandler().getTransactionResult(event.getPaymentDetail()).getStatus();
            }

            switch (status) {
                case SUCCESS -> {
                    // handle success
                    // TODO: get actual amount then set into trueamount for card, the true amount should be given in the returned json
                    callEventSync(new PaymentSuccessEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
                case FAILED -> {
                    // handle failed
                    Player failedPlayer = Bukkit.getPlayer(event.getPayment().getPlayerUUID());
                    String failedPlayerName = failedPlayer != null ? failedPlayer.getName() : "Unknown";
                    MessageUtil.error("[PaymentPoller] Payment failed | Player: " + failedPlayerName +
                            " | Amount: " + event.getPayment().getDetail().getAmount() +
                            " | Type: " + event.getPaymentType() +
                            " | PaymentID: " + event.getPaymentID() +
                            " | Message: " + (result != null ? result.getMessage() : "No message"));
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    if (event.getPaymentType() == PaymentType.BANKING) {
                        SPPlugin.getService(PaymentService.class).cancelBankPayment(event.getPayment().getPlayerUUID());
                    }
                    task.cancel();
                }
                case PENDING -> {
                    // do nothing, wait for next check
                }
                case WRONG_PRICE -> {
                    // handle invalid
                    CardDetail detail = (CardDetail) event.getPaymentDetail().setAmount(result.getAmount());
                    callEventSync(new PaymentSuccessEvent(event.getPayment().setDetail(detail), true));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
                case null -> {
                    Player nullPlayer = Bukkit.getPlayer(event.getPayment().getPlayerUUID());
                    String nullPlayerName = nullPlayer != null ? nullPlayer.getName() : "Unknown";
                    MessageUtil.error("[PaymentPoller] Null status received | Player: " + nullPlayerName +
                            " | Amount: " + event.getPayment().getDetail().getAmount() +
                            " | Type: " + event.getPaymentType() +
                            " | PaymentID: " + event.getPaymentID());
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }

                // Bank Zone
                case INVALID -> {
                    Player invalidPlayer = Bukkit.getPlayer(event.getPayment().getPlayerUUID());
                    String invalidPlayerName = invalidPlayer != null ? invalidPlayer.getName() : "Unknown";
                    MessageUtil.error("[PaymentPoller] Invalid payment | Player: " + invalidPlayerName +
                            " | Amount: " + event.getPayment().getDetail().getAmount() +
                            " | Type: " + event.getPaymentType() +
                            " | PaymentID: " + event.getPaymentID());
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
                case EXIST -> {
                    Player existPlayer = Bukkit.getPlayer(event.getPayment().getPlayerUUID());
                    String existPlayerName = existPlayer != null ? existPlayer.getName() : "Unknown";
                    MessageUtil.error("[PaymentPoller] Payment already exists on API | Player: " + existPlayerName +
                            " | Amount: " + event.getPayment().getDetail().getAmount() +
                            " | Type: " + event.getPaymentType() +
                            " | PaymentID: " + event.getPaymentID());
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
                case EXPIRED -> {
                    Player expiredPlayer = Bukkit.getPlayer(event.getPayment().getPlayerUUID());
                    String expiredPlayerName = expiredPlayer != null ? expiredPlayer.getName() : "Unknown";
                    MessageUtil.error("[PaymentPoller] Payment expired on API | Player: " + expiredPlayerName +
                            " | Amount: " + event.getPayment().getDetail().getAmount() +
                            " | Type: " + event.getPaymentType() +
                            " | PaymentID: " + event.getPaymentID());
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
                case CANCELLED -> {
                    Player cancelledPlayer = Bukkit.getPlayer(event.getPayment().getPlayerUUID());
                    String cancelledPlayerName = cancelledPlayer != null ? cancelledPlayer.getName() : "Unknown";
                    MessageUtil.error("[PaymentPoller] Payment cancelled on API | Player: " + cancelledPlayerName +
                            " | Amount: " + event.getPayment().getDetail().getAmount() +
                            " | Type: " + event.getPaymentType() +
                            " | PaymentID: " + event.getPaymentID());
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
            }
        }, 1L, Tick.tick().fromDuration(Duration.ofSeconds(interval)));

        SPPlugin.getService(PaymentService.class).getPollingPayments().putIfAbsent(event.getPaymentID(), event.getPayment());


    }

    private void callEventSync(Event event) {
        SPPlugin plugin = SPPlugin.getInstance();
        plugin.getFoliaLib().getScheduler().runNextTick(wrappedTask -> Bukkit.getPluginManager().callEvent(event));
    }


}
