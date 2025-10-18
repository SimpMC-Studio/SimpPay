package org.simpmc.simppay.service.cache;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Specialized cache for player statistics with time-based metrics
 * Stores player's total, daily, weekly, monthly, and yearly recharge amounts
 */
public class PlayerStatsCache {

    private final TimedCache<UUID, PlayerStatistics> cache;
    private final Function<UUID, PlayerStatistics> loader;

    /**
     * Creates a new player stats cache
     *
     * @param loader Function to load player stats from database
     */
    public PlayerStatsCache(Function<UUID, PlayerStatistics> loader) {
        this.loader = loader;
        // Cache expires after 5 minutes of inactivity
        this.cache = new TimedCache<>(5, TimeUnit.MINUTES);
    }

    /**
     * Get or load player's total recharge amount
     *
     * @param playerUUID The player's UUID
     * @return Total amount recharged
     */
    public long getTotalAmount(UUID playerUUID) {
        return cache.getOrLoad(playerUUID, loader).total();
    }

    /**
     * Get or load player's daily recharge amount
     *
     * @param playerUUID The player's UUID
     * @return Daily amount recharged
     */
    public long getDailyAmount(UUID playerUUID) {
        return cache.getOrLoad(playerUUID, loader).daily();
    }

    /**
     * Get or load player's weekly recharge amount
     *
     * @param playerUUID The player's UUID
     * @return Weekly amount recharged
     */
    public long getWeeklyAmount(UUID playerUUID) {
        return cache.getOrLoad(playerUUID, loader).weekly();
    }

    /**
     * Get or load player's monthly recharge amount
     *
     * @param playerUUID The player's UUID
     * @return Monthly amount recharged
     */
    public long getMonthlyAmount(UUID playerUUID) {
        return cache.getOrLoad(playerUUID, loader).monthly();
    }

    /**
     * Get or load player's yearly recharge amount
     *
     * @param playerUUID The player's UUID
     * @return Yearly amount recharged
     */
    public long getYearlyAmount(UUID playerUUID) {
        return cache.getOrLoad(playerUUID, loader).yearly();
    }

    /**
     * Invalidate cache entry for a specific player
     *
     * @param playerUUID The player's UUID
     */
    public void invalidate(UUID playerUUID) {
        cache.remove(playerUUID);
    }

    /**
     * Clear all cached player data
     */
    public void clear() {
        cache.clear();
    }
}
