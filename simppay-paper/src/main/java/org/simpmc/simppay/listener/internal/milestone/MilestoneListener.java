package org.simpmc.simppay.listener.internal.milestone;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.IService;
import org.simpmc.simppay.service.MilestoneService;
import org.simpmc.simppay.service.database.PaymentLogService;
import org.simpmc.simppay.service.database.PlayerService;
import org.simpmc.simppay.util.MessageUtil;

/**
 * Refactored Milestone Listener using new MilestoneService architecture.
 * Handles player joins, quits, and milestone reward triggering on payments.
 */
@Slf4j
public class MilestoneListener implements Listener, IService {
    private final SPPlugin plugin;

    public MilestoneListener(SPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void setup() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        MessageUtil.info("Milestone Listener initialized");
    }

    @Override
    public void shutdown() {
        // Event handlers automatically unregistered on plugin disable
    }

    /**
     * Handle player join - load their milestones
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
            if (milestoneService != null) {
                // Load player milestones with small delay to ensure they're fully loaded
                SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(() -> {
                    milestoneService.reloadPlayerMilestones(player.getUniqueId());
                    MessageUtil.debug("Loaded milestones for player " + player.getName());
                }, 1);
            }
        } catch (Exception e) {
            log.error("Error handling player join for milestone loading", e);
        }
    }

    /**
     * Handle player quit - clean up milestone cache
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try {
            MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
            if (milestoneService != null) {
                milestoneService.getMilestoneCache().clearPlayerMilestones(event.getPlayer().getUniqueId());
                MessageUtil.debug("Cleaned up milestone cache for player " + event.getPlayer().getName());
            }
        } catch (Exception e) {
            log.error("Error handling player quit for milestone cleanup", e);
        }
    }

    /**
     * Handle successful payment - check for milestone completion
     */
    @EventHandler
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        try {
            Player player = SPPlugin.getInstance().getServer().getPlayer(event.getPlayerUUID());
            if (player == null) {
                return; // Player is offline
            }

            MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
            if (milestoneService == null) {
                return;
            }

            PlayerService playerService = SPPlugin.getService(DatabaseService.class).getPlayerService();
            PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();

            // Get the player's new total amount
            double newTotalAmount = paymentLogService.getPlayerTotalAmount(
                    playerService.findByUuid(event.getPlayerUUID())
            );

            // Check for player milestone completion
            milestoneService.checkPlayerMilestones(player, (long) newTotalAmount);

            // Get server's new total amount
            long newServerAmount = paymentLogService.getEntireServerAmount();

            // Check for server milestone completion
            milestoneService.checkServerMilestones(newServerAmount);

            MessageUtil.debug("Payment success - checked milestones for player " + player.getName());
        } catch (Exception e) {
            log.error("Error handling payment success for milestones", e);
        }
    }
}
