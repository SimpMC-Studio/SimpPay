package org.simpmc.simppay.listener.internal.cache;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.event.PaymentSuccessEvent;

public class CacheUpdaterListener implements Listener {
    public CacheUpdaterListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getFoliaLib().getScheduler().runLaterAsync(() -> {
                    plugin.getCacheDataService().updateServerDataCache();
                }, 1
        );
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();
        plugin.getFoliaLib().getScheduler().runAsync(task2 -> plugin.getDatabaseService().getPlayerService().createPlayer(event.getPlayer()));
        plugin.getCacheDataService().addPlayerToQueue(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();
        plugin.getCacheDataService().clearPlayerCache(event.getPlayer().getUniqueId());
        plugin.getPaymentService().clearPlayerBankCache(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();
        plugin.getCacheDataService().addPlayerToQueue(event.getPlayerUUID());
        plugin.getFoliaLib().getScheduler().runAsync(task2 -> plugin.getCacheDataService().updateServerDataCache());
    }
}
