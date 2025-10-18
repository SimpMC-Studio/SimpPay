package org.simpmc.simppay.migration.dotman;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.database.entities.CardPayment;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.migration.MigrationResult;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.util.MessageUtil;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles migration of DotMan plugin data from MySQL/H2 database
 * Reads from DotMan's log table and imports into SimpPay
 */
@Slf4j
public class DotManMigration {

    private static final String BATCH_SIZE = "1000";
    private final DatabaseService simpPayDatabase;

    public DotManMigration() {
        this.simpPayDatabase = SPPlugin.getService(DatabaseService.class);
    }

    /**
     * Migrate transactions from DotMan database
     *
     * @param dotmanPath Path to DotMan plugin folder
     * @return Migration result with statistics
     */
    public MigrationResult migrateFromDatabase(File dotmanPath) {
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int skippedCount = 0;
        int totalRecords = 0;

        Connection dotmanConnection = null;

        try {
            // Read DotMan config to get database connection info
            File configFile = new File(dotmanPath, "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            String dbEngine = config.getString("database.engine", "h2");
            dotmanConnection = createDotManConnection(dbEngine, config, dotmanPath);

            if (dotmanConnection == null) {
                String error = "Failed to connect to DotMan database";
                errors.add(error);
                log.error(error);
                return createResult("DotMan", 0, 0, 0, errors, System.currentTimeMillis() - startTime);
            }

            MessageUtil.info("Connected to DotMan database: " + dbEngine);

            // Query DotMan's log table
            String query = "SELECT * FROM log ORDER BY id ASC";
            Statement statement = dotmanConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                totalRecords++;
                try {
                    DotManData data = parseDotManRecord(resultSet);

                    if (data == null || !data.isValid()) {
                        errors.add("Record " + totalRecords + ": Invalid data");
                        continue;
                    }

                    // Check if already migrated
                    if (isDuplicateDotMan(data)) {
                        skippedCount++;
                        continue;
                    }

                    // Create and save card payment
                    CardPayment payment = createCardPaymentFromDotMan(data);
                    if (payment != null) {
                        simpPayDatabase.getDatabase().getCardDao().create(payment);
                        successCount++;

                        if (successCount % 100 == 0) {
                            MessageUtil.info("DotMan Migration Progress: " + successCount + "/" + totalRecords);
                        }
                    } else {
                        errors.add("Record " + totalRecords + ": Failed to create payment object");
                    }

                } catch (Exception e) {
                    errors.add("Record " + totalRecords + ": " + e.getMessage());
                    log.debug("Error processing DotMan record", e);
                }
            }

            resultSet.close();
            statement.close();

            long duration = System.currentTimeMillis() - startTime;
            MessageUtil.info("DotMan migration completed: " + successCount + " success, " + skippedCount + " skipped, " + errors.size() + " errors");
            return createResult("DotMan", totalRecords, successCount, skippedCount, errors, duration);

        } catch (SQLException e) {
            String error = "Database error during DotMan migration: " + e.getMessage();
            errors.add(error);
            log.error(error, e);
            return createResult("DotMan", totalRecords, successCount, skippedCount, errors, System.currentTimeMillis() - startTime);
        } finally {
            if (dotmanConnection != null) {
                try {
                    dotmanConnection.close();
                    MessageUtil.info("Closed DotMan database connection");
                } catch (SQLException e) {
                    log.error("Failed to close DotMan connection", e);
                }
            }
        }
    }

    /**
     * Create a database connection to DotMan's database
     */
    private Connection createDotManConnection(String engine, YamlConfiguration config, File dotmanPath) throws SQLException {
        if ("h2".equalsIgnoreCase(engine)) {
            return createH2Connection(config, dotmanPath);
        } else if ("mysql".equalsIgnoreCase(engine) || "mariadb".equalsIgnoreCase(engine)) {
            return createMySQLConnection(config);
        } else {
            log.warn("Unknown database engine: " + engine);
            return null;
        }
    }

    /**
     * Create H2 database connection (local file-based)
     */
    private Connection createH2Connection(YamlConfiguration config, File dotmanPath) throws SQLException {
        String dbFile = config.getString("database.h2.file", "dotman");
        String dbPath = new File(dotmanPath, dbFile).getAbsolutePath();
        String jdbcUrl = "jdbc:h2:" + dbPath;

        try {
            Class.forName("org.h2.Driver");
            return DriverManager.getConnection(jdbcUrl);
        } catch (ClassNotFoundException e) {
            log.error("H2 driver not found. Cannot migrate from H2 database", e);
            return null;
        }
    }

