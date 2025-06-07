package org.simpmc.simppay.util.performance;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting utility for API calls and operations
 */
@UtilityClass
@Slf4j
public class RateLimiter {
    
    private static final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    
    /**
     * Check if an operation is allowed based on rate limit
     */
    public static boolean isAllowed(String key, int maxRequests, long windowMs) {
        TokenBucket bucket = buckets.computeIfAbsent(key, 
                k -> new TokenBucket(maxRequests, windowMs));
        return bucket.tryConsume();
    }
    
    /**
     * Get remaining tokens for a key
     */
    public static long getRemainingTokens(String key, int maxRequests, long windowMs) {
        TokenBucket bucket = buckets.computeIfAbsent(key, 
                k -> new TokenBucket(maxRequests, windowMs));
        return bucket.getAvailableTokens();
    }
    
    /**
     * Clear rate limit for a key
     */
    public static void clearRateLimit(String key) {
        buckets.remove(key);
    }
    
    private static class TokenBucket {
        private final long capacity;
        private final long refillPeriodMs;
        private final AtomicLong tokens;
        private volatile long lastRefillTime;
        
        public TokenBucket(long capacity, long refillPeriodMs) {
            this.capacity = capacity;
            this.refillPeriodMs = refillPeriodMs;
            this.tokens = new AtomicLong(capacity);
            this.lastRefillTime = System.currentTimeMillis();
        }
        
        public boolean tryConsume() {
            refill();
            
            long currentTokens = tokens.get();
            if (currentTokens > 0) {
                return tokens.compareAndSet(currentTokens, currentTokens - 1);
            }
            return false;
        }
        
        public long getAvailableTokens() {
            refill();
            return tokens.get();
        }
        
        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            
            if (timePassed >= refillPeriodMs) {
                tokens.set(capacity);
                lastRefillTime = now;
            }
        }
    }
}
