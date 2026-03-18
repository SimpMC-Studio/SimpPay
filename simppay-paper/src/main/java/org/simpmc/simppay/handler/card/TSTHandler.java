package org.simpmc.simppay.handler.card;

import com.google.gson.JsonObject;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.card.ThesieutocConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.data.card.thesieutoc.TSTCardAdapter;
import org.simpmc.simppay.event.PaymentQueueSuccessEvent;
import org.simpmc.simppay.handler.CardHandler;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.model.detail.PaymentDetail;
import org.simpmc.simppay.util.HashUtil;
import org.simpmc.simppay.util.HttpUtils;
import org.simpmc.simppay.util.MessageUtil;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@NoArgsConstructor
public class TSTHandler extends CardHandler {

    // TODO: Improve to GSON ? All logic copied from thesieutoc39

    String TST_CREATE_URL = "http://vnpt.thesieutoc.net/API/transaction";
    String TST_GET_STATUS_URL = "http://vnpt.thesieutoc.net/API/get_status_card.php";

    @Override
    public PaymentStatus processPayment(Payment payment) {
        CardDetail detail = (CardDetail) payment.getDetail();

        JsonObject request;
        try {
            request = requestTransaction(detail).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return PaymentStatus.FAILED;
        }
        if (request == null) {
            MessageUtil.debug("[Thesieutoc-ProcessPayment] Request is null");
            return PaymentStatus.FAILED;
        }
        if (request.get("status").getAsInt() == 2) {
            MessageUtil.debug("[Thesieutoc-ProcessPayment]" + request);
            return PaymentStatus.FAILED;
        }
        if (request.get("status").getAsInt() == 00) {
            MessageUtil.debug("[Thesieutoc-ProcessPayment]" + request);
            String refID = request.get("transaction_id").getAsString();
            payment.getDetail().setRefID(refID);
            Bukkit.getPluginManager().callEvent(new PaymentQueueSuccessEvent(payment));
            return PaymentStatus.PENDING;
        }
        MessageUtil.debug("[Thesieutoc-ProcessPayment]" + request);
        // default to failed if other status codes
        return PaymentStatus.FAILED;
    }

    private CompletableFuture<JsonObject> requestTransaction(CardDetail card) {
        return CompletableFuture.supplyAsync(() -> {
            ThesieutocConfig config = ConfigManager.getInstance().getConfig(ThesieutocConfig.class);

            if (config.apiKey == null || config.secretKey == null) {
                MessageUtil.info("[TST-RequestTransaction] API key or secret key is not set");
                return null;
            }
            String base = TST_CREATE_URL + "?APIkey={0}&APIsecret={1}&mathe={2}&seri={3}&type={4}&menhgia={5}";
            String rnd = HashUtil.randomMD5();
            String url = MessageFormat.format(base,
                    config.apiKey,
                    config.secretKey,
                    card.pin,
                    card.serial,
                    adaptCardType(card.type),
                    String.valueOf(TSTCardAdapter.getCardPriceID(card.price))
            ).replace("\"", "");

            JsonObject json = HttpUtils.getJsonResponse(url);
            if (json != null) {
                json.addProperty("randomMD5", rnd);
            }
            return json;
        });
    }

    @Override
    public PaymentResult getTransactionResult(PaymentDetail card) {
        // {
        //    "status": "10",
        //    "msg": "Thẻ Viettel mệnh giá 100,000VNĐ với số seri 10010570741249 Nạp Thất Bại",
        //    "amount": "10000"
        //}
        ThesieutocConfig config = ConfigManager.getInstance().getConfig(ThesieutocConfig.class);
        String base = TST_GET_STATUS_URL + "?APIkey={0}&APIsecret={1}&transaction_id={2}";
        String url = MessageFormat.format(base,
                config.apiKey,
                config.secretKey,
                card.getRefID()
        );

        // todo: scheduler
        JsonObject json = HttpUtils.getJsonResponse(url);
        if (json != null) {
            json.addProperty("randomMD5", card.getRefID());
        }
        if (json == null) {
            MessageUtil.debug("[Thesieutoc-GetTransactionStatus] Request is null");
            return new PaymentResult(PaymentStatus.FAILED, 0, null);
        }
        MessageUtil.debug("[Thesieutoc-ProcessPayment]" + json);
        int status = json.get("status").getAsInt(); // TODO: need to return wrong price ?
        PaymentStatus paymentStatus = TSTCardAdapter.getCardStatus(status);
        if (paymentStatus == PaymentStatus.WRONG_PRICE) {
            return new PaymentResult(
                    PaymentStatus.WRONG_PRICE,
                    json.get("amount").getAsInt(),
                    json.get("msg").getAsString()
            );
        }
        if (paymentStatus == PaymentStatus.SUCCESS) {
            return new PaymentResult(
                    PaymentStatus.SUCCESS,
                    (int) card.getAmount(),
                    json.get("msg").getAsString()
            );
        }
        if (paymentStatus == PaymentStatus.FAILED) {
            return new PaymentResult(
                    PaymentStatus.FAILED,
                    (int) card.getAmount(),
                    json.get("msg").getAsString()
            );
        }
        return new PaymentResult(paymentStatus, 0, json.get("msg").getAsString());
    }

    @Override
    public String adaptCardType(CardType cardType) {
        switch (cardType) {
            case VIETTEL -> {
                return "Viettel";
            }
            case VINAPHONE -> {
                return "Vinaphone";
            }
            case VIETNAMOBILE -> {
                return "Vietnamobile";
            }
            case MOBIFONE -> {
                return "Mobifone";
            }
            case GATE -> {
                return "Gate";
            }
            case ZING -> {
                return "Zing";
            }
            case GARENA -> {
                return "Garena";
            }
            case VCOIN -> {
                return "Vcoin";
            }
        }

        return null;
    }
}

