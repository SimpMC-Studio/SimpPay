package org.simpmc.simppay.migration.thesieutoc;

/**
 * Data transfer object for TheSieuToc transaction data
 * Represents a single parsed log entry
 */
public record TheSieuTocData(
        long timestamp,         // Milliseconds since epoch
        String playerName,      // Minecraft player name
        String serial,          // Card serial number
        String pin,             // Card PIN number
        int amount,             // Amount in VND
        String telco,           // Telco provider (VIETTEL, MOBIFONE, etc.)
        String status           // SUCCESS or FAILED
) {

    /**
     * Validate that all required fields are present and valid
     */
    public boolean isValid() {
        return playerName != null && !playerName.trim().isEmpty()
                && serial != null && !serial.trim().isEmpty()
                && pin != null && !pin.trim().isEmpty()
                && amount > 0
                && telco != null && !telco.trim().isEmpty()
                && status != null && !status.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "TheSieuTocData{" +
                "timestamp=" + timestamp +
                ", playerName='" + playerName + '\'' +
                ", serial='" + serial + '\'' +
                ", pin='" + pin + '\'' +
                ", amount=" + amount +
                ", telco='" + telco + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
