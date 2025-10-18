package org.simpmc.simppay.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.data.milestone.MilestoneType;

import java.util.UUID;

/**
 * Fired when a player's milestone progress is updated.
 * Useful for updating progress bars or displays in real-time.
 */
@Getter
public class MilestoneProgressEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final UUID playerUUID;
    private final MilestoneType type;
    private final MilestoneConfig milestoneConfig;
    private final long currentAmount;
    private final double progressPercentage; // 0.0 to 1.0
    private final Player player;

    public MilestoneProgressEvent(
            Player player,
            MilestoneType type,
            MilestoneConfig milestoneConfig,
            long currentAmount,
            double progressPercentage) {
        this.player = player;
        this.playerUUID = player.getUniqueId();
        this.type = type;
        this.milestoneConfig = milestoneConfig;
        this.currentAmount = currentAmount;
        this.progressPercentage = Math.min(1.0, progressPercentage); // Cap at 100%
    }

    /**
     * Get progress as a percentage (0-100)
     */
    public double getProgressPercent() {
        return progressPercentage * 100.0;
    }

    /**
     * Get remaining amount needed to complete milestone
     */
    public long getRemainingAmount() {
        return Math.max(0, milestoneConfig.getAmount() - currentAmount);
    }

    /**
     * Check if milestone is complete (progress >= 100%)
     */
    public boolean isComplete() {
        return progressPercentage >= 1.0;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
