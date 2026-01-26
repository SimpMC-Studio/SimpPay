package org.simpmc.simppay.handler.banking.sepay.data;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data model for Sepay webhook payload.
 * Matches the JSON structure sent by Sepay when a transaction occurs.
 * 
 * Documentation: https://developer.sepay.vn/en/sepay-webhooks/tich-hop-webhook
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SepayWebhookPayload {
    /**
     * Transaction unique ID
     */
    @SerializedName("id")
    private long id;

    /**
     * Bank gateway name (e.g., "Vietcombank", "MBBank")
     */
    @SerializedName("gateway")
    private String gateway;

    /**
     * Transaction timestamp (format: "yyyy-MM-dd HH:mm:ss")
     */
    @SerializedName("transactionDate")
    private String transactionDate;

    /**
     * Bank account number that received the payment
     */
    @SerializedName("accountNumber")
    private String accountNumber;

    /**
     * Payment code (nullable)
     */
    @SerializedName("code")
    private String code;

    /**
     * Transaction content/description
     */
    @SerializedName("content")
    private String content;

    /**
     * Transfer type: "in" (money received) or "out" (money sent)
     */
    @SerializedName("transferType")
    private String transferType;

    /**
     * Amount transferred
     */
    @SerializedName("transferAmount")
    private double transferAmount;

    /**
     * Accumulated balance after transaction
     */
    @SerializedName("accumulated")
    private double accumulated;

    /**
     * Sub-account identifier (nullable)
     */
    @SerializedName("subAccount")
    private String subAccount;

    /**
     * Bank reference code
     */
    @SerializedName("referenceCode")
    private String referenceCode;

    /**
     * Additional description (nullable)
     */
    @SerializedName("description")
    private String description;

    /**
     * Check if this is an incoming transfer
     */
    public boolean isIncomingTransfer() {
        return "in".equalsIgnoreCase(transferType);
    }
}
