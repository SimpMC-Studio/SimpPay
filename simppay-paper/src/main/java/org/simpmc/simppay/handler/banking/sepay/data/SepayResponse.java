package org.simpmc.simppay.handler.banking.sepay.data;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Sepay API response wrapper.
 * Phase 4: Sepay integration data model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SepayResponse {
    /**
     * HTTP status code (200 = success)
     */
    @SerializedName("status")
    private int status;

    /**
     * Error message (null if success)
     */
    @SerializedName("error")
    private String error;

    /**
     * Response messages
     */
    @SerializedName("messages")
    private Map<String, Object> messages;

    /**
     * List of transactions
     */
    @SerializedName("transactions")
    private List<SepayTransaction> transactions;

    /**
     * Helper method to check if response is successful
     */
    public boolean isSuccess() {
        return status == 200 && error == null;
    }

    /**
     * Helper method to check if any transactions were found
     */
    public boolean hasTransactions() {
        return transactions != null && !transactions.isEmpty();
    }
}
