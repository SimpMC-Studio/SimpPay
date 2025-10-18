package org.simpmc.simppay.api.milestone;

import org.bukkit.entity.Player;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.data.milestone.MilestoneType;

import java.util.List;
import java.util.UUID;

/**
 * Public API for external plugins to interact with the milestone system
 * Provides read-only access to milestone data and notifications
 */
public interface MilestoneAPI {

    /**
     * Get all active milestones for a player
     *
     * @param playerUUID The player's UUID
     * @return List of active milestones
     */
    List<MilestoneConfig> getPlayerMilestones(UUID playerUUID);

    /**
     * Get all active server milestones
     *
     * @return List of server milestones
     */
    List<MilestoneConfig> getServerMilestones();

    /**
     * Get player's current total payment amount
     *
     * @param playerUUID The player's UUID
     * @return Player's total payment amount
     */
    long getPlayerTotalAmount(UUID playerUUID);

    /**
     * Get player's payment amount for a specific type
     *
     * @param playerUUID The player's UUID
     * @param type The milestone type (ALL, DAILY, WEEKLY, MONTHLY, YEARLY)
     * @return Payment amount for that type
     */
    long getPlayerAmountByType(UUID playerUUID, MilestoneType type);

    /**
     * Get server's total payment amount
     *
     * @return Server total payment amount
     */
    long getServerTotalAmount();

    /**
     * Get server's payment amount for a specific type
     *
     * @param type The milestone type
     * @return Payment amount for that type
     */
    long getServerAmountByType(MilestoneType type);

    /**
     * Check if a player has completed a specific milestone
     *
     * @param playerUUID The player's UUID
     * @param milestoneId The milestone ID
     * @return True if completed
     */
    boolean isPlayerMilestoneCompleted(UUID playerUUID, String milestoneId);

    /**
     * Check if server has completed a specific milestone
     *
     * @param milestoneId The milestone ID
     * @return True if completed
     */
    boolean isServerMilestoneCompleted(String milestoneId);

    /**
     * Send a progress update to a player
     * Can be used for custom displays
     *
     * @param player The player
     * @param milestone The milestone config
     * @param progressPercent Progress as percentage (0.0 - 100.0)
     */
    void sendProgressUpdate(Player player, MilestoneConfig milestone, double progressPercent);

    /**
     * Notify player of milestone completion
     * Triggers all notifications (title, sound, particles)
     *
     * @param player The player
     * @param milestone The completed milestone
     */
    void notifyPlayerMilestoneComplete(Player player, MilestoneConfig milestone);

    /**
     * Notify all players of server milestone completion
     *
     * @param milestone The completed milestone
     */
    void notifyServerMilestoneComplete(MilestoneConfig milestone);

    /**
     * Get cache statistics for monitoring
     *
     * @return Map of cache statistics
     */
    java.util.Map<String, Object> getCacheStats();
}
