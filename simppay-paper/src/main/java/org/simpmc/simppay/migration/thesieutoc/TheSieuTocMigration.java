package org.simpmc.simppay.migration.thesieutoc;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.database.entities.CardPayment;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.migration.MigrationResult;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.util.MessageUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles migration of TheSieuToc .txt log files into SimpPay database
 * Pattern: [HH:mm dd/MM/yyyy] PlayerName | Serial | PIN | Amount | Telco | Status
 */
@Slf4j
public class TheSieuTocMigration {

    private static final Pattern LINE_PATTERN = Pattern.compile(
            "\\[(\\d{2}:\\d{2} \\d{2}/\\d{2}/\\d{4})\\]\\s+([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)\\s*\\|\\s*([^|]+)"
    );

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    private final DatabaseService databaseService;

    public TheSieuTocMigration() {
        this.databaseService = SPPlugin.getService(DatabaseService.class);
    }

    /**
     * Migrate transactions from a TheSieuToc log file
     *
     * @param file The log file to migrate
     * @return Migration result with statistics
     */
    public MigrationResult migrateFromFile(File file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;
        int totalLines = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                totalLines++;
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    TheSieuTocData data = parseLine(line);
                    if (data == null) {
                        errors.add("Line " + totalLines + ": Failed to parse - " + line);
                        continue;
                    }

                    // Validate data
                    if (!data.isValid()) {
                        errors.add("Line " + totalLines + ": Invalid data - " + data);
                        continue;
                    }

                    // Check if transaction already exists
                    if (isDuplicate(data)) {
                        skippedCount++;
                        continue;
                    }

                    // Create and save card payment
                    CardPayment payment = createCardPaymentFromData(data);
                    if (payment != null) {
                        databaseService.getDatabase().getCardDao().create(payment);
                        successCount++;
                        MessageUtil.debug("Migrated TheSieuToc transaction: " + data.playerName());
                    } else {
                        errors.add("Line " + totalLines + ": Failed to create payment object");
                    }

                } catch (Exception e) {
                    errors.add("Line " + totalLines + ": " + e.getMessage());
                    log.debug("Error processing TheSieuToc line: " + line, e);
                }
            }
        } catch (IOException e) {
            errors.add("Failed to read file: " + e.getMessage());
            log.error("Failed to read TheSieuToc log file: " + file.getAbsolutePath(), e);
        }

        return new MigrationResult(
                "TheSieuToc",
                totalLines,
                successCount,
                skippedCount,
                errors.size(),
                errors,
                0,
                java.time.Instant.now()
        );
    }

    /**
     * Parse a single line from the log file
     * Pattern: [HH:mm dd/MM/yyyy] PlayerName | Serial | PIN | Amount | Telco | Status
     */
    private TheSieuTocData parseLine(String line) {
        Matcher matcher = LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            return null;
        }

        try {
            String timestampStr = matcher.group(1);
            String playerName = matcher.group(2).trim();
            String serial = matcher.group(3).trim();
            String pin = matcher.group(4).trim();
            String amountStr = matcher.group(5).trim();
            String telco = matcher.group(6).trim();
            String status = matcher.group(7).trim();

            // Parse timestamp
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr, DATE_FORMATTER);
            long timestampMillis = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            // Parse amount
            int amount;
            try {
                amount = Integer.parseInt(amountStr);
            } catch (NumberFormatException e) {
                // Try to remove commas if amount is formatted like "100,000"
                amount = Integer.parseInt(amountStr.replace(",", ""));
            }

            // Normalize status (Vietnamese to English)
            String normalizedStatus = status.equalsIgnoreCase("thanh cong") ? "SUCCESS" : "FAILED";

            return new TheSieuTocData(timestampMillis, playerName, serial, pin, amount, telco, normalizedStatus);
        } catch (Exception e) {
            log.debug("Failed to parse line: " + line, e);
            return null;
        }
    }

    /**
     * Check if a transaction with the same serial and pin already exists
     */
    private boolean isDuplicate(TheSieuTocData data) throws SQLException {
        List<CardPayment> existingPayments = databaseService.getDatabase().getCardDao().queryBuilder()
                .where()
                .eq("serial", data.serial())
                .and()
                .eq("pin", data.pin())
                .query();

        return !existingPayments.isEmpty();
    }

    /**
     * Create a CardPayment entity from parsed TheSieuToc data
     */
    private CardPayment createCardPaymentFromData(TheSieuTocData data) {
        try {
            // Resolve player UUID
            String playerUuidStr = getPlayerUUID(data.playerName());
            UUID playerUuid = UUID.fromString(playerUuidStr);

            // Get or create SPPlayer
            SPPlayer spPlayer = databaseService.getDatabase().getPlayerDao().queryForId(playerUuid);
            if (spPlayer == null) {
                spPlayer = new SPPlayer();
                spPlayer.setUuid(playerUuid);
                spPlayer.setName(data.playerName());
                databaseService.getDatabase().getPlayerDao().createOrUpdate(spPlayer);
            }

            // Map TheSieuToc telco to CardType
            CardType cardType = mapTelcoToCardType(data.telco());

            // Create CardPayment
            CardPayment payment = new CardPayment();
            payment.setPaymentID(UUID.randomUUID());
            payment.setPlayer(spPlayer);
            payment.setSerial(data.serial());
            payment.setPin(data.pin());
            payment.setPriceValue(data.amount());
            payment.setCardType(cardType);
            payment.setRefID("MIGRATED_" + System.nanoTime());
            payment.setTrueAmount(data.amount());
            payment.setAmount(data.amount());
            payment.setTimestamp(data.timestamp());
            payment.setApiProvider(org.simpmc.simppay.handler.data.CardAPI.THESIEUTOC);

            return payment;
        } catch (SQLException e) {
            log.error("Failed to create CardPayment from TheSieuToc data", e);
            return null;
        }
    }

    /**
     * Resolve player UUID from name
     * Tries: Online players → Offline players → Deterministic UUID from name
     */
    private String getPlayerUUID(String playerName) {
        // Try online players first
        var onlinePlayer = Bukkit.getPlayerExact(playerName);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId().toString();
        }

        // Try offline players
        @SuppressWarnings("deprecation")
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer.hasPlayedBefore()) {
            return offlinePlayer.getUniqueId().toString();
        }

        // Generate deterministic UUID from name (ensures consistency if migrated multiple times)
        return UUID.nameUUIDFromBytes(("TheSieuToc:" + playerName).getBytes()).toString();
    }

    /**
     * Map TheSieuToc telco name to SimpPay CardType
     */
    private CardType mapTelcoToCardType(String telco) {
        return switch (telco.toUpperCase()) {
            case "VIETTEL" -> CardType.VIETTEL;
            case "MOBIFONE", "MOBI" -> CardType.MOBIFONE;
            case "VINAPHONE", "VINA" -> CardType.VINAPHONE;
            case "VIETNAMOBILE", "VNMOBILE" -> CardType.VIETNAMOBILE;
            case "ZING" -> CardType.ZING;
            case "GATE" -> CardType.GATE;
            case "GARENA" -> CardType.GARENA;
            case "VCOIN" -> CardType.VCOIN;
            default -> CardType.GATE; // Default fallback to GATE for unknown types
        };
    }
}
