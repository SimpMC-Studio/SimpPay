package org.simpmc.simppay.handler.card;

import com.google.gson.*;
import lombok.NoArgsConstructor;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.card.ThesieuvietConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.data.card.thesieuviet.TSVCardAdapter;
import org.simpmc.simppay.handler.CardAdapter;
import org.simpmc.simppay.handler.PaymentHandler;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.model.detail.PaymentDetail;
import org.simpmc.simppay.util.HashUtils;
import org.simpmc.simppay.util.HttpUtils;
import org.simpmc.simppay.util.MessageUtil;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@NoArgsConstructor
public class TSVHandler implements PaymentHandler, CardAdapter {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(TSVResponse.class, new TSVResponseDeserializer())
            .create();

    private static final String TSV_CREATE_URL = "https://thesieuviet.net/chargingws/v2";
    private static final String TSV_GET_STATUS_URL = "https://thesieuviet.net/chargingws/v2";

    private static class TSVRequest {
        private final String telco;
        private final String code;
        private final String serial;
        private final int amount;
        private final String request_id;
        private final String partner_id;
        private final String sign;
        private final String command;

        public TSVRequest(CardDetail card, ThesieuvietConfig config, String requestId, String command) {
            this.telco = adaptCardTypeStatic(card.type);
            this.code = card.pin;
            this.serial = card.serial;
            this.amount = TSVCardAdapter.getCardPriceValue(card.price);
            this.request_id = requestId;
            this.partner_id = config.partnerId;
            this.sign = generateSignatureStatic(card, config);
            this.command = command;
        }
    }

    private static class TSVResponse {
        int status;
        String message;
        String request_id;
        int declared_value;
        int value;
        int amount;
        String code;
        String serial;
        String telco;
        String trans_id;
        String callback_sign;
    }

    private static class TSVResponseDeserializer implements JsonDeserializer<TSVResponse> {
        @Override
        public TSVResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return GSON.fromJson(json, TSVResponse.class);
        }
    }

    @Override
    public PaymentStatus processPayment(Payment payment) {
        CardDetail detail = (CardDetail) payment.getDetail();

        try {
            TSVResponse response = createTransaction(detail, payment.getPaymentID().toString(), "charging").get(); // Sửa getId() thành getPaymentID().toString()
            if (response == null) {
                logDebug("Invalid API response", response);
                return PaymentStatus.FAILED;
            }

            return TSVCardAdapter.parseStatus(response.status);

        } catch (InterruptedException | ExecutionException e) {
            logError("API request failed", e);
            return PaymentStatus.FAILED;
        }
    }

    private CompletableFuture<TSVResponse> createTransaction(CardDetail card, String requestId, String command) {
        return CompletableFuture.supplyAsync(() -> {
            ThesieuvietConfig config = ConfigManager.getInstance().getConfig(ThesieuvietConfig.class);
            if (!isConfigValid(config)) {
                logDebug("Invalid configuration");
                return null;
            }

            try {
                TSVRequest request = new TSVRequest(card, config, requestId, command);
                JsonObject headers = new JsonObject();
                headers.addProperty("Content-Type", "application/json");

                JsonObject response = HttpUtils.postJsonResponse(
                        TSV_CREATE_URL,
                        GSON.toJsonTree(request).getAsJsonObject(),
                        headers
                );

                return response != null ? GSON.fromJson(response, TSVResponse.class) : null;
            } catch (Exception e) {
                logError("Create transaction error", e);
                return null;
            }
        });
    }

    @Override
    public PaymentResult getTransactionResult(PaymentDetail detail) {
        ThesieuvietConfig config = ConfigManager.getInstance().getConfig(ThesieuvietConfig.class);
        if (!isConfigValid(config)) {
            return new PaymentResult(PaymentStatus.FAILED, 0, "Invalid config");
        }

        try {
            String url = TSV_GET_STATUS_URL;
            TSVRequest request = new TSVRequest(
                    (CardDetail) detail,
                    config,
                    detail.getRefID(),
                    "check"
            );
            JsonObject headers = new JsonObject();
            headers.addProperty("Content-Type", "application/json");

            JsonObject response = HttpUtils.postJsonResponse(url, GSON.toJsonTree(request).getAsJsonObject(), headers);
            if (response == null) {
                return new PaymentResult(PaymentStatus.FAILED, 0, "Empty API response");
            }

            TSVResponse statusResponse = GSON.fromJson(response, TSVResponse.class);
            return buildPaymentResult(statusResponse);

        } catch (Exception e) {
            logError("Get transaction result error", e);
            return new PaymentResult(PaymentStatus.FAILED, 0, "API Error: " + e.getMessage());
        }
    }

    private PaymentResult buildPaymentResult(TSVResponse response) {
        if (response == null) {
            return new PaymentResult(PaymentStatus.FAILED, 0, "Invalid response format");
        }

        return new PaymentResult(
                TSVCardAdapter.parseStatus(response.status),
                response.amount,
                response.message != null ? response.message : "No message"
        );
    }

    private boolean isConfigValid(ThesieuvietConfig config) {
        return config != null &&
                config.partnerId != null && !config.partnerId.isEmpty() &&
                config.partnerKey != null && !config.partnerKey.isEmpty();
    }

    private static String generateSignatureStatic(CardDetail card, ThesieuvietConfig config) {
        String raw = config.partnerKey + card.pin + card.serial;
        return HashUtils.md5Hash(raw); 
    }

    private static String adaptCardTypeStatic(CardType cardType) {
        switch (cardType) {
            case VIETTEL: return "VIETTEL";
            case MOBIFONE: return "MOBIFONE";
            case VINAPHONE: return "VINAPHONE";
            case VIETNAMOBILE: return "VIETNAMOBILE";
            case GARENA: return "GARENA";
            case ZING: return "ZING";
            case VCOIN: return "VCOIN";
            case GATE: return "GATE";
            default: return "UNKNOWN";
        }
    }

    private void logDebug(String message) {
        MessageUtil.debug("[TSV] " + message);
    }

    private void logDebug(String context, Object data) {
        MessageUtil.debug("[TSV] " + context + ": " + GSON.toJson(data));
    }

    private void logError(String message, Throwable e) {
        MessageUtil.debug("[TSV] ERROR - " + message + ": " + e.getMessage());
    }

    @Override
    public String adaptCardType(CardType cardType) {
        return adaptCardTypeStatic(cardType);
    }

    @Override
    public PaymentStatus cancel(Payment payment) {
        throw new UnsupportedOperationException("TSV does not support payment cancellation");
    }
}