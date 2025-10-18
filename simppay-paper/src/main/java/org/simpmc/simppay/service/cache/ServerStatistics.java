package org.simpmc.simppay.service.cache;

/**
 * Record representing server-wide statistics snapshot
 * Immutable and thread-safe
 */
public record ServerStatistics(
        long total,
        long daily,
        long weekly,
        long monthly,
        long yearly,
        long cardTotal,
        long bankTotal
) {}
