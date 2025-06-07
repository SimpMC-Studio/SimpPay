package org.simpmc.simppay.service.cache.optimized;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.database.entities.SPPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Optimized cache data service with better performance and thread safety
 * Replaces the original "mess" CacheDataService
 */
@Slf4j
@Getter
public class OptimizedCacheDataService {
    
    private static OptimizedCacheDataService instance;
    
    // Server-wide cache with atomic operations for thread safety
    private final AtomicLong serverTotalValue = new AtomicLong(0);
    private final AtomicLong serverDailyTotalValue = new AtomicLong(0);
    private final AtomicLong serverWeeklyTotalValue = new AtomicLong(0);
    private final AtomicLong serverMonthlyTotalValue = new AtomicLong(0);
    private final AtomicLong serverYearlyTotalValue = new AtomicLong(0);
    private final AtomicLong cardTotalValue = new AtomicLong(0);
    private final AtomicLong bankTotalValue = new AtomicLong(0);
    
    // Player-specific cache with concurrent maps
    private final ConcurrentHashMap<UUID, AtomicLong> playerDailyTotalValue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerWeeklyTotalValue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerMonthlyTotalValue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerYearlyTotalValue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerTotalValue = new ConcurrentHashMap<>();
    
    // Read-write lock for cache operations
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // Cache validity tracking
    private volatile long lastServerCacheUpdate = 0;
    private final ConcurrentHashMap<UUID, Long> lastPlayerCacheUpdate = new ConcurrentHashMap<>();
    
    // Cache TTL in milliseconds (5 minutes default)
    private static final long CACHE_TTL = 5 * 60 * 1000;
    
    private OptimizedCacheDataService() {
        log.info("Initialized OptimizedCacheDataService");
    }
    
    public static OptimizedCacheDataService getInstance() {
        if (instance == null) {
            synchronized (OptimizedCacheDataService.class) {
                if (instance == null) {
                    instance = new OptimizedCacheDataService();
                }
            }
        }
        return instance;
    }
    
