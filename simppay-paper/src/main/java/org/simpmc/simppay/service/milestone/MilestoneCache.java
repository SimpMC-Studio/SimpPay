package org.simpmc.simppay.service.milestone;

import lombok.Getter;
import lombok.Synchronized;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.data.milestone.MilestoneType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe cache for milestone tracking.
 * Manages both player and server-wide milestone state.
 */
@Getter
public class MilestoneCache {

    // Player milestones: UUID -> List of active milestones
    private final ConcurrentHashMap<UUID, CopyOnWriteArrayList<MilestoneConfig>> playerMilestones = new ConcurrentHashMap<>();

    // Server-wide milestones: List of active milestones (all players see same list)
    private final CopyOnWriteArrayList<MilestoneConfig> serverMilestones = new CopyOnWriteArrayList<>();

    // Track reset cycles for time-based milestones
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<MilestoneType, String>> playerResetCycles = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<MilestoneType, String> serverResetCycles = new ConcurrentHashMap<>();

    // Last update timestamps
    private final ConcurrentHashMap<UUID, Long> playerLastUpdate = new ConcurrentHashMap<>();
    private long serverLastUpdate = 0;

    /**
     * Get all active milestones for a player
     */
    public List<MilestoneConfig> getPlayerMilestones(UUID playerUUID) {
        return playerMilestones.getOrDefault(playerUUID, new CopyOnWriteArrayList<>());
    }

    /**
     * Set player milestones (replaces existing)
     */
    public void setPlayerMilestones(UUID playerUUID, List<MilestoneConfig> milestones) {
        CopyOnWriteArrayList<MilestoneConfig> list = new CopyOnWriteArrayList<>(milestones);
        playerMilestones.put(playerUUID, list);
        playerLastUpdate.put(playerUUID, System.currentTimeMillis());
    }

    /**
     * Add a milestone to a player's active list
     */
    public void addPlayerMilestone(UUID playerUUID, MilestoneConfig milestone) {
        playerMilestones.computeIfAbsent(playerUUID, k -> new CopyOnWriteArrayList<>()).add(milestone);
        playerLastUpdate.put(playerUUID, System.currentTimeMillis());
    }

    /**
     * Remove a completed milestone from a player's active list
     */
    public boolean removePlayerMilestone(UUID playerUUID, MilestoneConfig milestone) {
        CopyOnWriteArrayList<MilestoneConfig> list = playerMilestones.get(playerUUID);
        if (list != null) {
            boolean removed = list.remove(milestone);
            if (removed) {
                playerLastUpdate.put(playerUUID, System.currentTimeMillis());
            }
            return removed;
        }
        return false;
    }

    /**
     * Clear all milestones for a player
     */
    public void clearPlayerMilestones(UUID playerUUID) {
        playerMilestones.remove(playerUUID);
        playerLastUpdate.remove(playerUUID);
        playerResetCycles.remove(playerUUID);
    }

    /**
     * Set server milestones (replaces existing)
     */
    public void setServerMilestones(List<MilestoneConfig> milestones) {
        serverMilestones.clear();
        serverMilestones.addAll(milestones);
        serverLastUpdate = System.currentTimeMillis();
    }

    /**
     * Remove a completed server milestone
     */
    public boolean removeServerMilestone(MilestoneConfig milestone) {
        boolean removed = serverMilestones.remove(milestone);
        if (removed) {
            serverLastUpdate = System.currentTimeMillis();
        }
        return removed;
    }

    /**
     * Get the reset cycle ID for a player milestone type
     */
    public String getPlayerResetCycle(UUID playerUUID, MilestoneType type) {
        return playerResetCycles.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).get(type);
    }

    /**
     * Set the reset cycle ID for a player milestone type
     */
    public void setPlayerResetCycle(UUID playerUUID, MilestoneType type, String cycleId) {
        playerResetCycles.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>()).put(type, cycleId);
    }

    /**
     * Get the reset cycle ID for a server milestone type
     */
    public String getServerResetCycle(MilestoneType type) {
        return serverResetCycles.get(type);
    }

    /**
     * Set the reset cycle ID for a server milestone type
     */
    public void setServerResetCycle(MilestoneType type, String cycleId) {
        serverResetCycles.put(type, cycleId);
    }

    /**
     * Get when a player's milestones were last updated
     */
    public long getPlayerLastUpdate(UUID playerUUID) {
        return playerLastUpdate.getOrDefault(playerUUID, 0L);
    }

    /**
     * Get when server milestones were last updated
     */
    public long getServerLastUpdate() {
        return serverLastUpdate;
    }

    /**
     * Check if a player's milestone cache is stale (older than maxAge milliseconds)
     */
    public boolean isPlayerMilestonesCacheStale(UUID playerUUID, long maxAgeMillis) {
        long lastUpdate = getPlayerLastUpdate(playerUUID);
        return System.currentTimeMillis() - lastUpdate > maxAgeMillis;
    }

    /**
     * Check if server milestone cache is stale
     */
    public boolean isServerMilestonesCacheStale(long maxAgeMillis) {
        return System.currentTimeMillis() - serverLastUpdate > maxAgeMillis;
    }

    /**
     * Clear all cached data
     */
    public void clear() {
        playerMilestones.clear();
        serverMilestones.clear();
        playerResetCycles.clear();
        serverResetCycles.clear();
        playerLastUpdate.clear();
        serverLastUpdate = 0;
    }

    /**
     * Get cache statistics (for debugging)
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cachedPlayers", playerMilestones.size());
        stats.put("totalPlayerMilestones", playerMilestones.values().stream().mapToInt(List::size).sum());
        stats.put("serverMilestones", serverMilestones.size());
        stats.put("serverLastUpdate", serverLastUpdate);
        return stats;
    }
}
