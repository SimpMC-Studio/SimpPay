package org.simpmc.simppay.handler.card;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.simpmc.simppay.config.types.card.ThesieutocConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardPrice;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.testutil.MockBukkitSetup;
import org.simpmc.simppay.util.HttpUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class TSTHandlerTest {

    @AfterEach
    void tearDown() {
        MockBukkitSetup.clearConfigManager();
    }

    @Test
    void adaptCardType_allMappings() {
        TSTHandler handler = new TSTHandler();
        assertEquals("Viettel", handler.adaptCardType(CardType.VIETTEL));
        assertEquals("Vinaphone", handler.adaptCardType(CardType.VINAPHONE));
        assertEquals("Vietnamobile", handler.adaptCardType(CardType.VIETNAMOBILE));
        assertEquals("Mobifone", handler.adaptCardType(CardType.MOBIFONE));
        assertEquals("Gate", handler.adaptCardType(CardType.GATE));
        assertEquals("Zing", handler.adaptCardType(CardType.ZING));
        assertEquals("Garena", handler.adaptCardType(CardType.GARENA));
        assertEquals("Vcoin", handler.adaptCardType(CardType.VCOIN));
    }

    @Test
    void getTransactionResult_nullResponse_returnsFailed() {
        ThesieutocConfig config = new ThesieutocConfig();
        config.apiKey = "test-key";
        config.secretKey = "test-secret";
        MockBukkitSetup.mockConfigManager(ThesieutocConfig.class, config);

        try (MockedStatic<HttpUtils> utils = mockStatic(HttpUtils.class)) {
            utils.when(() -> HttpUtils.getJsonResponse(anyString())).thenReturn(null);

            TSTHandler handler = new TSTHandler();
            CardDetail detail = CardDetail.builder()
                    .serial("1234567890").pin("111111")
                    .price(CardPrice._10K).type(CardType.VIETTEL)
                    .refID("TX123").build();

            PaymentResult result = handler.getTransactionResult(detail);
            assertEquals(PaymentStatus.FAILED, result.getStatus());
        }
    }

    @Test
    void getTransactionResult_successStatus0_returnsSuccess() {
        // TST status 0 = SUCCESS (see TSTCardAdapter)
        ThesieutocConfig config = new ThesieutocConfig();
        config.apiKey = "test-key";
        config.secretKey = "test-secret";
        MockBukkitSetup.mockConfigManager(ThesieutocConfig.class, config);

        JsonObject mockJson = new JsonObject();
        mockJson.addProperty("status", 0);
        mockJson.addProperty("amount", 10000);
        mockJson.addProperty("msg", "Success");

        try (MockedStatic<HttpUtils> utils = mockStatic(HttpUtils.class)) {
            utils.when(() -> HttpUtils.getJsonResponse(anyString())).thenReturn(mockJson);

            TSTHandler handler = new TSTHandler();
            CardDetail detail = CardDetail.builder()
                    .serial("1234567890").pin("111111")
                    .price(CardPrice._10K).type(CardType.VIETTEL)
                    .refID("TX123").build();

            PaymentResult result = handler.getTransactionResult(detail);
            assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        }
    }

    @Test
    void getTransactionResult_failedStatus2_returnsFailed() {
        // TST status 2 = FAILED (see TSTCardAdapter)
        ThesieutocConfig config = new ThesieutocConfig();
        config.apiKey = "test-key";
        config.secretKey = "test-secret";
        MockBukkitSetup.mockConfigManager(ThesieutocConfig.class, config);

        JsonObject mockJson = new JsonObject();
        mockJson.addProperty("status", 2);
        mockJson.addProperty("amount", 10000);
        mockJson.addProperty("msg", "Card failed");

        try (MockedStatic<HttpUtils> utils = mockStatic(HttpUtils.class)) {
            utils.when(() -> HttpUtils.getJsonResponse(anyString())).thenReturn(mockJson);

            TSTHandler handler = new TSTHandler();
            CardDetail detail = CardDetail.builder()
                    .serial("1234567890").pin("111111")
                    .price(CardPrice._10K).type(CardType.VIETTEL)
                    .refID("TX123").build();

            PaymentResult result = handler.getTransactionResult(detail);
            assertEquals(PaymentStatus.FAILED, result.getStatus());
        }
    }

    @Test
    void getTransactionResult_wrongPriceStatus10_returnsWrongPrice() {
        // TST status 10 = WRONG_PRICE (see TSTCardAdapter)
        ThesieutocConfig config = new ThesieutocConfig();
        config.apiKey = "test-key";
        config.secretKey = "test-secret";
        MockBukkitSetup.mockConfigManager(ThesieutocConfig.class, config);

        JsonObject mockJson = new JsonObject();
        mockJson.addProperty("status", 10);
        mockJson.addProperty("amount", 5000);
        mockJson.addProperty("msg", "Wrong price");

        try (MockedStatic<HttpUtils> utils = mockStatic(HttpUtils.class)) {
            utils.when(() -> HttpUtils.getJsonResponse(anyString())).thenReturn(mockJson);

            TSTHandler handler = new TSTHandler();
            CardDetail detail = CardDetail.builder()
                    .serial("1234567890").pin("111111")
                    .price(CardPrice._10K).type(CardType.VIETTEL)
                    .refID("TX123").build();

            PaymentResult result = handler.getTransactionResult(detail);
            assertEquals(PaymentStatus.WRONG_PRICE, result.getStatus());
            assertEquals(5000, result.getAmount());
        }
    }
}
