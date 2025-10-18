package org.simpmc.simppay.listener.internal.player.database;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.IService;
import org.simpmc.simppay.service.cache.CacheDataService;

/**
 * Listener responsible for persisting payment success events to database
 */
public class SuccessDatabaseHandlingListener implements Listener, IService {
    private final SPPlugin plugin;

    public SuccessDatabaseHandlingListener(SPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void shutdown() {
        // Event handlers automatically unregistered on plugin disable
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void updateDBz(PaymentSuccessEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();

        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            SPPlugin.getService(DatabaseService.class).getPaymentLogService().addPayment(event.getPayment());
        });
    }

    @EventHandler
    public void updateQueue(PaymentSuccessEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();

        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            SPPlugin.getService(CacheDataService.class).addPlayerToQueue(event.getPlayerUUID());
        });
    }
}
