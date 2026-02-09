package org.simpmc.simppay.listener.internal.cache;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.service.cache.CacheDataService;

/**
 * Cache Updater Listener - Phase 2.1 Redesign
 * <p>
 * Key Changes:
 * - Removed queue-based updates (no more addPlayerToQueue)
 * - Synchronous cache updates on payment events (real-time)
 * - Async server cache updates remain for performance
 */
public class CacheUpdaterListener implements Listener {
    public CacheUpdaterListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Initialize server cache on startup
        plugin.getFoliaLib().getScheduler().runLaterAsync(() -> {
                    SPPlugin.getService(CacheDataService.class).updateServerDataCache();
                }, 1
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();
        plugin.getFoliaLib().getScheduler().runAsync(task2 -> {
            SPPlugin.getService(DatabaseService.class).getPlayerService().createPlayer(event.getPlayer());
            // Load player cache asynchronously on join (not critical path)
            SPPlugin.getService(CacheDataService.class).updatePlayerCacheSync(event.getPlayer().getUniqueId());
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SPPlugin.getService(CacheDataService.class).clearPlayerCache(event.getPlayer().getUniqueId());
        SPPlugin.getService(PaymentService.class).clearPlayerBankCache(event.getPlayer().getUniqueId());
        SPPlugin.getService(PaymentService.class).cancelBankPayment(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();

        // Async player cache update - ConcurrentHashMap + AtomicLong is thread-safe
        // Avoids blocking global/main thread which caused HikariCP pool exhaustion
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            SPPlugin.getService(CacheDataService.class).updatePlayerCacheSync(event.getPlayerUUID());
        });

        // Async server cache update (less critical, can be delayed slightly)
        plugin.getFoliaLib().getScheduler().runAsync(task2 -> {
            SPPlugin.getService(CacheDataService.class).updateServerDataCache();
        });
    }
}
