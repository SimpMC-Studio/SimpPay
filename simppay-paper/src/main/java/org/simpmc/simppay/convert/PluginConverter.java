package org.simpmc.simppay.convert;

import com.j256.ormlite.dao.Dao;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.cache.CacheDataService;
import org.simpmc.simppay.util.MessageUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;

public abstract class PluginConverter {

    public abstract String getName();

    public boolean supportsMySQL() { return false; }
    public boolean supportsFlatfile() { return false; }
    public boolean supportsCredentials() { return false; }

    public ImportResult importFromMySQL(String host, int port, String database, String username, String password) {
        ImportResult result = new ImportResult();
        result.addFailed("MySQL import không được hỗ trợ bởi converter này.");
        return result;
    }

    public ImportResult importFromFlatfile(File logFile) {
        ImportResult result = new ImportResult();
        result.addFailed("Flatfile import không được hỗ trợ bởi converter này.");
        return result;
    }

    public ImportResult importCredentials() {
        ImportResult result = new ImportResult();
        result.addFailed("Credentials import không được hỗ trợ bởi converter này.");
        return result;
    }

    protected SPPlayer findOrCreatePlayer(UUID playerUUID, String name) throws SQLException {
        DatabaseService dbService = SPPlugin.getService(DatabaseService.class);
        Dao<SPPlayer, UUID> playerDao = dbService.getDatabase().getPlayerDao();
        SPPlayer spPlayer = playerDao.queryForId(playerUUID);
        if (spPlayer == null) {
            spPlayer = new SPPlayer();
            spPlayer.setUuid(playerUUID);
            spPlayer.setName(name != null ? name : playerUUID.toString());
            playerDao.createOrUpdate(spPlayer);
        }
        return spPlayer;
    }

    protected void rebuildCache(Set<UUID> playerUUIDs) {
        if (playerUUIDs.isEmpty()) return;

        CacheDataService cacheService = CacheDataService.getInstance();
        if (cacheService == null) return;

        for (UUID uuid : playerUUIDs) {
            try {
                cacheService.updatePlayerCacheSync(uuid);
            } catch (Exception e) {
                MessageUtil.debug("[Import] Cache rebuild failed for " + uuid + ": " + e.getMessage());
            }
        }

        try {
            cacheService.updateServerDataCache();
        } catch (Exception e) {
            MessageUtil.debug("[Import] Server cache rebuild failed: " + e.getMessage());
        }
    }
}