    /**
     * Create MySQL/MariaDB connection
     */
    private Connection createMySQLConnection(YamlConfiguration config) throws SQLException {
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String user = config.getString("database.mysql.user", "root");
        String password = config.getString("database.mysql.password", "");
        String database = config.getString("database.mysql.database", "dotman");

        String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            return DriverManager.getConnection(jdbcUrl, user, password);
        } catch (ClassNotFoundException e) {
            // Try MariaDB driver
            try {
                Class.forName("org.mariadb.jdbc.Driver");
                return DriverManager.getConnection(jdbcUrl, user, password);
            } catch (ClassNotFoundException e2) {
                log.error("MySQL/MariaDB driver not found. Cannot migrate from MySQL database", e2);
                return null;
            }
        }
    }

    /**
     * Parse a record from DotMan's log table
     */
    private DotManData parseDotManRecord(ResultSet rs) throws SQLException {
        try {
            String logId = rs.getString("id");
            String playerUuid = rs.getString("player_uuid");
            String playerName = rs.getString("player_name");
            String cardType = rs.getString("card_type");
            String cardSerial = rs.getString("card_seri");
            String cardPin = rs.getString("card_pin");
            long amount = rs.getLong("amount");
            long pointsReceived = rs.getLong("point_received");
            long timestamp = rs.getTimestamp("created_at").getTime();
            boolean success = "true".equalsIgnoreCase(rs.getString("status"));

            return new DotManData(
                    logId,
                    playerUuid,
                    playerName,
                    cardType,
                    cardSerial,
                    cardPin,
                    amount,
                    pointsReceived,
                    timestamp,
                    success
            );
        } catch (SQLException e) {
            log.debug("Failed to parse DotMan record", e);
            return null;
        }
    }

    /**
     * Check if a transaction from DotMan has already been migrated
     */
    private boolean isDuplicateDotMan(DotManData data) throws SQLException {
        String refIDPrefix = "DOTMAN_" + data.logId();
        List<CardPayment> existing = simpPayDatabase.getDatabase().getCardDao().queryBuilder()
                .where()
                .like("ref_id", refIDPrefix + "%")
                .query();

        return !existing.isEmpty();
    }

    /**
     * Create a CardPayment entity from DotMan data
     */
    private CardPayment createCardPaymentFromDotMan(DotManData data) {
        try {
            UUID playerUuid = UUID.fromString(data.playerUuid());

            // Get or create SPPlayer
            SPPlayer spPlayer = simpPayDatabase.getDatabase().getPlayerDao().queryForId(playerUuid);
            if (spPlayer == null) {
                spPlayer = new SPPlayer();
                spPlayer.setUuid(playerUuid);
                spPlayer.setName(data.playerName());
                simpPayDatabase.getDatabase().getPlayerDao().createOrUpdate(spPlayer);
            }

            // Map DotMan card type to SimpPay CardType
            CardType cardType = mapDotManCardType(data.cardType());

            // For manual topups, use special values
            String serial = data.isManualTopup() ? "MANUAL" : data.cardSerial();
            String pin = data.isManualTopup() ? "--" : data.cardPin();

            CardPayment payment = new CardPayment();
            payment.setPaymentID(UUID.randomUUID());
            payment.setPlayer(spPlayer);
            payment.setSerial(serial);
            payment.setPin(pin);
            payment.setPriceValue(data.amount());
            payment.setCardType(cardType);
            payment.setRefID("DOTMAN_" + data.logId());
            payment.setTrueAmount(data.amount());
            payment.setAmount(data.amount());
            payment.setTimestamp(data.timestamp());
            payment.setApiProvider(org.simpmc.simppay.handler.data.CardAPI.THESIEUTOC); // Use THESIEUTOC as fallback for migrated data

            return payment;
        } catch (SQLException e) {
            log.error("Failed to create CardPayment from DotMan data", e);
            return null;
        }
    }

    /**
     * Map DotMan card type to SimpPay CardType
     */
    private CardType mapDotManCardType(String dotmanType) {
        if (dotmanType == null) {
            return CardType.GATE;
        }

        return switch (dotmanType.toUpperCase()) {
            case "VIETTEL" -> CardType.VIETTEL;
            case "MOBIFONE", "MOBI" -> CardType.MOBIFONE;
            case "VINAPHONE", "VINA" -> CardType.VINAPHONE;
            case "VIETNAMOBILE", "VNMOBILE" -> CardType.VIETNAMOBILE;
            case "ZING" -> CardType.ZING;
            case "GATE" -> CardType.GATE;
            case "GARENA" -> CardType.GARENA;
            case "VCOIN" -> CardType.VCOIN;
            case "MANUAL", "THỦ CÔNG" -> CardType.GATE; // Manual topups use GATE as placeholder
            default -> CardType.GATE; // Default fallback
        };
    }

    /**
     * Helper to create migration result
     */
    private MigrationResult createResult(String source, int total, int success, int skipped, List<String> errors, long duration) {
        return new MigrationResult(
                source,
                total,
                success,
                skipped,
                errors.size(),
                errors,
                duration,
                java.time.Instant.now()
        );
    }
}
