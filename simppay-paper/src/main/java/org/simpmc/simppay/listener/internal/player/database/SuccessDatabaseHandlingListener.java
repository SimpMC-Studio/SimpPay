package org.simpmc.simppay.listener.internal.player.database;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.database.StreakService;

/**
 * Phase 2.1: Removed updateQueue method - cache updates now handled synchronously in CacheUpdaterListener
 * Phase 5: Added streak update on payment success
 */
public class SuccessDatabaseHandlingListener implements Listener {
    private final StreakService streakService;

    public SuccessDatabaseHandlingListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.streakService = new StreakService();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void updateDBz(PaymentSuccessEvent event) {
        SPPlugin plugin = SPPlugin.getInstance();

        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            SPPlugin.getService(DatabaseService.class).getPaymentLogService().addPayment(event.getPayment());

            // Phase 5: Update player streak
            streakService.updateStreak(event.getPlayerUUID());
        });
    }

    // Note: Queue-based cache update method removed in Phase 2.1
    // Cache now updates synchronously in CacheUpdaterListener
}
