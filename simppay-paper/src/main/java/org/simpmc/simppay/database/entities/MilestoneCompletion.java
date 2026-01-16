package org.simpmc.simppay.database.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.simpmc.simppay.data.milestone.MilestoneType;

import java.util.Date;
import java.util.UUID;

@NoArgsConstructor
@Data
@DatabaseTable(tableName = "milestone_completions")
public class MilestoneCompletion {
    @DatabaseField(generatedId = true, dataType = DataType.UUID)
    private UUID id;

    @DatabaseField(columnName = "player_uuid", foreign = true, foreignAutoRefresh = true, canBeNull = true)
    private SPPlayer player;

    @DatabaseField(columnName = "milestone_type", canBeNull = false, dataType = DataType.ENUM_STRING)
    private MilestoneType milestoneType;

    @DatabaseField(columnName = "milestone_amount", canBeNull = false)
    private long milestoneAmount;

    @DatabaseField(columnName = "completed_at", canBeNull = false, dataType = DataType.DATE)
    private Date completedAt;

    @DatabaseField(columnName = "server_wide", canBeNull = false)
    private boolean serverWide;

    public MilestoneCompletion(SPPlayer player, MilestoneType milestoneType, long milestoneAmount, boolean serverWide) {
        this.player = player;
        this.milestoneType = milestoneType;
        this.milestoneAmount = milestoneAmount;
        this.completedAt = new Date();
        this.serverWide = serverWide;
    }
}
