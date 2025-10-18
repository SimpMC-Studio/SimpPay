package org.simpmc.simppay.event;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.data.milestone.MilestoneType;

import java.util.UUID;

/**
 * Fired when time-based milestones are reset (daily, weekly, monthly, yearly).
 * Fired for both player and server-wide resets.
 */
@Getter
public class MilestoneResetEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final MilestoneType type;
    private final String cycleId;
    private final boolean isPlayerReset; // false for server-wide
    private final UUID playerUUID; // null if server-wide
    private final int affectedPlayerCount; // for server resets

    /**
     * Create a player milestone reset event
     */
    public MilestoneResetEvent(UUID playerUUID, MilestoneType type, String cycleId) {
        this.playerUUID = playerUUID;
        this.type = type;
        this.cycleId = cycleId;
        this.isPlayerReset = true;
        this.affectedPlayerCount = 1;
    }

    /**
     * Create a server-wide milestone reset event
     */
    public MilestoneResetEvent(MilestoneType type, String cycleId, int affectedPlayerCount) {
        this.playerUUID = null;
        this.type = type;
        this.cycleId = cycleId;
        this.isPlayerReset = false;
        this.affectedPlayerCount = affectedPlayerCount;
    }

    /**
     * Get a human-readable description of the reset
     */
    public String getDescription() {
        String typeStr = type.toString().toLowerCase();
        if (isPlayerReset) {
            return "Player " + typeStr + " milestone reset";
        } else {
            return "Server-wide " + typeStr + " milestone reset (" + affectedPlayerCount + " players affected)";
        }
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
