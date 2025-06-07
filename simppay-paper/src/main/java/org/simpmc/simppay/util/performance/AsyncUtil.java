package org.simpmc.simppay.util.performance;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Performance utilities for async operations and optimization
 */
@UtilityClass
@Slf4j
public class AsyncUtil {
    
    private static final Executor ASYNC_EXECUTOR = ForkJoinPool.commonPool();
    
    /**
     * Run a task asynchronously with error handling
     */
    public static <T> CompletableFuture<T> runAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, ASYNC_EXECUTOR)
                .exceptionally(throwable -> {
                    log.error("Async task failed", throwable);
                    return null;
                });
    }
    
    /**
     * Run a task asynchronously with timeout
     */
    public static <T> CompletableFuture<T> runAsyncWithTimeout(Supplier<T> supplier, long timeout, TimeUnit unit) {
        CompletableFuture<T> future = runAsync(supplier);
        
        return future.orTimeout(timeout, unit)
                .exceptionally(throwable -> {
                    if (throwable instanceof java.util.concurrent.TimeoutException) {
                        log.warn("Async task timed out after {} {}", timeout, unit);
                    } else {
                        log.error("Async task failed", throwable);
                    }
                    return null;
                });
    }
    
    /**
     * Run a void task asynchronously
     */
    public static CompletableFuture<Void> runVoidAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, ASYNC_EXECUTOR)
                .exceptionally(throwable -> {
                    log.error("Async void task failed", throwable);
                    return null;
                });
    }
    
    /**
     * Chain multiple async operations
     */
    public static <T, U> CompletableFuture<U> chainAsync(Supplier<T> first, 
                                                        java.util.function.Function<T, U> second) {
        return runAsync(first)
                .thenCompose(result -> runAsync(() -> second.apply(result)));
    }
}
