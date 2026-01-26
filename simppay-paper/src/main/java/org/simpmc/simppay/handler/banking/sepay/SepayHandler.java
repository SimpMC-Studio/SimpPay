package org.simpmc.simppay.handler.banking.sepay;

import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.banking.SepayConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.event.PaymentBankPromptEvent;
import org.simpmc.simppay.event.PaymentQueueSuccessEvent;
import org.simpmc.simppay.handler.BankHandler;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.handler.banking.sepay.data.BankData;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.BankingDetail;
import org.simpmc.simppay.model.detail.PaymentDetail;
import org.simpmc.simppay.service.BankCacheService;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.ReferenceCodeUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Sepay Banking Handler - Webhook-based
 * <p>
 * Handles manual bank transfer payments through Sepay webhooks.
 * Players transfer to a bank account and Sepay sends webhook when transaction occurs.
 * Uses remote QR API from qr.sepay.vn for QR code generation.
 */
@NoArgsConstructor
public class SepayHandler extends BankHandler {

    @Override
    public PaymentStatus processPayment(Payment payment) {
        // Sepay is for manual bank transfers - just display bank account info
        BankingDetail detail = (BankingDetail) payment.getDetail();
        SepayConfig config = ConfigManager.getInstance().getConfig(SepayConfig.class);

        if (config.webhookApiKey == null || config.webhookApiKey.equals("YOUR_WEBHOOK_API_KEY_HERE")) {
            MessageUtil.info("[Sepay-ProcessPayment] Webhook API key is not configured");
            return PaymentStatus.FAILED;
        }

        if (config.accountNumber == null || config.accountNumber.equals("YOUR_BANK_ACCOUNT_NUMBER")) {
            MessageUtil.info("[Sepay-ProcessPayment] Bank account number is not configured");
            return PaymentStatus.FAILED;
        }

        // Get BIN from cached bank data
        BankCacheService bankCache = SPPlugin.getService(BankCacheService.class);
        BankData bankData = bankCache.getBankByName(config.bankName);
        
        if (bankData == null) {
            MessageUtil.warn("[Sepay-ProcessPayment] Bank not found in cache: " + config.bankName);
            MessageUtil.warn("[Sepay-ProcessPayment] Using default BIN. Please check your bankName configuration.");
            return PaymentStatus.FAILED;
        }

        String bin = bankData.getBin();
        MessageUtil.debug("[Sepay-ProcessPayment] Resolved BIN: " + bin + " for bank: " + config.bankName);

        // Generate unique reference code with configured prefix
        String referenceCode = ReferenceCodeUtil.generateWithPrefix(config.descriptionPrefix);
        detail.setRefID(referenceCode);
        MessageUtil.debug("[Sepay-ProcessPayment] Generated reference code: " + referenceCode);

        // Fire queue success event (payment is now pending)
        Bukkit.getPluginManager().callEvent(new PaymentQueueSuccessEvent(payment));

        // Build remote QR API URL
        String qrImageUrl = buildQrImageUrl(
                config.accountNumber,
                config.bankName,
                (int) detail.getAmount(),
                referenceCode
        );
        MessageUtil.debug("[Sepay-ProcessPayment] QR Image URL: " + qrImageUrl);

        // Build banking data for display to player
        BankingData bankingData = BankingData.builder()
                .bin(bin)
                .playerUUID(payment.getPlayerUUID())
                .desc(referenceCode)
                .amount(detail.getAmount())
                .url(null) // No checkout URL for manual transfers
                .accountNumber(config.accountNumber)
                .qrString(null) // No longer using local QR string
                .qrImageUrl(qrImageUrl) // Use remote QR image URL
                .build();

        // Fire bank prompt event (show bank info to player)
        Bukkit.getPluginManager().callEvent(new PaymentBankPromptEvent(bankingData));

        MessageUtil.debug("[Sepay-ProcessPayment] Manual transfer initiated for " + detail.getAmount() + " VND");
        MessageUtil.debug("[Sepay-ProcessPayment] Reference: " + referenceCode + ", waiting for webhook...");
        return PaymentStatus.PENDING;
    }

    @Override
    public PaymentResult getTransactionResult(PaymentDetail detail) {
        // Webhooks handle transaction confirmation automatically
        // This method is called by polling systems - we just return PENDING
        // The webhook listener will update the payment status when transaction is received
        MessageUtil.debug("[Sepay-GetTransactionResult] Webhook-based system - returning PENDING");
        return new PaymentResult(PaymentStatus.PENDING, 0, null);
    }

    @Override
    public PaymentStatus cancel(Payment payment) {
        // Sepay doesn't have a cancel API for manual transfers
        // Just mark as cancelled locally
        MessageUtil.debug("[Sepay-Cancel] Manual transfer cancelled for " + payment.getDetail().getRefID());
        return PaymentStatus.CANCELLED;
    }

    /**
     * Builds the remote QR image URL from qr.sepay.vn
     *
     * @param accountNumber Bank account number
     * @param bankName Bank name (e.g., "Vietcombank")
     * @param amount Payment amount
     * @param description Payment description (reference code)
     * @return QR image URL
     */
    private String buildQrImageUrl(String accountNumber, String bankName, int amount, String description) {
        return String.format(
                "https://qr.sepay.vn/img?acc=%s&bank=%s&amount=%d&des=%s&template=qronly",
                URLEncoder.encode(accountNumber, StandardCharsets.UTF_8),
                URLEncoder.encode(bankName, StandardCharsets.UTF_8),
                amount,
                URLEncoder.encode(description, StandardCharsets.UTF_8)
        );
    }
}
