package org.simpmc.simppay.config.types.data;

import de.exlll.configlib.Configuration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.simpmc.simppay.data.milestone.MilestoneType;

import java.util.List;
import java.util.UUID;

@Configuration
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MilestoneConfig {
    /** Unique identifier for this milestone (auto-generated if not provided) */
    public String id;

    /** Milestone type (ALL, DAILY, WEEKLY, MONTHLY, YEARLY) */
    public MilestoneType type;

    /** Amount threshold to reach for this milestone */
    public long amount;

    /** Display name for UI and notifications */
    public String displayName;

    /** Description of the milestone */
    public String description;

    /** Icon material for GUIs (e.g., "EMERALD", "GOLD_INGOT") */
    public String icon;

    /** Whether to announce this milestone globally when completed */
    public boolean announceGlobal;

    /** BossBar configuration for progress display */
    public BossBarConfig bossbar;

    /** Reward commands to execute when milestone is reached */
    public List<String> commands;

    /**
     * Constructor for backward compatibility (old config format)
     */
    public MilestoneConfig(MilestoneType type, long amount, BossBarConfig bossbar, List<String> commands) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.amount = amount;
        this.displayName = String.format("Milestone: %,d", amount);
        this.description = "";
        this.icon = "EMERALD";
        this.announceGlobal = false;
        this.bossbar = bossbar;
        this.commands = commands;
    }

    /**
     * Ensure ID is always set
     */
    public String getId() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    /**
     * Get a user-friendly display name
     */
    public String getDisplayName() {
        return displayName != null && !displayName.isBlank() ? displayName : String.format("Milestone: %,d", amount);
    }
}

