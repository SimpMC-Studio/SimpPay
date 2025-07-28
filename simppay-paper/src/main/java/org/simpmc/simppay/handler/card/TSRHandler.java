package org.simpmc.simppay.handler.card;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.card.Gachthe1sConfig;
import org.simpmc.simppay.config.types.card.ThesieureConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.event.PaymentQueueSuccessEvent;
import org.simpmc.simppay.handler.CardHandler;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.model.detail.PaymentDetail;
import org.simpmc.simppay.util.HashUtil;
import org.simpmc.simppay.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class TSRHandler extends CardHandler {
    // Gachthe1s.com
    String TSR_CREATE_URL = "https://thesieure.com/chargingws/v2";
    String TSR_GET_STATUS_URL = "https://thesieure.com/chargingws/v2";
    String PARTNER_ID = ConfigManager.getInstance().getConfig(ThesieureConfig.class).partnerId;
    String PARTNER_KEY = ConfigManager.getInstance().getConfig(ThesieureConfig.class).partnerKey;

    @Override
    public String adaptCardType(CardType cardType) {
        return switch (cardType) {
            case VIETTEL -> "VIETTEL";
            case MOBIFONE -> "MOBIFONE";
            case VINAPHONE -> "VINAPHONE";
            case VIETNAMOBILE -> "VNMOBI";
            case GATE -> "GATE";
            case ZING -> "ZING";
            case GARENA -> "GARENA";
            case VCOIN -> "VCOIN";
            default -> throw new IllegalArgumentException("Unsupported card type: " + cardType);
        };
    }

    @Override
    public PaymentStatus processPayment(Payment paymentarg) {
        CardDetail detail = (CardDetail) paymentarg.getDetail();
        List<Map<String, String>> formData = new ArrayList<>();

        String hash = HashUtil.md5(PARTNER_KEY + detail.pin + detail.serial);
        formData.add(Map.of(
                "telco", adaptCardType(detail.getType()),
                "code", detail.pin,
                "serial", detail.serial,
                "amount", String.valueOf((int) detail.getAmount()),
                "request_id", hash,
                "partner_id", PARTNER_ID,
                "sign", hash,
                "command", "charging"
        ));
        String response;
        try {
            response = postFormData(formData, TSR_CREATE_URL).get();
        } catch (InterruptedException | ExecutionException e) {
            MessageUtil.debug("[TSR-ProcessPayment] Error while processing payment: " + e.getMessage());
            return PaymentStatus.FAILED;
        }
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        if (jsonResponse.get("status").getAsInt() == 99) {
            MessageUtil.debug("[TSR-ProcessPayment] " + jsonResponse);
            detail.setRefID(hash);
            paymentarg.getDetail().setRefID(hash);
            paymentarg.setDetail(detail);
            // call event to queue payment
            Bukkit.getPluginManager().callEvent(new PaymentQueueSuccessEvent(paymentarg));
            return PaymentStatus.PENDING;
        } else {
            MessageUtil.debug(response);
            return PaymentStatus.FAILED;
        }
    }

    @Override
    public PaymentResult getTransactionResult(PaymentDetail detail1) {
        CardDetail detail = (CardDetail) detail1;
        List<Map<String, String>> formData = new ArrayList<>();

        String hash = HashUtil.md5(PARTNER_KEY + detail.pin + detail.serial);
        formData.add(Map.of(
                "telco", adaptCardType(detail.getType()),
                "code", detail.pin,
                "serial", detail.serial,
                "amount", String.valueOf((int) detail.getAmount()),
                "request_id", hash,
                "partner_id", PARTNER_ID,
                "sign", hash,
                "command", "check"
        ));
        String response;
        try {
            response = postFormData(formData, TSR_GET_STATUS_URL).get();
        } catch (InterruptedException | ExecutionException e) {
            MessageUtil.debug("[TSR-GetTransactionResult] Error while getting transaction result: " + e.getMessage());
            return new PaymentResult(PaymentStatus.FAILED, (int) detail.getAmount(), "Error while processing request");
        }
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        if (jsonResponse.get("message").getAsString().equals("VALID_CARD")) {
            return new PaymentResult(
                    PaymentStatus.SUCCESS,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );
        }
        if (jsonResponse.get("message").getAsString().equals("INVALID_CARD")) {
            return new PaymentResult(
                    PaymentStatus.FAILED,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );
        }
        if (jsonResponse.get("message").getAsString().equals("PENDING")) {
            return new PaymentResult(
                    PaymentStatus.PENDING,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );
        }
        return new PaymentResult(
                PaymentStatus.FAILED,
                (int) detail.getAmount(),
                ""
        );
    }

    @Override
    public PaymentStatus cancel(Payment payment) {
        throw new UnsupportedOperationException("Cannot cancel card payment");
    }
}
