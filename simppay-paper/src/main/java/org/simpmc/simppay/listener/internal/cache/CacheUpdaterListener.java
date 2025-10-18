package org.simpmc.simppay.listener.internal.cache;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.IService;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.service.cache.CacheDataService;

/**
 * Listener responsible for updating player and server caches on events
 */
public class CacheUpdaterListener implements Listener, IService {
    private final SPPlugin plugin;

    public CacheUpdaterListener(SPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Initial server cache load
        plugin.getFoliaLib().getScheduler().runLaterAsync(
                task -> SPPlugin.getService(CacheDataService.class).updateServerDataCache(),
                1
        );
    }

    @Override
    public void shutdown() {
        // Event handlers automatically unregistered on plugin disable
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();
        plugin.getFoliaLib().getScheduler().runAsync(
                task -> SPPlugin.getService(DatabaseService.class).getPlayerService().createPlayer(event.getPlayer())
        );
        SPPlugin.getService(CacheDataService.class).addPlayerToQueue(event.getPlayer().getUniqueId());
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
        SPPlugin.getService(CacheDataService.class).addPlayerToQueue(event.getPlayerUUID());
        plugin.getFoliaLib().getScheduler().runAsync(
                task -> SPPlugin.getService(CacheDataService.class).updateServerDataCache()
        );
    }
}
