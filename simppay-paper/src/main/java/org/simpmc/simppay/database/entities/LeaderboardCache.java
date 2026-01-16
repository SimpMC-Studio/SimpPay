package org.simpmc.simppay.database.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
@DatabaseTable(tableName = "leaderboard_cache")
public class LeaderboardCache {
    @DatabaseField(columnName = "cache_key", id = true, canBeNull = false)
    private String cacheKey; // Format: "{type}_{rank}" e.g., "daily_1", "weekly_5", "monthly_10"

    @DatabaseField(columnName = "player_uuid", dataType = DataType.UUID)
    private UUID playerUUID;

    @DatabaseField(columnName = "player_name", canBeNull = false)
    private String playerName;

    @DatabaseField(columnName = "amount", canBeNull = false)
    private long amount;

    @DatabaseField(columnName = "rank", canBeNull = false)
    private int rank;

    @DatabaseField(columnName = "last_updated", canBeNull = false, dataType = DataType.DATE)
    private Date lastUpdated;

    public LeaderboardCache(String cacheKey, UUID playerUUID, String playerName, long amount, int rank) {
        this.cacheKey = cacheKey;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.amount = amount;
        this.rank = rank;
        this.lastUpdated = new Date();
    }
}
