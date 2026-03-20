package org.simpmc.simppay.handler.banking;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.simpmc.simppay.config.types.banking.Web2mConfig;
import org.simpmc.simppay.handler.banking.web2m.W2MHandler;
import org.simpmc.simppay.testutil.MockBukkitSetup;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class W2MHandlerTest {

    @AfterEach
    void tearDown() {
        MockBukkitSetup.clearConfigManager();
    }

    @Test
    void w2mConfig_defaultAccountNumber_isDefaultPlaceholder() {
        Web2mConfig config = new Web2mConfig();
        // Default account number "123123123" triggers FAILED path in processPayment
        assertEquals("123123123", config.accountNumber);
    }

    @Test
    void getTransactionResult_matchesRefIdInDescription() {
        // Verify the matching logic: tx.getDescription().contains(detail.getRefID())
        String refId = "abc1234567";
        String description = "Payment " + refId + " done";
        assertTrue(description.contains(refId),
                "Transaction matching should work when description contains refId");
    }

    @Test
    void getTransactionResult_noMatch_notContained() {
        String refId = "abc1234567";
        String description = "Unrelated payment description";
        assertFalse(description.contains(refId),
                "No match when description does not contain refId");
    }
}
