package org.simpmc.simppay.database.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.simpmc.simppay.data.milestone.MilestoneType;

/**
 * Records completed server-wide milestones (all-time history).
 * Tracks when entire server reaches milestone thresholds.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@DatabaseTable(tableName = "server_milestone_completions")
public class ServerMilestoneCompletion {

    @DatabaseField(columnName = "id", id = true, generatedId = true)
    private long id;

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

    @DatabaseField(columnName = "players_online_count", canBeNull = false)
    private int playersOnlineCount;

    public ServerMilestoneCompletion(String milestoneId, MilestoneType type, long targetAmount, String commandsExecuted, int playersOnlineCount) {
        this.milestoneId = milestoneId;
        this.type = type;
        this.targetAmount = targetAmount;
        this.completionTime = System.currentTimeMillis();
        this.commandsExecuted = commandsExecuted;
        this.playersOnlineCount = playersOnlineCount;
    }
}
