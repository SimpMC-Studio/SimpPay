package org.simpmc.simppay.handler.card.nencer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.simpmc.simppay.config.ConfigManager;
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

public class NencerCardHandler extends CardHandler {

    private final String apiUrl;
    private final Class<? extends NencerCardConfig> configClass;
    private final String debugPrefix;
    private final Map<CardType, String> cardTypeOverrides;

    public NencerCardHandler(String apiUrl, Class<? extends NencerCardConfig> configClass,
                             String debugPrefix, Map<CardType, String> cardTypeOverrides) {
        this.apiUrl = apiUrl;
        this.configClass = configClass;
        this.debugPrefix = debugPrefix;
        this.cardTypeOverrides = cardTypeOverrides;
    }

    @Override
    public String adaptCardType(CardType cardType) {
        if (cardTypeOverrides.containsKey(cardType)) {
            return cardTypeOverrides.get(cardType);
        }
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
        NencerCardConfig config = ConfigManager.getInstance().getConfig(configClass);
        CardDetail detail = (CardDetail) paymentarg.getDetail();
        List<Map<String, String>> formData = new ArrayList<>();

        String hash = HashUtil.md5(config.getPartnerKey() + detail.pin + detail.serial);
        formData.add(Map.of(
                "telco", adaptCardType(detail.getType()),
                "code", detail.pin,
                "serial", detail.serial,
                "amount", String.valueOf((int) detail.getAmount()),
                "request_id", hash,
                "partner_id", config.getPartnerId(),
                "sign", hash,
                "command", "charging"
        ));
        String response;
        try {
            response = postFormData(formData, apiUrl).get();
        } catch (InterruptedException | ExecutionException e) {
            MessageUtil.debug("[" + debugPrefix + "-ProcessPayment] Error while processing payment: " + e.getMessage());
            return PaymentStatus.FAILED;
        }
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        if (jsonResponse.get("status").getAsInt() == 99) {
            MessageUtil.debug("[" + debugPrefix + "-ProcessPayment] " + jsonResponse);
            detail.setRefID(hash);
            paymentarg.getDetail().setRefID(hash);
            paymentarg.setDetail(detail);
            Bukkit.getPluginManager().callEvent(new PaymentQueueSuccessEvent(paymentarg));
            return PaymentStatus.PENDING;
        } else {
            MessageUtil.debug(response);
            return PaymentStatus.FAILED;
        }
    }

    @Override
    public PaymentResult getTransactionResult(PaymentDetail detail1) {
        NencerCardConfig config = ConfigManager.getInstance().getConfig(configClass);
        CardDetail detail = (CardDetail) detail1;
        List<Map<String, String>> formData = new ArrayList<>();

        String hash = HashUtil.md5(config.getPartnerKey() + detail.pin + detail.serial);
        formData.add(Map.of(
                "telco", adaptCardType(detail.getType()),
                "code", detail.pin,
                "serial", detail.serial,
                "amount", String.valueOf((int) detail.getAmount()),
                "request_id", hash,
                "partner_id", config.getPartnerId(),
                "sign", hash,
                "command", "check"
        ));
        String response;
        try {
            response = postFormData(formData, apiUrl).get();
        } catch (InterruptedException | ExecutionException e) {
            MessageUtil.debug("[" + debugPrefix + "-GetTransactionResult] Error while getting transaction result: " + e.getMessage());
            return new PaymentResult(PaymentStatus.FAILED, (int) detail.getAmount(), "Error while processing request");
        }
        return getNencerAPIResult(detail, response);
    }

    protected PaymentResult getNencerAPIResult(CardDetail detail, String response) {
        JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
        int status = jsonResponse.get("status").getAsInt();
        if (status == 1) {
            return new PaymentResult(
                    PaymentStatus.SUCCESS,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );
        }
        if (status == 2) {
            return new PaymentResult(
                    PaymentStatus.WRONG_PRICE,
                    jsonResponse.get("value").getAsInt(),
                    "Sai menh gia, menh gia thuc la: " + jsonResponse.get("declared_value").getAsString()
            );
        }
        if (status == 3) {
            return new PaymentResult(
                    PaymentStatus.FAILED,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );
        }
        if (status == 99) {
            return new PaymentResult(
                    PaymentStatus.PENDING,
                    (int) detail.getAmount(),
                    jsonResponse.get("message").getAsString()
            );
        }
        return new PaymentResult(PaymentStatus.FAILED, (int) detail.getAmount(), "");
    }
}
