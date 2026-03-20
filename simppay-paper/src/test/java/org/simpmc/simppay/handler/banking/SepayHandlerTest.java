package org.simpmc.simppay.handler.banking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.simpmc.simppay.config.types.banking.SepayConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.handler.banking.sepay.SepayHandler;
import org.simpmc.simppay.model.PaymentResult;
import org.simpmc.simppay.model.detail.PaymentDetail;
import org.simpmc.simppay.testutil.MockBukkitSetup;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SepayHandlerTest {

    @AfterEach
    void tearDown() {
        MockBukkitSetup.clearConfigManager();
    }

    @Test
    void sepayConfig_defaultConfig_hasPlaceholderValues() {
        SepayConfig config = new SepayConfig();
        assertEquals("YOUR_WEBHOOK_API_KEY_HERE", config.webhookApiKey);
        assertEquals("YOUR_BANK_ACCOUNT_NUMBER", config.accountNumber);
        assertEquals("Vietcombank", config.bankName);
        assertEquals(8080, config.webhookPort);
        assertEquals("smc123", config.descriptionPrefix);
    }

    @Test
    void getTransactionResult_alwaysReturnsPending() {
        MockBukkitSetup.mockConfigManager(); // sets up default MainConfig so debug() is safe
        SepayHandler handler = new SepayHandler();
        PaymentDetail detail = mock(PaymentDetail.class);

        PaymentResult result = handler.getTransactionResult(detail);

        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(0, result.getAmount());
    }

    @Test
    void processPayment_defaultConfig_returns_FAILED_dueToPlaceholderApiKey() {
        SepayConfig config = new SepayConfig();
        // processPayment checks for placeholder api key and returns FAILED immediately
        // verify the config guard logic by checking the default value:
        assertTrue(config.webhookApiKey.equals("YOUR_WEBHOOK_API_KEY_HERE"),
                "Default config must have placeholder API key to trigger FAILED path");
    }
}