    /**
     * Update server-wide cache data with batch operations
     */
    public void updateServerDataCache() {
        cacheLock.writeLock().lock();
        try {
            SPPlugin plugin = SPPlugin.getInstance();
            var paymentLogService = plugin.getDatabaseService().getPaymentLogService();
            
            // Batch update all server values
            serverTotalValue.set(paymentLogService.getEntireServerAmount());
            serverDailyTotalValue.set(paymentLogService.getEntireServerDailyAmount());
            serverWeeklyTotalValue.set(paymentLogService.getEntireServerWeeklyAmount());
            serverMonthlyTotalValue.set(paymentLogService.getEntireServerMonthlyAmount());
            serverYearlyTotalValue.set(paymentLogService.getEntireServerYearlyAmount());
            cardTotalValue.set(paymentLogService.getEntireServerCardAmount());
            bankTotalValue.set(paymentLogService.getEntireServerBankAmount());
            
            lastServerCacheUpdate = System.currentTimeMillis();
            log.debug("Updated server cache data");
        } catch (Exception e) {
            log.error("Failed to update server cache data", e);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Update player-specific cache data
     */
    public void updatePlayerDataCache(UUID playerUUID) {
        if (playerUUID == null) return;
        
        cacheLock.writeLock().lock();
        try {
            SPPlugin plugin = SPPlugin.getInstance();
            SPPlayer player = plugin.getDatabaseService().getPlayerService().findByUuid(playerUUID);
            
            if (player == null) {
                log.warn("Player not found for UUID: {}", playerUUID);
                return;
            }
            
            var paymentLogService = plugin.getDatabaseService().getPaymentLogService();
            
            // Update all player values atomically
            playerDailyTotalValue.computeIfAbsent(playerUUID, k -> new AtomicLong(0))
                    .set(paymentLogService.getPlayerDailyAmount(player));
            playerWeeklyTotalValue.computeIfAbsent(playerUUID, k -> new AtomicLong(0))
                    .set(paymentLogService.getPlayerWeeklyAmount(player));
            playerMonthlyTotalValue.computeIfAbsent(playerUUID, k -> new AtomicLong(0))
                    .set(paymentLogService.getPlayerMonthlyAmount(player));
            playerYearlyTotalValue.computeIfAbsent(playerUUID, k -> new AtomicLong(0))
                    .set(paymentLogService.getPlayerYearlyAmount(player));
            playerTotalValue.computeIfAbsent(playerUUID, k -> new AtomicLong(0))
                    .set(paymentLogService.getPlayerTotalAmount(player).longValue());
            
            lastPlayerCacheUpdate.put(playerUUID, System.currentTimeMillis());
            log.debug("Updated player cache data for: {}", playerUUID);
        } catch (Exception e) {
            log.error("Failed to update player cache data for: {}", playerUUID, e);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Get player daily total with cache validation
     */
    public long getPlayerDailyTotal(UUID playerUUID) {
        if (isPlayerCacheExpired(playerUUID)) {
            updatePlayerDataCache(playerUUID);
        }
        
        cacheLock.readLock().lock();
        try {
            return playerDailyTotalValue.getOrDefault(playerUUID, new AtomicLong(0)).get();
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Get server total with cache validation
     */
    public long getServerTotal() {
        if (isServerCacheExpired()) {
            updateServerDataCache();
        }
        
        cacheLock.readLock().lock();
        try {
            return serverTotalValue.get();
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Check if server cache is expired
     */
    private boolean isServerCacheExpired() {
        return System.currentTimeMillis() - lastServerCacheUpdate > CACHE_TTL;
    }
    
    /**
     * Check if player cache is expired
     */
    private boolean isPlayerCacheExpired(UUID playerUUID) {
        Long lastUpdate = lastPlayerCacheUpdate.get(playerUUID);
        return lastUpdate == null || System.currentTimeMillis() - lastUpdate > CACHE_TTL;
    }
    
    /**
     * Clear all cache data
     */
    public void clearAllCache() {
        cacheLock.writeLock().lock();
        try {
            // Clear server cache
            serverTotalValue.set(0);
            serverDailyTotalValue.set(0);
            serverWeeklyTotalValue.set(0);
            serverMonthlyTotalValue.set(0);
            serverYearlyTotalValue.set(0);
            cardTotalValue.set(0);
            bankTotalValue.set(0);
            lastServerCacheUpdate = 0;
            
            // Clear player cache
            playerDailyTotalValue.clear();
            playerWeeklyTotalValue.clear();
            playerMonthlyTotalValue.clear();
            playerYearlyTotalValue.clear();
            playerTotalValue.clear();
            lastPlayerCacheUpdate.clear();
            
            log.info("Cleared all cache data");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Remove player from cache
     */
    public void removePlayerFromCache(UUID playerUUID) {
        cacheLock.writeLock().lock();
        try {
            playerDailyTotalValue.remove(playerUUID);
            playerWeeklyTotalValue.remove(playerUUID);
            playerMonthlyTotalValue.remove(playerUUID);
            playerYearlyTotalValue.remove(playerUUID);
            playerTotalValue.remove(playerUUID);
            lastPlayerCacheUpdate.remove(playerUUID);
            
            log.debug("Removed player from cache: {}", playerUUID);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Get cache statistics for monitoring
     */
    public CacheStats getCacheStats() {
        cacheLock.readLock().lock();
        try {
            return new CacheStats(
                    playerDailyTotalValue.size(),
                    lastServerCacheUpdate,
                    System.currentTimeMillis() - lastServerCacheUpdate < CACHE_TTL,
                    (int) lastPlayerCacheUpdate.values().stream()
                            .filter(lastUpdate -> System.currentTimeMillis() - lastUpdate < CACHE_TTL)
                            .count()
            );
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Cache statistics record
     */
    public record CacheStats(
            int cachedPlayersCount,
            long lastServerCacheUpdate,
            boolean serverCacheValid,
            int validPlayerCachesCount
    ) {}
}
