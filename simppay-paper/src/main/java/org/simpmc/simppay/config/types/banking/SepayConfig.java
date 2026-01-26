package org.simpmc.simppay.config.types.banking;

import de.exlll.configlib.Configuration;
import org.simpmc.simppay.config.annotations.Folder;

/**
 * Sepay banking integration configuration - Webhook-based.
 * 
 * IMPORTANT: This configuration uses webhooks instead of API polling.
 * You must configure webhooks in your Sepay dashboard:
 * 1. Go to https://my.sepay.vn/webhooks
 * 2. Create a new webhook with:
 *    - Call URL: http://YOUR_SERVER_IP:WEBHOOK_PORT/sepay/webhook
 *    - Authentication Type: API Key
 *    - Request Content Type: application/json
 *    - Event: Money In
 * 3. Copy the API key and set it in webhookApiKey below
 */
@Configuration
@Folder("banking/sepay")
public class SepayConfig {
    /**
     * Bank account number to receive payments
     * Example: "0071000888888"
     */
    public String accountNumber = "YOUR_BANK_ACCOUNT_NUMBER";

    /**
     * Bank name (must match VietQR shortName - case insensitive)
     * Examples: "Vietcombank", "Techcombank", "MBBank", "VPBank", etc.
     * 
     * The BIN code will be automatically fetched from VietQR API based on this name.
     * See https://api.vietqr.io/v2/banks for full list of supported banks.
     */
    public String bankName = "Vietcombank";

    /**
     * Webhook server port
     * Make sure this port is open and accessible from the internet.
     * Sepay needs to be able to send webhooks to this port.
     */
    public int webhookPort = 8080;

    /**
     * Webhook endpoint path
     * Full webhook URL will be: http://YOUR_SERVER_IP:webhookPort/webhookPath
     */
    public String webhookPath = "/sepay/webhook";

    /**
     * Webhook API key for authentication
     * This key must match the one you configured in Sepay dashboard.
     * Sepay will send this in the Authorization header as: "APIkey_YOUR_KEY"
     * 
     * IMPORTANT: Keep this secret! Do not share it publicly.
     */
    public String webhookApiKey = "YOUR_WEBHOOK_API_KEY_HERE";

    /**
     * Prefix for payment description/reference codes.
     * This is added before the 10-character reference code.
     * Example: "smc123" -> description = "smc123ABC1234567"
     * 
     * The full description is used to match incoming webhook payments.
     */
    public String descriptionPrefix = "smc123";
}

