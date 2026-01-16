package org.simpmc.simppay.handler.banking.sepay;

import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.banking.SepayConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.event.PaymentBankPromptEvent;
import org.simpmc.simppay.event.PaymentQueueSuccessEvent;
import org.simpmc.simppay.handler.BankHandler;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.handler.banking.sepay.data.SepayResponse;
import org.simpmc.simppay.handler.banking.sepay.data.SepayTransaction;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.BankingDetail;
import org.simpmc.simppay.model.detail.PaymentDetail;
import org.simpmc.simppay.util.GsonUtil;
import org.simpmc.simppay.util.MessageUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Sepay Banking Handler - Phase 4
 * <p>
 * Handles manual bank transfer payments through Sepay API.
 * Players transfer to a bank account and Sepay API verifies the transaction.
 */
@NoArgsConstructor
public class SepayHandler extends BankHandler {

    @Override
    public PaymentStatus processPayment(Payment payment) {
        // Sepay is for manual bank transfers - just display bank account info
        BankingDetail detail = (BankingDetail) payment.getDetail();
        SepayConfig config = ConfigManager.getInstance().getConfig(SepayConfig.class);

        if (config.apiToken == null || config.apiToken.equals("YOUR_SEPAY_API_TOKEN_HERE")) {
            MessageUtil.info("[Sepay-ProcessPayment] API token is not configured");
            return PaymentStatus.FAILED;
        }

        if (config.accountNumber == null || config.accountNumber.equals("YOUR_BANK_ACCOUNT_NUMBER")) {
            MessageUtil.info("[Sepay-ProcessPayment] Bank account number is not configured");
            return PaymentStatus.FAILED;
        }

        // Use payment amount as reference (will be used to verify transaction later)
        String refID = String.valueOf(detail.getAmount());
        detail.setRefID(refID);

        // Fire queue success event (payment is now pending)
        Bukkit.getPluginManager().callEvent(new PaymentQueueSuccessEvent(payment));

        // Build banking data for display to player
        BankingData bankData = BankingData.builder()
                .bin(config.bin)
                .playerUUID(payment.getPlayerUUID())
                .desc("Transfer " + detail.getAmount() + " VND")
                .amount(detail.getAmount())
                .url(null) // No checkout URL for manual transfers
                .accountNumber(config.accountNumber)
                .qrString(generateVietQRString(config, (int) detail.getAmount()))
                .build();

        // Fire bank prompt event (show bank info to player)
        Bukkit.getPluginManager().callEvent(new PaymentBankPromptEvent(bankData));

        MessageUtil.debug("[Sepay-ProcessPayment] Manual transfer initiated for " + detail.getAmount() + " VND");
        return PaymentStatus.PENDING;
    }

    @Override
    public PaymentResult getTransactionResult(PaymentDetail detail) {
        // Query Sepay API to find matching transaction
        try {
            SepayResponse response = searchTransaction(detail.getRefID()).get();

            if (response == null || !response.isSuccess()) {
                MessageUtil.debug("[Sepay-GetTransactionResult] API call failed or no response");
                return new PaymentResult(PaymentStatus.PENDING, 0, null);
            }

            if (!response.hasTransactions()) {
                MessageUtil.debug("[Sepay-GetTransactionResult] No matching transaction found yet");
                return new PaymentResult(PaymentStatus.PENDING, 0, null);
            }

            // Find transaction with matching amount
            double expectedAmount = Double.parseDouble(detail.getRefID());
            for (SepayTransaction transaction : response.getTransactions()) {
                if (Math.abs(transaction.getAmountInValue() - expectedAmount) < 0.01) {
                    // Found matching transaction!
                    MessageUtil.debug("[Sepay-GetTransactionResult] Transaction found: " + transaction.getId());
                    return new PaymentResult(
                            PaymentStatus.SUCCESS,
                            (int) transaction.getAmountInValue(),
                            transaction.getTransactionContent()
                    );
                }
            }

            MessageUtil.debug("[Sepay-GetTransactionResult] Transaction with exact amount not found");
            return new PaymentResult(PaymentStatus.PENDING, 0, null);

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return new PaymentResult(PaymentStatus.FAILED, 0, null);
        }
    }

    @Override
    public PaymentStatus cancel(Payment payment) {
        // Sepay doesn't have a cancel API for manual transfers
        // Just mark as cancelled locally
        MessageUtil.debug("[Sepay-Cancel] Manual transfer cancelled for " + payment.getDetail().getRefID());
        return PaymentStatus.CANCELLED;
    }

    /**
     * Search for transactions via Sepay API
     *
     * @param amountStr Expected payment amount as string
     * @return CompletableFuture with SepayResponse
     */
    private CompletableFuture<SepayResponse> searchTransaction(String amountStr) {
        return CompletableFuture.supplyAsync(() -> {
            SepayConfig config = ConfigManager.getInstance().getConfig(SepayConfig.class);

            try {
                // Build query URL with filters
                String baseUrl = "https://my.sepay.vn/userapi/transactions/list";
                String url = MessageFormat.format(
                        "{0}?account_number={1}&amount_in={2}&limit=10",
                        baseUrl,
                        URLEncoder.encode(config.accountNumber, StandardCharsets.UTF_8),
                        URLEncoder.encode(amountStr, StandardCharsets.UTF_8)
                );

                MessageUtil.debug("[Sepay-API] Querying: " + url);

                String response = get(url, config);
                MessageUtil.debug("[Sepay-API] Response: " + response);

                return GsonUtil.getGson().fromJson(response, SepayResponse.class);

            } catch (IOException e) {
                MessageUtil.debug("[Sepay-API] Error: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Generates VietQR compatible string for QR code display
     *
     * @param config Sepay configuration
     * @param amount Payment amount
     * @return QR code string
     */
    private String generateVietQRString(SepayConfig config, int amount) {
        // VietQR format: BIN|ACCOUNT_NUMBER|AMOUNT|DESCRIPTION
        return MessageFormat.format(
                "{0}|{1}|{2}|SimpPay Payment {3}",
                config.bin,
                config.accountNumber,
                amount,
                amount
        );
    }

    /**
     * Makes GET request to Sepay API with authentication
     *
     * @param url    API endpoint URL
     * @param config Sepay configuration
     * @return Response body
     * @throws IOException If request fails
     */
    private @NotNull String get(String url, SepayConfig config) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + config.apiToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return reader.lines().collect(Collectors.joining());
        }
    }
}
