package org.simpmc.simppay.handler.banking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.simpmc.simppay.config.types.banking.PayosConfig;
import org.simpmc.simppay.handler.banking.payos.PayosHandler;
import org.simpmc.simppay.model.detail.BankingDetail;
import org.simpmc.simppay.testutil.MockBukkitSetup;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PayosHandlerTest {

    @AfterEach
    void tearDown() {
        MockBukkitSetup.clearConfigManager();
    }

    @Test
    void payosHandler_baseApiUrl_isCorrectDefault() {
        PayosHandler handler = new PayosHandler();
        assertEquals("https://api-merchant.payos.vn/v2/payment-requests", handler.baseApiUrl);
    }

    @Test
    void payosHandler_baseApiUrl_isOverridable() {
        PayosHandler handler = new PayosHandler();
        handler.baseApiUrl = "http://localhost:9999/mock";
        assertEquals("http://localhost:9999/mock", handler.baseApiUrl);
    }

    @Test
    void payosHandler_supportsCancellation() {
        PayosHandler handler = new PayosHandler();
        assertTrue(handler.supportsCancellation());
    }

    @Test
    void payosConfig_defaults_arePlaceholders() {
        PayosConfig config = new PayosConfig();
        assertEquals("your-client-id", config.clientId);
        assertEquals("your-api-key", config.apiKey);
        assertEquals("your-checksum-key", config.checksumKey);
    }

    @Test
    void bankingDetail_builder_works() {
        BankingDetail detail = BankingDetail.builder()
                .amount(100000)
                .refID("test-ref-id")
                .description("test-desc")
                .build();

        assertEquals(100000, detail.getAmount());
        assertEquals("test-ref-id", detail.getRefID());
    }
}
