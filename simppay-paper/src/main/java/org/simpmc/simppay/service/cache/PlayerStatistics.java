package org.simpmc.simppay.service.cache;

/**
 * Record representing player statistics snapshot
 * Immutable and thread-safe
 */
public record PlayerStatistics(
        long total,
        long daily,
        long weekly,
        long monthly,
        long yearly
) {}
