package org.simpmc.simppay.service.cache;

/**
 * Sealed interface defining all cache types used in SimpPay
 */
public sealed interface CacheType permits
        CacheType.PlayerStats,
        CacheType.ServerStats {

    /**
     * Player-specific statistics cache type
     */
    record PlayerStats() implements CacheType {}

    /**
     * Server-wide statistics cache type
     */
    record ServerStats() implements CacheType {}
}
