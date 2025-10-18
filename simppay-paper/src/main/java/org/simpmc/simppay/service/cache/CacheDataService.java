package org.simpmc.simppay.service.cache;

import lombok.Getter;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.IService;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service responsible for caching player and server statistics for fast placeholder lookups.
 * Uses Caffeine cache with automatic expiration to ensure data freshness.
 *
 * Cache invalidation strategy:
 * - Player cache: Expires after 5 minutes of inactivity
 * - Server cache: Expires after 2 minutes of inactivity
 * - Both caches refresh on PaymentSuccessEvent
 */
@Getter
public class CacheDataService implements IService {

    private final PlayerStatsCache playerStatsCache;
    private final ServerStatsCache serverStatsCache;
    private final ConcurrentLinkedQueue<UUID> playerQueue = new ConcurrentLinkedQueue<>();

    public CacheDataService() {
        this.playerStatsCache = new PlayerStatsCache(this::loadPlayerStats);
        this.serverStatsCache = new ServerStatsCache(this::loadServerStats);
    }

    @Override
    public void setup() {
        // Async queue processing for batch player cache updates
        SPPlugin.getInstance().getFoliaLib().getScheduler()
                .runTimerAsync(this::processPlayerQueue, 1, 20L);

        // Initial server cache load
        serverStatsCache.refresh();
    }

    @Override
    public void shutdown() {
        // Clear all caches on shutdown
        playerStatsCache.clear();
        serverStatsCache.clear();
        playerQueue.clear();
    }

    /**
     * Add a player to the cache update queue
     *
     * @param playerUUID The player's UUID
     */
    public void addPlayerToQueue(UUID playerUUID) {
        playerQueue.offer(playerUUID);
    }

    /**
     * Clear all cached data for a specific player
     *
     * @param playerUUID The player's UUID
     */
    public void clearPlayerCache(UUID playerUUID) {
        playerStatsCache.invalidate(playerUUID);
    }

    /**
     * Clear all cached data (player and server)
     */
    public void clearAllCache() {
        playerStatsCache.clear();
        serverStatsCache.clear();
    }

    /**
     * Refresh server cache from database
     */
    public void updateServerDataCache() {
        serverStatsCache.refresh();
    }

    /**
     * Process pending player cache updates from the queue
     */
    private void processPlayerQueue() {
        while (!playerQueue.isEmpty()) {
            UUID playerUUID = playerQueue.poll();

            // Get player from database
            SPPlayer player = SPPlugin.getService(DatabaseService.class)
                    .getPlayerService()
                    .findByUuid(playerUUID);

            if (player == null) {
                // Re-queue if player not found yet (may be initializing)
                playerQueue.offer(playerUUID);
            }
            // Else: Cache will load automatically on next access via playerStatsCache
        }
    }

    /**
     * Load player statistics from database (called by cache loader)
     *
     * @param playerUUID The player's UUID
     * @return Loaded player statistics
     */
    private PlayerStatistics loadPlayerStats(UUID playerUUID) {
        SPPlayer player = SPPlugin.getService(DatabaseService.class)
                .getPlayerService()
                .findByUuid(playerUUID);

        if (player == null) {
            return new PlayerStatistics(0, 0, 0, 0, 0);
        }

        var paymentLog = SPPlugin.getService(DatabaseService.class).getPaymentLogService();

        long total = Math.round(paymentLog.getPlayerTotalAmount(player));
        long daily = Math.round(paymentLog.getPlayerDailyAmount(player));
        long weekly = Math.round(paymentLog.getPlayerWeeklyAmount(player));
        long monthly = Math.round(paymentLog.getPlayerMonthlyAmount(player));
        long yearly = Math.round(paymentLog.getPlayerYearlyAmount(player));

        return new PlayerStatistics(total, daily, weekly, monthly, yearly);
    }

    /**
     * Load server statistics from database (called by cache loader)
     *
     * @return Loaded server statistics
     */
    private ServerStatistics loadServerStats() {
        var paymentLog = SPPlugin.getService(DatabaseService.class).getPaymentLogService();

        long total = Math.round(paymentLog.getEntireServerAmount());
        long daily = Math.round(paymentLog.getEntireServerDailyAmount());
        long weekly = Math.round(paymentLog.getEntireServerWeeklyAmount());
        long monthly = Math.round(paymentLog.getEntireServerMonthlyAmount());
        long yearly = Math.round(paymentLog.getEntireServerYearlyAmount());
        long cardAmount = Math.round(paymentLog.getEntireServerCardAmount());
        long bankAmount = Math.round(paymentLog.getEntireServerBankAmount());

        return new ServerStatistics(total, daily, weekly, monthly, yearly, cardAmount, bankAmount);
    }
}
