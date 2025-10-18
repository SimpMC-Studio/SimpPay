package org.simpmc.simppay.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Generic thread-safe cache wrapper using Caffeine for expiring entries
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class TimedCache<K, V> {
    private final Cache<K, V> cache;
    private final long duration;
    private final TimeUnit timeUnit;

    /**
     * Creates a new timed cache with automatic expiration
     *
     * @param duration How long entries should remain in cache
     * @param timeUnit The time unit for duration
     */
    public TimedCache(long duration, TimeUnit timeUnit) {
        this(duration, timeUnit, null);
    }

    /**
     * Creates a new timed cache with automatic expiration and removal listener
     *
     * @param duration How long entries should remain in cache
     * @param timeUnit The time unit for duration
     * @param removalListener Called when entries are removed from cache
     */
    public TimedCache(long duration, TimeUnit timeUnit, @Nullable RemovalListener<K, V> removalListener) {
        this.duration = duration;
        this.timeUnit = timeUnit;

        var builder = Caffeine.newBuilder()
                .expireAfterWrite(duration, timeUnit)
                .recordStats();

        if (removalListener != null) {
            builder.removalListener(removalListener);
        }

        this.cache = builder.build();
    }

    /**
     * Get a value from the cache
     *
     * @param key The key to lookup
     * @return The cached value, or null if not present or expired
     */
    @Nullable
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    /**
     * Get a value with automatic loading
     *
     * @param key The key to lookup
     * @param loader Function to load the value if not cached
     * @return The cached or newly loaded value
     */
    @NotNull
    public V getOrLoad(@NotNull K key, @NotNull Function<K, V> loader) {
        return cache.get(key, loader);
    }

    /**
     * Put a value into the cache
     *
     * @param key The key
     * @param value The value to cache
     */
    public void put(@NotNull K key, @NotNull V value) {
        cache.put(key, value);
    }

    /**
     * Remove a specific key from the cache
     *
     * @param key The key to remove
     */
    public void remove(@NotNull K key) {
        cache.invalidate(key);
    }

    /**
     * Clear all entries from the cache
     */
    public void clear() {
        cache.invalidateAll();
    }


}
