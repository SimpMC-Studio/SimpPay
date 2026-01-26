package org.simpmc.simppay.handler.banking.sepay.data;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents bank information from VietQR API.
 * Used for caching bank data and resolving BIN codes from bank names.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankData {
    /**
     * Bank ID in VietQR system
     */
    @SerializedName("id")
    private int id;

    /**
     * Full bank name (Vietnamese)
     */
    @SerializedName("name")
    private String name;

    /**
     * Bank code (short identifier)
     */
    @SerializedName("code")
    private String code;

    /**
     * Bank BIN code (first 6 digits for QR generation)
     */
    @SerializedName("bin")
    private String bin;

    /**
     * Short name (English) - used for matching with user config
     */
    @SerializedName("shortName")
    private String shortName;

    /**
     * Bank logo URL
     */
    @SerializedName("logo")
    private String logo;

    /**
     * Transfer support flag
     */
    @SerializedName("transferSupported")
    private int transferSupported;

    /**
     * Lookup support flag
     */
    @SerializedName("lookupSupported")
    private int lookupSupported;

    /**
     * SWIFT code (nullable)
     */
    @SerializedName("swift_code")
    private String swiftCode;
}
