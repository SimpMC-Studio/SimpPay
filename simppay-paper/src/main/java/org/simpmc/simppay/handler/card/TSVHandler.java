package org.simpmc.simppay.handler.card;
// Herz đã làm phần này
import com.google.gson.JsonObject;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.card.ThesieuvietConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.data.card.thesieuviet.TSVCardAdapter;
import org.simpmc.simppay.event.PaymentQueueSuccessEvent;
import org.simpmc.simppay.handler.CardAdapter;
import org.simpmc.simppay.handler.PaymentHandler;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.model.detail.PaymentDetail;
import org.simpmc.simppay.util.HashUtils;
import org.simpmc.simppay.util.HttpUtils;
import org.simpmc.simppay.util.MessageUtil;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@NoArgsConstructor
public class TSVHandler implements PaymentHandler, CardAdapter {

    private static final String TSV_CREATE_URL = "https://api.thesieuviet.com/v1/transactions";
    private static final String TSV_GET_STATUS_URL = "https://api.thesieuviet.com/v1/transactions/{0}";

    @Override
    public PaymentStatus processPayment(Payment payment) {
        CardDetail detail = (CardDetail) payment.getDetail();

        JsonObject response;
        try {
            response = createTransaction(detail).get();
        } catch (InterruptedException | ExecutionException e) {
            MessageUtil.debug("[TSV-ProcessPayment] API request failed: " + e.getMessage());
            return PaymentStatus.FAILED;
        }

        if (response == null || !response.has("data")) {
            MessageUtil.debug("[TSV-ProcessPayment] Invalid API response");
            return PaymentStatus.FAILED;
        }

        JsonObject data = response.getAsJsonObject("data");
        String status = data.get("status").getAsString().toLowerCase();

        switch (status) {
            case "pending":
                String transactionId = data.get("transaction_id").getAsString();
                detail.setRefID(transactionId);
                Bukkit.getPluginManager().callEvent(new PaymentQueueSuccessEvent(payment));
                return PaymentStatus.PENDING;

            case "success":
                return PaymentStatus.SUCCESS;

            default:
                MessageUtil.debug("[TSV-ProcessPayment] Failed response: " + response);
                return PaymentStatus.FAILED;
        }
    }

    private CompletableFuture<JsonObject> createTransaction(CardDetail card) {
        return CompletableFuture.supplyAsync(() -> {
            ThesieuvietConfig config = ConfigManager.getInstance().getConfig(ThesieuvietConfig.class);
            if (!isConfigValid(config)) return null;

            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("pin", card.pin);
                payload.addProperty("serial", card.serial);
                payload.addProperty("type", adaptCardType(card.type));
                payload.addProperty("amount", TSVCardAdapter.getCardPriceValue(card.price));
                payload.addProperty("partner_id", config.partnerId);
                payload.addProperty("signature", generateSignature(card, config));

                JsonObject headers = new JsonObject();
                headers.addProperty("Authorization", "Bearer " + config.partnerKey);

                return HttpUtils.postJsonResponse(TSV_CREATE_URL, payload, headers);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private boolean isConfigValid(ThesieuvietConfig config) {
        return config.partnerId != null && !config.partnerId.isEmpty() &&
                config.partnerKey != null && !config.partnerKey.isEmpty();
    }

    private JsonObject validateResponse(JsonObject response) {
        if (response == null || !response.has("success")) {
            return null;
        }
        return response.get("success").getAsBoolean() ? response : null;
    }

    private String generateSignature(CardDetail card, ThesieuvietConfig config) {
        String raw = String.join("|",
                config.partnerId,
                card.pin,
                card.serial,
                String.valueOf(card.price),
                config.partnerKey
        );
        return HashUtils.sha256(raw);
    }

    @Override
    public PaymentResult getTransactionResult(PaymentDetail detail) {
        ThesieuvietConfig config = ConfigManager.getInstance().getConfig(ThesieuvietConfig.class);
        if (!isConfigValid(config)) return new PaymentResult(PaymentStatus.FAILED, 0, "Invalid config");

        String url = MessageFormat.format(TSV_GET_STATUS_URL, detail.getRefID());
        JsonObject headers = new JsonObject();
        headers.addProperty("Authorization", "Bearer " + config.partnerKey);

        JsonObject response = HttpUtils.getJsonResponse(url, headers);

        if (response == null || !response.has("data")) {
            return new PaymentResult(PaymentStatus.FAILED, 0, "Invalid API response");
        }

        JsonObject data = response.getAsJsonObject("data");
        PaymentStatus status = TSVCardAdapter.parseStatus(data.get("status").getAsString());
        String message = data.has("message") ? data.get("message").getAsString() : "No message";
        int amount = data.has("amount") ? data.get("amount").getAsInt() : 0;

        return new PaymentResult(status, amount, message);
    }

    @Override
    public PaymentStatus cancel(Payment payment) {
        throw new UnsupportedOperationException("TSV does not support payment cancellation");
    }

    @Override
    public String adaptCardType(CardType cardType) {
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
}