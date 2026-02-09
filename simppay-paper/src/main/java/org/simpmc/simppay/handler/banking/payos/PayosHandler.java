package org.simpmc.simppay.handler.banking.payos;

import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.BankingConfig;
import org.simpmc.simppay.config.types.banking.PayosConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.bank.payos.PayosAdapter;
import org.simpmc.simppay.event.PaymentBankPromptEvent;
import org.simpmc.simppay.event.PaymentQueueSuccessEvent;
import org.simpmc.simppay.handler.BankHandler;
import org.simpmc.simppay.handler.banking.data.BankingData;
import org.simpmc.simppay.handler.banking.payos.data.PayosPayment;
import org.simpmc.simppay.handler.banking.payos.data.PayosResponse;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.BankingDetail;
import org.simpmc.simppay.model.detail.PaymentDetail;
import org.simpmc.simppay.service.OrderIDService;
import org.simpmc.simppay.util.GsonUtil;
import org.simpmc.simppay.util.HashUtils;
import org.simpmc.simppay.util.MessageUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@NoArgsConstructor
public class PayosHandler extends BankHandler {
    String RETURN_CANCEL_URl = "https://payos.vn";

    @Override
    public PaymentStatus processPayment(Payment payment) {
        // Create payment through payos and call queue success event, ref id should contain payos payment link id
        BankingDetail detail = (BankingDetail) payment.getDetail();

        PayosResponse request;
        try {
            request = requestTransaction(detail).get();
        } catch (InterruptedException | ExecutionException e) {
            MessageUtil.error("[PayOS-ProcessPayment] Failed to create payment request", e);
            return PaymentStatus.FAILED;
        }
        if (request == null || request.getData() == null) {
            MessageUtil.debug("[PayOS-ProcessPayment] Request is null");
            return PaymentStatus.FAILED;
        }
        if (PayosAdapter.getStatus(request.getData().getStatus()) == PaymentStatus.FAILED) {
            MessageUtil.debug("[PayOS-ProcessPayment]" + request);
            return PaymentStatus.FAILED;
        }
        // TODO: call PaymentBankPromptEvent with payment link and qrcode string
        // this mean success sent the payment to payos
        if (PayosAdapter.getStatus(request.getData().getStatus()) == PaymentStatus.PENDING) {
            MessageUtil.debug("[PayOS-ProcessPayment]" + request);
            String refID = request.getData().getPaymentLinkId();
            payment.getDetail().setRefID(refID);
            Bukkit.getPluginManager().callEvent(new PaymentQueueSuccessEvent(payment));

            BankingData bankData = BankingData.builder()
                    .bin(request.getData().getBin())
                    .playerUUID(payment.getPlayerUUID())
                    .desc(request.getData().getDescription())
                    .amount(request.getData().getAmount())
                    .url(request.getData().getCheckoutUrl())
                    .accountNumber(request.getData().getAccountNumber())
                    .qrString(request.getData().getQrCode())
                    .build();

            Bukkit.getPluginManager().callEvent(new PaymentBankPromptEvent(bankData));
            return PaymentStatus.PENDING;
        }
        MessageUtil.debug("[PayOS-ProcessPayment]" + request);
        // default to failed if other status codes
        return PaymentStatus.FAILED;
    }

    @Override
    public PaymentResult getTransactionResult(PaymentDetail detail) {
        PayosResponse res;
        try {
            res = getTransactionStatus(detail.getRefID()).get();
        } catch (ExecutionException | InterruptedException e) {
            MessageUtil.error("[PayOS-GetTransactionResult] Failed to get transaction status", e);
            return new PaymentResult(PaymentStatus.FAILED, 0, null);
        }
        if (res == null || res.getData() == null) {
            MessageUtil.debug("[PayOS-GetTransactionStatus] Data is null");
            return new PaymentResult(PaymentStatus.FAILED, 0, null);
        }
        if (Integer.valueOf(res.getCode()) == 231) {
            MessageUtil.debug("[PayOS-GetTransactionStatus] Payment id exist");
            MessageUtil.debug("[PayOS-GetTransactionStatus] Lỗi này xảy ra khi bạn reset config và mất file last_id.txt, hãy lên cổng payos và tìm lại id đơn hàng mới nhất và điền vào file ó");
            MessageUtil.debug("[PayOS-GetTransactionStatus]" + res);
            return new PaymentResult(PaymentStatus.EXIST, 0, null);
        }
        MessageUtil.debug("[PayOS-GetTransactionStatus]" + res);
        PaymentStatus paymentStatus = PayosAdapter.getStatus(res.getData().getStatus());
        return new PaymentResult(paymentStatus, (int) res.getData().getAmount(), res.getData().getCheckoutUrl());
    }

