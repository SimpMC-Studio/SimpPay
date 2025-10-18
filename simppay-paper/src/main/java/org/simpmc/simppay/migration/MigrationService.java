package org.simpmc.simppay.migration;

import lombok.extern.slf4j.Slf4j;
import org.simpmc.simppay.migration.dotman.DotManMigration;
import org.simpmc.simppay.migration.thesieutoc.TheSieuTocMigration;
import org.simpmc.simppay.util.MessageUtil;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Central service for managing data migrations from legacy plugins
 * Supports TheSieuToc and DotMan
 */
@Slf4j
public class MigrationService {

    private static MigrationService instance;
    private MigrationResult lastMigrationResult;

    private MigrationService() {
    }

    /**
     * Get singleton instance
     */
    public static MigrationService getInstance() {
        if (instance == null) {
            instance = new MigrationService();
        }
        return instance;
    }

    /**
     * Migrate data from TheSieuToc plugin
     *
     * @param path Path to TheSieuToc plugin folder (e.g., "plugins/TheSieuToc")
     * @return Migration result with success/error counts
     */
    public MigrationResult migrateFromTheSieuToc(String path) {
        long startTime = System.currentTimeMillis();

        try {
            File sourceFolder = new File(path);
            if (!sourceFolder.exists()) {
                String error = "Path does not exist: " + path;
                log.error(error);
                return createErrorResult("TheSieuToc", 0, 0, error, System.currentTimeMillis() - startTime);
            }

            // Look for log files
            File logsFolder = new File(sourceFolder, "logs");
            if (!logsFolder.exists()) {
                String error = "logs folder not found at: " + logsFolder.getAbsolutePath();
                log.error(error);
                return createErrorResult("TheSieuToc", 0, 0, error, System.currentTimeMillis() - startTime);
            }

            File[] logFiles = logsFolder.listFiles((dir, name) -> name.endsWith(".txt"));
            if (logFiles == null || logFiles.length == 0) {
                String error = "No .txt log files found in: " + logsFolder.getAbsolutePath();
                log.warn(error);
                return createErrorResult("TheSieuToc", 0, 0, error, System.currentTimeMillis() - startTime);
            }

            MessageUtil.info("Found " + logFiles.length + " log files for TheSieuToc migration");

            // Process each log file
            List<String> allErrors = new ArrayList<>();
            int totalSuccess = 0;
            int totalSkipped = 0;
            int totalRecords = 0;

            TheSieuTocMigration migration = new TheSieuTocMigration();
            for (File logFile : logFiles) {
                try {
                    MessageUtil.info("Processing TheSieuToc log file: " + logFile.getName());
                    MigrationResult result = migration.migrateFromFile(logFile);

                    totalRecords += result.totalRecords();
                    totalSuccess += result.successCount();
                    totalSkipped += result.skippedCount();
                    allErrors.addAll(result.errors());

                    MessageUtil.debug("File " + logFile.getName() + ": " + result.successCount() + " success, " + result.skippedCount() + " skipped");
                } catch (Exception e) {
                    String errorMsg = "Error processing " + logFile.getName() + ": " + e.getMessage();
                    allErrors.add(errorMsg);
                    log.error(errorMsg, e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            MigrationResult result = new MigrationResult(
                    "TheSieuToc",
                    totalRecords,
                    totalSuccess,
                    totalSkipped,
                    allErrors.size(),
                    allErrors,
                    duration,
                    Instant.now()
            );

            this.lastMigrationResult = result;
            MessageUtil.info("TheSieuToc migration completed: " + result.successCount() + " success, " + result.errorCount() + " errors");
            return result;

        } catch (Exception e) {
            log.error("Unexpected error during TheSieuToc migration", e);
            return createErrorResult("TheSieuToc", 0, 0, e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Migrate data from DotMan plugin
     *
     * @param path Path to DotMan plugin folder (e.g., "plugins/DotMan")
     * @return Migration result with success/error counts
     */
    public MigrationResult migrateFromDotMan(String path) {
        long startTime = System.currentTimeMillis();

        try {
            File sourceFolder = new File(path);
            if (!sourceFolder.exists()) {
                String error = "Path does not exist: " + path;
                log.error(error);
                return createErrorResult("DotMan", 0, 0, error, System.currentTimeMillis() - startTime);
            }

            File configFile = new File(sourceFolder, "config.yml");
            if (!configFile.exists()) {
                String error = "config.yml not found at: " + configFile.getAbsolutePath();
                log.error(error);
                return createErrorResult("DotMan", 0, 0, error, System.currentTimeMillis() - startTime);
            }

            MessageUtil.info("Starting DotMan migration from: " + sourceFolder.getAbsolutePath());

            DotManMigration migration = new DotManMigration();
            MigrationResult result = migration.migrateFromDatabase(sourceFolder);

            this.lastMigrationResult = result;
            MessageUtil.info("DotMan migration completed: " + result.successCount() + " success, " + result.errorCount() + " errors");
            return result;

        } catch (Exception e) {
            log.error("Unexpected error during DotMan migration", e);
            return createErrorResult("DotMan", 0, 0, e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Get the last migration result
     *
     * @return Last migration result, or null if no migration has been run
     */
    public MigrationResult getLastMigrationResult() {
        return lastMigrationResult;
    }

    /**
     * Clear the last migration result
     */
    public void clearLastResult() {
        lastMigrationResult = null;
    }

    /**
     * Helper method to create error result
     */
    private MigrationResult createErrorResult(String source, int total, int success, String error, long duration) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new MigrationResult(
                source,
                total,
                success,
                0,
                1,
                errors,
                duration,
                Instant.now()
        );
    }
}
