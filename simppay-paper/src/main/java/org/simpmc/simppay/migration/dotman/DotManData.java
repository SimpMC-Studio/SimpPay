package org.simpmc.simppay.migration.dotman;

/**
 * Data transfer object for DotMan transaction data
 * Represents a single record from DotMan's log table
 */
public record DotManData(
        String logId,           // Unique log ID from DotMan
        String playerUuid,      // Player UUID from DotMan
        String playerName,      // Player name
        String cardType,        // Card type (VIETTEL, MOBIFONE, etc. or MANUAL)
        String cardSerial,      // Card serial (or MANUAL/-- for manual topups)
        String cardPin,         // Card PIN (or -- for manual topups)
        long amount,            // Amount in VND
        long pointsReceived,    // Points given to player
        long timestamp,         // Transaction timestamp (milliseconds)
        boolean success         // Was transaction successful
) {

    /**
     * Validate that all required fields are present and valid
     */
    public boolean isValid() {
        return playerUuid != null && !playerUuid.trim().isEmpty()
                && playerName != null && !playerName.trim().isEmpty()
                && cardType != null && !cardType.trim().isEmpty()
                && cardSerial != null
                && cardPin != null
                && amount > 0
                && timestamp > 0;
    }

    /**
     * Check if this is a manual topup (not a card transaction)
     */
    public boolean isManualTopup() {
        return "MANUAL".equalsIgnoreCase(cardType)
                || "THỦ CÔNG".equalsIgnoreCase(cardType)
                || "--".equals(cardSerial)
                || "--".equals(cardPin);
    }

    @Override
    public String toString() {
        return "DotManData{" +
                "logId='" + logId + '\'' +
                ", playerUuid='" + playerUuid + '\'' +
                ", playerName='" + playerName + '\'' +
                ", cardType='" + cardType + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", success=" + success +
                '}';
    }
}