    @Override
    public PaymentStatus cancel(Payment payment) {
        try {
            PayosResponse response = cancel(payment.getDetail().getRefID()).get();

            if (response == null || response.getData() == null) {
                MessageUtil.debug("[PayOS-Cancel] Data is null");
                return PaymentStatus.FAILED;
            }
            MessageUtil.debug("[PayOS-Cancel]" + response);
            if (PayosAdapter.getStatus(response.getData().getStatus()) == PaymentStatus.CANCELLED) {
                return PaymentStatus.CANCELLED;
            }
        } catch (InterruptedException | ExecutionException e) {
            MessageUtil.error("[PayOS-Cancel] Failed to cancel payment", e);
            return PaymentStatus.FAILED;
        }
        return PaymentStatus.FAILED;
    }

    private CompletableFuture<PayosResponse> getTransactionStatus(String paymentID) {
        return CompletableFuture.supplyAsync(() -> {
            PayosConfig config = ConfigManager.getInstance().getConfig(PayosConfig.class);
            String base = "https://api-merchant.payos.vn/v2/payment-requests/{0}";
            String url = MessageFormat.format(base,
                    paymentID
            );
            try {
                String response = get(url, config);
                return GsonUtil.getGson().fromJson(response, PayosResponse.class);
            } catch (IOException e) {
                MessageUtil.warn("[PayOS-GetTransactionStatus] Network error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });

    }

    private CompletableFuture<PayosResponse> cancel(String paymentID) {

        return CompletableFuture.supplyAsync(() -> {
            PayosConfig config = ConfigManager.getInstance().getConfig(PayosConfig.class);
            String base = "https://api-merchant.payos.vn/v2/payment-requests/{0}/cancel";
            String url = MessageFormat.format(base,
                    paymentID
            );
            try {
                String response = post(url, config, "{\n" +
                        "    \"cancellationReason\": \"Changed my mind\"\n" +
                        "}");
                return GsonUtil.getGson().fromJson(response, PayosResponse.class);
            } catch (IOException e) {
                MessageUtil.warn("[PayOS-Cancel] Network error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<PayosResponse> requestTransaction(BankingDetail bank) {
        return CompletableFuture.supplyAsync(() -> {
            PayosConfig config = ConfigManager.getInstance().getConfig(PayosConfig.class);
            BankingConfig bankConfig = ConfigManager.getInstance().getConfig(BankingConfig.class);

            if (config.apiKey == null || config.clientId == null) {
                MessageUtil.info("[PayOS-RequestTransaction] API key or secret key is not set");
                return null;
            }
            String base = "https://api-merchant.payos.vn/v2/payment-requests";
            try {
                String orderid = String.valueOf(SPPlugin.getService(OrderIDService.class).getNextId());
                String valuetoBeHashed = MessageFormat.format("amount={0,number,#}&cancelUrl={1}&description={2}&orderCode={3}&returnUrl={4}",
                        bank.getAmount(),
                        RETURN_CANCEL_URl,
                        "payos",
                        orderid,
                        RETURN_CANCEL_URl);
                String hash = HashUtils.hmacSha256Hex(config.checksumKey, valuetoBeHashed);
                MessageUtil.debug("[PayOS-RequestTransaction] Hash: " + hash);
                PayosPayment payosPayment = PayosPayment.builder()
                        .amount(bank.getAmount())
                        .cancelUrl("https://payos.vn")
                        .returnUrl("https://payos.vn")
                        .description("payos")
                        .orderCode(Integer.parseInt(orderid))
                        .signature(hash)
                        .expiredAt((int) (System.currentTimeMillis() / 1000L + bankConfig.bankingTimeout)) // 5 minute
                        .build();

                String payload = GsonUtil.getGson().toJson(payosPayment, PayosPayment.class);
                MessageUtil.debug("[PayOS-RequestTransaction] Payload: " + payload);
                String response = post(base, config, payload);
                MessageUtil.debug("[PayOS-RequestTransaction] Response: " + response);

                return GsonUtil.getGson().fromJson(response, PayosResponse.class);

            } catch (Exception e) {
                MessageUtil.warn("[PayOS-RequestTransaction] Failed to create transaction: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private @NotNull String post(String base, PayosConfig config, String payload) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (new URL(base)).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("x-api-key", config.apiKey);
        connection.setRequestProperty("x-client-id", config.clientId);
        connection.setRequestProperty("x-partner-code", "simpmc");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Charset", "UTF-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);

        try (var outputStream = connection.getOutputStream()) {
            outputStream.write(payload.getBytes());
            outputStream.flush();
        }

        try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    private @NotNull String get(String base, PayosConfig config) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) (new URL(base)).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("x-api-key", config.apiKey);
        connection.setRequestProperty("x-client-id", config.clientId);
        connection.setRequestProperty("x-partner-code", "simpmc");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Charset", "UTF-8");
        connection.setConnectTimeout(7000);
        connection.setReadTimeout(7000);

        try (var reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            return reader.lines().collect(Collectors.joining());
        }
    }
}
