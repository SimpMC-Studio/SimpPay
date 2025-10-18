package org.simpmc.simppay.database.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.simpmc.simppay.data.milestone.MilestoneType;

import java.util.UUID;

/**
 * Records completed milestones for players (all-time history).
 * Used for tracking achievements and preventing duplicate reward execution.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DatabaseTable(tableName = "milestone_completions")
public class MilestoneCompletion {

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

    @DatabaseField(columnName = "completion_time", canBeNull = false, dataType = DataType.LONG, index = true)
    private long completionTime;

    @DatabaseField(columnName = "commands_executed", canBeNull = false, dataType = DataType.LONG_STRING)
    private String commandsExecuted; // JSON array of executed commands

    public MilestoneCompletion(SPPlayer player, String milestoneId, MilestoneType type, long targetAmount, String commandsExecuted) {
        this.player = player;
        this.milestoneId = milestoneId;
        this.type = type;
        this.targetAmount = targetAmount;
        this.completionTime = System.currentTimeMillis();
        this.commandsExecuted = commandsExecuted;
    }
}
