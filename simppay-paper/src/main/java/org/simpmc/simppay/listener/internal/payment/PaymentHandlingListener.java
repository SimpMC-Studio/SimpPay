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
import org.simpmc.simppay.service.IService;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.SoundUtil;

import java.time.Duration;

/**
 * Listener responsible for handling payment success/failure events
 */
public class PaymentHandlingListener implements Listener, IService {
    private final SPPlugin plugin;

    public PaymentHandlingListener(SPPlugin plugin) {
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
    public void onFailedPayment(PaymentFailedEvent event) {
        MessageConfig messageConfig = ConfigManager.getInstance().getConfig(MessageConfig.class);
        Player player = Bukkit.getPlayer(event.getPlayerUUID());
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
            // self expires after 5 minutes
            if (System.currentTimeMillis() < event.getPayment().getCreatedAt().getTime() + Duration.ofMinutes(bankingTimeout).toMillis()) {
                // if payment is created less than 5 minutes ago, continue checking
            } else {
                // if payment is created more than 5 minutes ago, cancel the task
                MessageUtil.debug("[Payment-Poller] Payment created more than 5 minutes ago, cancelling task");
                callEventSync(new PaymentFailedEvent(event.getPayment()));
                SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                task.cancel();
                return;
            }

            // check if payment is still in pollingPayments
            if (!SPPlugin.getService(PaymentService.class).getPollingPayments().containsKey(event.getPaymentID())) {
                MessageUtil.debug("[Payment-Poller] Payment is not in pollingPayments, cancelling task");
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
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    MessageUtil.debug("[Payment-Poller] Payment status is null");
                    task.cancel();
                }

                // Bank Zone
                case INVALID -> {
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
                case EXIST -> {
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    MessageUtil.debug("[Payment-Poller] Payment alrady exist on payment api");
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
                case EXPIRED -> {
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    MessageUtil.debug("[Payment-Poller] Payment expired on payment api");
                    SPPlugin.getService(PaymentService.class).getPollingPayments().remove(event.getPayment().getPaymentID());
                    task.cancel();
                }
                case CANCELLED -> {
                    callEventSync(new PaymentFailedEvent(event.getPayment()));
                    MessageUtil.debug("[Payment-Poller] Payment cancelled on payment api");
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
