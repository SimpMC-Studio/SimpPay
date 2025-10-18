package org.simpmc.simppay.migration;

import java.time.Instant;
import java.util.List;

/**
 * Result of a data migration operation
 * Tracks success/failure/duplicate counts and error details
 */
public record MigrationResult(
        String source,              // "TheSieuToc" or "DotMan"
        int totalRecords,           // Total records found
        int successCount,           // Successfully imported
        int skippedCount,           // Duplicates skipped
        int errorCount,             // Failed imports
        List<String> errors,        // Error messages with details
        long durationMs,            // Migration duration in milliseconds
        Instant timestamp           // When migration started
) {

    /**
     * Check if migration had any errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Calculate success rate as percentage
     */
    public double getSuccessRate() {
        if (totalRecords == 0) return 0.0;
        return ((double) successCount / totalRecords) * 100.0;
    }

    /**
     * Get human-readable summary of migration results
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("§6═══════════════════════════════════════\n");
        summary.append("§e§lMigration Result: ").append(source).append("\n");
        summary.append("§f").append("Total: ").append(totalRecords)
                .append(" | Success: ").append(successCount)
                .append(" | Skipped: ").append(skippedCount)
                .append(" | Errors: ").append(errorCount).append("\n");
        summary.append("§fSuccess Rate: §e").append(String.format("%.1f%%", getSuccessRate())).append("\n");
        summary.append("§fDuration: §e").append(durationMs).append("ms").append("\n");

        if (hasErrors()) {
            summary.append("§c⚠ Errors encountered during migration").append("\n");
        } else {
            summary.append("§a✓ Migration completed successfully").append("\n");
        }

        summary.append("§6═══════════════════════════════════════");
        return summary.toString();
    }

    /**
     * Format error list for display
     */
    public String getErrorsSummary(int maxErrors) {
        if (errors.isEmpty()) {
            return "§aNo errors";
        }

        StringBuilder sb = new StringBuilder();
        int shown = Math.min(maxErrors, errors.size());
        for (int i = 0; i < shown; i++) {
            sb.append("§c• ").append(errors.get(i)).append("\n");
        }

        if (errors.size() > maxErrors) {
            sb.append("§e... and ").append(errors.size() - maxErrors).append(" more errors\n");
        }

        return sb.toString();
    }
}
