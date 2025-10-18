package org.simpmc.simppay.service.cache;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Specialized cache for server-wide statistics with time-based metrics
 * Stores aggregated player recharge amounts across all time periods and payment types
 */
public class ServerStatsCache {

    private final TimedCache<String, ServerStatistics> cache;
    private final Supplier<ServerStatistics> loader;
    private static final String CACHE_KEY = "server_stats";

    /**
     * Creates a new server stats cache
     *
     * @param loader Function to load server stats from database
     */
    public ServerStatsCache(Supplier<ServerStatistics> loader) {
        this.loader = loader;
        // Cache expires after 2 minutes of inactivity
        this.cache = new TimedCache<>(2, TimeUnit.MINUTES);
    }

    /**
     * Get or load server's total recharge amount across all time
     *
     * @return Total amount recharged on server
     */
    public long getTotalAmount() {
        return getOrLoadStats().total();
    }

    /**
     * Get or load server's daily recharge amount
     *
     * @return Daily amount recharged on server
     */
    public long getDailyAmount() {
        return getOrLoadStats().daily();
    }

    /**
     * Get or load server's weekly recharge amount
     *
     * @return Weekly amount recharged on server
     */
    public long getWeeklyAmount() {
        return getOrLoadStats().weekly();
    }

    /**
     * Get or load server's monthly recharge amount
     *
     * @return Monthly amount recharged on server
     */
    public long getMonthlyAmount() {
        return getOrLoadStats().monthly();
    }

    /**
     * Get or load server's yearly recharge amount
     *
     * @return Yearly amount recharged on server
     */
    public long getYearlyAmount() {
        return getOrLoadStats().yearly();
    }

    /**
     * Get or load total amount recharged via card payments on server
     *
     * @return Total card payment amount on server
     */
    public long getCardTotalAmount() {
        return getOrLoadStats().cardTotal();
    }

    /**
     * Get or load total amount recharged via bank transfers on server
     *
     * @return Total bank transfer amount on server
     */
    public long getBankTotalAmount() {
        return getOrLoadStats().bankTotal();
    }

    /**
     * Refresh the server stats cache (forces reload from database)
     */
    public void refresh() {
        cache.remove(CACHE_KEY);
        // Next access will reload from database
    }

    /**
     * Clear the server stats cache
     */
    public void clear() {
        cache.clear();
    }

    private ServerStatistics getOrLoadStats() {
        return cache.getOrLoad(CACHE_KEY, a -> loader.get());
    }
}
