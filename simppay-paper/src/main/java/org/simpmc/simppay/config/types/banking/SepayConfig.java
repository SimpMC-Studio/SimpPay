package org.simpmc.simppay.config.types.banking;

import de.exlll.configlib.Configuration;
import org.simpmc.simppay.config.annotations.Folder;

/**
 * Phase 4: Sepay banking integration configuration.
 * <p>
 * Get your API token from: https://my.sepay.vn/userapi/setting
 */
@Configuration
@Folder("banking/sepay")
public class SepayConfig {
    /**
     * Sepay API Bearer token for authentication
     * Format: "Bearer YOUR_TOKEN_HERE"
     */
    public String apiToken = "YOUR_SEPAY_API_TOKEN_HERE";

    /**
     * Bank account number to receive payments
     * Example: "0071000888888"
     */
    public String accountNumber = "YOUR_BANK_ACCOUNT_NUMBER";

    /**
     * Bank brand name (for display purposes)
     * Examples: "Vietcombank", "TechcomBank", "MBBank", etc.
     */
    public String bankBrandName = "Your Bank Name";

    /**
     * Bank BIN code (first 6 digits of card number, for QR code generation)
     * Examples: "970436" (Vietcombank), "970407" (TechcomBank)
     */
    public String bin = "970436";
}
