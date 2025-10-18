package org.simpmc.simppay.database.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.simpmc.simppay.data.milestone.MilestoneType;

/**
 * Tracks the current progress for active (incomplete) player milestones.
 * This persists milestone progress across server restarts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DatabaseTable(tableName = "milestone_progress")
public class MilestoneProgress {

    @DatabaseField(columnName = "id", id = true, generatedId = true)
    private long id;

    @DatabaseField(columnName = "player_uuid", canBeNull = false, foreign = true, foreignAutoRefresh = true, index = true)
    private SPPlayer player;

    @DatabaseField(columnName = "milestone_id", canBeNull = false)
    private String milestoneId;

    @DatabaseField(columnName = "milestone_type", canBeNull = false)
    private MilestoneType type;

    @DatabaseField(columnName = "target_amount", canBeNull = false)
    private long targetAmount;

    @DatabaseField(columnName = "current_progress", canBeNull = false)
    private long currentProgress;

    @DatabaseField(columnName = "last_updated", canBeNull = false, dataType = DataType.LONG, index = true)
    private long lastUpdated;

    @DatabaseField(columnName = "reset_cycle_id", canBeNull = true)
    private String resetCycleId; // e.g., "2024-10-19" for daily, "week-42-2024" for weekly

    public MilestoneProgress(SPPlayer player, String milestoneId, MilestoneType type, long targetAmount, long currentProgress, String resetCycleId) {
        this.player = player;
        this.milestoneId = milestoneId;
        this.type = type;
        this.targetAmount = targetAmount;
        this.currentProgress = currentProgress;
        this.lastUpdated = System.currentTimeMillis();
        this.resetCycleId = resetCycleId;
    }
}
