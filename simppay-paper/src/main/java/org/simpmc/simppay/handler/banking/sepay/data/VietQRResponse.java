package org.simpmc.simppay.handler.banking.sepay.data;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response wrapper for VietQR API bank list endpoint.
 * Endpoint: https://api.vietqr.io/v2/banks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VietQRResponse {
    /**
     * Response code ("00" = success)
     */
    @SerializedName("code")
    private String code;

    /**
     * Response description
     */
    @SerializedName("desc")
    private String desc;

    /**
     * List of bank data
     */
    @SerializedName("data")
    private List<BankData> data;

    /**
     * Check if response is successful
     */
    public boolean isSuccess() {
        return "00".equals(code);
    }

    /**
     * Check if data is available
     */
    public boolean hasData() {
        return data != null && !data.isEmpty();
    }
}
