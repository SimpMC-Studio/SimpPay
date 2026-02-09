package org.simpmc.simppay.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.api.DatabaseSettings;
import org.simpmc.simppay.database.entities.*;

import java.sql.SQLException;
import java.util.UUID;

public class Database {

    private final HikariDataSource dataSource;
    private final DataSourceConnectionSource connectionSource;
    @Getter
    private final Dao<BankingPayment, UUID> bankDao;
    @Getter
    private final Dao<CardPayment, UUID> cardDao;
    @Getter
    private final Dao<PlayerStreakPayment, UUID> streakDao;
    @Getter
    private final Dao<SPPlayer, UUID> playerDao;
    @Getter
    private final Dao<PlayerData, UUID> playerDataDao;
    @Getter
    private final Dao<MilestoneCompletion, UUID> milestoneDao;
    @Getter
    private final Dao<LeaderboardCache, String> leaderboardCacheDao;

    public Database(DatabaseSettings db) throws SQLException {
        // Retrieve config values from your ConfigManager
        // 'database' is used here to determine the type ("mysql" or "sqlite")
        String dbType = db.getType();
        String host = db.getHost();
        int port = db.getPort();
        String username = db.getUsername();
        String password = db.getPassword();

        // HikariCP configuration
        HikariConfig config = new HikariConfig();
        String jdbcUrl;
        String driverClassName;

        if (dbType.equalsIgnoreCase("mysql")) {
            // For MySQL, you need a database name.
            // Ensure you have added a field in your config for the MySQL database name.
            String dbName = db.getDatabase(); // e.g., "mydatabase"
            jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&serverTimezone=UTC";
            driverClassName = "com.mysql.cj.jdbc.Driver";

            // Additional MySQL-specific HikariCP settings
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.setUsername(username);
            config.setPassword(password);
        } else if (dbType.equalsIgnoreCase("h2")) {
            jdbcUrl = "jdbc:h2:file:" + SPPlugin.getInstance().getDataFolder().getAbsolutePath() + "/simppay.db;AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=TRUE";
            driverClassName = "org.h2.Driver";
            config.setUsername("root");
            config.setPassword("password");
        } else {
            throw new RuntimeException("Unsupported database type: " + dbType);
        }

        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName(driverClassName);

        // General HikariCP settings
        // H2 needs a larger pool since async milestone/cache tasks run concurrent DB queries
        config.setMaximumPoolSize(dbType.equalsIgnoreCase("h2") ? 20 : 10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000);        // 10 minutes
        config.setMaxLifetime(1800000);       // 30 minutes

        dataSource = new HikariDataSource(config);
        connectionSource = new DataSourceConnectionSource(dataSource, jdbcUrl);

        // Create tables if they do not exist

        TableUtils.createTableIfNotExists(connectionSource, SPPlayer.class);
        TableUtils.createTableIfNotExists(connectionSource, BankingPayment.class);
        TableUtils.createTableIfNotExists(connectionSource, CardPayment.class);
        TableUtils.createTableIfNotExists(connectionSource, PlayerStreakPayment.class);
        TableUtils.createTableIfNotExists(connectionSource, PlayerData.class);
        TableUtils.createTableIfNotExists(connectionSource, MilestoneCompletion.class);
        TableUtils.createTableIfNotExists(connectionSource, LeaderboardCache.class);

        // Create the DAOs
        playerDao = DaoManager.createDao(connectionSource, SPPlayer.class);
        bankDao = DaoManager.createDao(connectionSource, BankingPayment.class);
        cardDao = DaoManager.createDao(connectionSource, CardPayment.class);
        streakDao = DaoManager.createDao(connectionSource, PlayerStreakPayment.class);
        playerDataDao = DaoManager.createDao(connectionSource, PlayerData.class);
        milestoneDao = DaoManager.createDao(connectionSource, MilestoneCompletion.class);
        leaderboardCacheDao = DaoManager.createDao(connectionSource, LeaderboardCache.class);

    }

    /**
     * Closes both the ORMLite connection source and the HikariCP data source.
     */
    public void close() {
        if (connectionSource != null) {
            connectionSource.closeQuietly();
        }
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
