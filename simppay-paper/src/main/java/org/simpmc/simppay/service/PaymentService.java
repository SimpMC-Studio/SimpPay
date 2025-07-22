package org.simpmc.simppay.service;

import lombok.Getter;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.BankingConfig;
import org.simpmc.simppay.config.types.CardConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.handler.HandlerRegistry;
import org.simpmc.simppay.handler.data.BankAPI;
import org.simpmc.simppay.handler.data.CardAPI;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.util.MessageUtil;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class PaymentService implements IService {


    private final ConcurrentHashMap<UUID, Payment> pollingPayments = new ConcurrentHashMap<>(); // payment id is key
    private HandlerRegistry handlerRegistry;
    private final ConcurrentHashMap<UUID, Payment> payments = new ConcurrentHashMap<>(); // payment id is key
    private final ConcurrentHashMap<UUID, UUID> playerBankingSessionPayment = new ConcurrentHashMap<>(); // Store player uuid and payment id
    private final ConcurrentHashMap<UUID, byte[]> playerBankQRCode = new ConcurrentHashMap<>(); // Store player uuid and VietQR map bytew
    @Override
    public void setup() {
        handlerRegistry = new HandlerRegistry();
    }

    @Override
    public void shutdown() {

    }

    // use for storing data and pulling data out of the db later on
    public static BankAPI getBankAPI() {
        BankingConfig bankingConfig = ConfigManager.getInstance().getConfig(BankingConfig.class);
        return bankingConfig.bankApi;
    }

    public static CardAPI getCardAPI() {
        CardConfig cardConfig = ConfigManager.getInstance().getConfig(CardConfig.class);
        return cardConfig.cardApi;
    }

    public PaymentStatus sendCard(Payment payment) {
        PaymentStatus status = handlerRegistry.getCardHandler().processPayment(payment);
        if (status == PaymentStatus.EXIST) {
            return status;
        }
        payments.putIfAbsent(payment.getPaymentID(), payment);
        return status;
    }

    public PaymentStatus sendBank(Payment payment) {

        PaymentStatus status = handlerRegistry.getBankHandler().processPayment(payment);
        if (status == PaymentStatus.EXIST) {
            return status;
        }
        payments.putIfAbsent(payment.getPaymentID(), payment);
        return status;
    }

    public void clearPlayerBankCache(UUID playerUUID) {
        playerBankQRCode.remove(playerUUID);
        playerBankingSessionPayment.remove(playerUUID);
    }

    public void removePlayerQRSession(UUID playerUUID) {
        UUID paymentID = playerBankingSessionPayment.get(playerUUID);
        int retryCount = 0;
        boolean cancelled = false;
        if (paymentID == null) {
            MessageUtil.debug("[PaymentService-Cancel] No payment found for " + playerUUID);
            return;
        }
        while (retryCount < 5 && !cancelled) {
            PaymentStatus status = handlerRegistry.getBankHandler().cancel(payments.get(paymentID)); // call to cancel payment

            if (status == PaymentStatus.CANCELLED) {
                MessageUtil.debug("[PaymentService-Cancel] " + payments.get(paymentID));
                cancelled = true;
            } else {
                MessageUtil.debug("[PaymentService-Cancel] " + payments.get(paymentID) + " failed to cancel, retrying...");
                retryCount++;
            }
        }

        if (!cancelled) {
            SPPlugin.getInstance().getLogger().info("[PaymentService-Cancel] Max retries reached for " + payments.get(paymentID));
            return;
        }

        payments.remove(paymentID); // remove payment from existing payment on the server
        pollingPayments.remove(paymentID); // remove payment from polling payments
        playerBankingSessionPayment.remove(playerUUID);
        playerBankQRCode.remove(playerUUID);
    }


}
