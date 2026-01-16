package org.simpmc.simppay.handler.banking.sepay.data;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single transaction from Sepay API.
 * Phase 4: Sepay integration data model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SepayTransaction {
    /**
     * Transaction unique ID
     */
    @SerializedName("id")
    private String id;

    /**
     * Bank brand name (e.g., "Vietcombank", "MBBank")
     */
    @SerializedName("bank_brand_name")
    private String bankBrandName;

    /**
     * Bank account number that received the payment
     */
    @SerializedName("account_number")
    private String accountNumber;

    /**
     * Transaction timestamp (format: "yyyy-MM-dd HH:mm:ss")
     */
    @SerializedName("transaction_date")
    private String transactionDate;

    /**
     * Amount sent out (0 for incoming transfers)
     */
    @SerializedName("amount_out")
    private String amountOut;

    /**
     * Amount received (incoming transfer amount)
     */
    @SerializedName("amount_in")
    private String amountIn;

    /**
     * Accumulated balance after transaction
     */
    @SerializedName("accumulated")
    private String accumulated;

    /**
     * Transaction content/description (contains sender info)
     */
    @SerializedName("transaction_content")
    private String transactionContent;

    /**
     * Bank reference number
     */
    @SerializedName("reference_number")
    private String referenceNumber;

    /**
     * Transaction code (optional)
     */
    @SerializedName("code")
    private String code;

    /**
     * Sub-account identifier (if applicable)
     */
    @SerializedName("sub_account")
    private String subAccount;

    /**
     * Bank account ID in Sepay system
     */
    @SerializedName("bank_account_id")
    private String bankAccountId;

    /**
     * Helper method to get incoming amount as double
     */
    public double getAmountInValue() {
        try {
            return Double.parseDouble(amountIn);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
