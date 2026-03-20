package org.simpmc.simppay.handler.card;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.data.card.CardPrice;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.handler.card.nencer.NencerCardConfig;
import org.simpmc.simppay.handler.card.nencer.NencerCardHandler;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.CardDetail;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NencerCardHandlerTest {

    /** Exposes protected getNencerAPIResult for testing. */
    private static class TestableNencerCardHandler extends NencerCardHandler {
        private static final NencerCardConfig CONFIG = new NencerCardConfig() {
            @Override public String getPartnerId() { return "test-partner"; }
            @Override public String getPartnerKey() { return "test-key"; }
        };

        TestableNencerCardHandler() {
            super("http://example.com/api", CONFIG.getClass(), "TEST", Map.of());
        }

        TestableNencerCardHandler(Map<CardType, String> overrides) {
            super("http://example.com/api", CONFIG.getClass(), "TEST", overrides);
        }

        @Override
        public PaymentResult getNencerAPIResult(CardDetail detail, String response) {
            return super.getNencerAPIResult(detail, response);
        }
    }

    @Test
    void adaptCardType_defaultMappings() {
        TestableNencerCardHandler handler = new TestableNencerCardHandler();
        assertEquals("VIETTEL", handler.adaptCardType(CardType.VIETTEL));
        assertEquals("MOBIFONE", handler.adaptCardType(CardType.MOBIFONE));
        assertEquals("VINAPHONE", handler.adaptCardType(CardType.VINAPHONE));
        assertEquals("VNMOBI", handler.adaptCardType(CardType.VIETNAMOBILE));
        assertEquals("GATE", handler.adaptCardType(CardType.GATE));
        assertEquals("ZING", handler.adaptCardType(CardType.ZING));
        assertEquals("GARENA", handler.adaptCardType(CardType.GARENA));
        assertEquals("VCOIN", handler.adaptCardType(CardType.VCOIN));
    }

    @Test
    void adaptCardType_withOverrides() {
        TestableNencerCardHandler handler = new TestableNencerCardHandler(
                Map.of(CardType.VIETTEL, "VTT_CUSTOM")
        );
        assertEquals("VTT_CUSTOM", handler.adaptCardType(CardType.VIETTEL));
        assertEquals("MOBIFONE", handler.adaptCardType(CardType.MOBIFONE));
    }

    @Test
    void getNencerAPIResult_status1_returnsSuccess() {
        TestableNencerCardHandler handler = new TestableNencerCardHandler();
        CardDetail detail = CardDetail.builder()
                .serial("SN123").pin("PIN123")
                .price(CardPrice._10K).type(CardType.VIETTEL)
                .build();

        PaymentResult result = handler.getNencerAPIResult(detail,
                buildJson(1, "Success", 10000, 10000));

        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        assertEquals(10000, result.getAmount());
    }

    @Test
    void getNencerAPIResult_status2_returnsWrongPrice() {
        TestableNencerCardHandler handler = new TestableNencerCardHandler();
        CardDetail detail = CardDetail.builder()
                .serial("SN123").pin("PIN123")
                .price(CardPrice._10K).type(CardType.VIETTEL)
                .build();

        PaymentResult result = handler.getNencerAPIResult(detail,
                "{\"status\":2,\"message\":\"Wrong price\",\"value\":5000,\"declared_value\":\"10000\"}");

        assertEquals(PaymentStatus.WRONG_PRICE, result.getStatus());
        assertEquals(5000, result.getAmount());
    }

    @Test
    void getNencerAPIResult_status3_returnsFailed() {
        TestableNencerCardHandler handler = new TestableNencerCardHandler();
        CardDetail detail = CardDetail.builder()
                .serial("SN123").pin("PIN123")
                .price(CardPrice._10K).type(CardType.VIETTEL)
                .build();

        PaymentResult result = handler.getNencerAPIResult(detail,
                buildJson(3, "Card failed", 10000, 10000));

        assertEquals(PaymentStatus.FAILED, result.getStatus());
    }

    @Test
    void getNencerAPIResult_status99_returnsPending() {
        TestableNencerCardHandler handler = new TestableNencerCardHandler();
        CardDetail detail = CardDetail.builder()
                .serial("SN123").pin("PIN123")
                .price(CardPrice._10K).type(CardType.VIETTEL)
                .build();

        PaymentResult result = handler.getNencerAPIResult(detail,
                buildJson(99, "Pending", 10000, 10000));

        assertEquals(PaymentStatus.PENDING, result.getStatus());
    }

    private String buildJson(int status, String message, int value, int declaredValue) {
        return String.format(
                "{\"status\":%d,\"message\":\"%s\",\"value\":%d,\"declared_value\":\"%d\"}",
                status, message, value, declaredValue
        );
    }
}
