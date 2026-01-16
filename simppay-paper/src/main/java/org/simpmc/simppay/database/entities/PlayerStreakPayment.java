package org.simpmc.simppay.database.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@Data
@DatabaseTable(tableName = "player_streak")
public class PlayerStreakPayment {
    @DatabaseField(columnName = "player_uuid", id = true, dataType = DataType.UUID)
    private UUID playerUUID;

    @DatabaseField(columnName = "last_recharge_date", dataType = DataType.DATE)
    private Date lastRechargeDate;

    @DatabaseField(columnName = "current_streak")
    private int currentStreak;

    @DatabaseField(columnName = "best_streak")
    private int bestStreak;

    @DatabaseField(columnName = "last_reward_tier")
    private int lastRewardTier;

    @DatabaseField(columnName = "claimed_today")
    private boolean claimedToday;
}
