package org.simpmc.simppay.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MocNapConfig;
import org.simpmc.simppay.config.types.MocNapServerConfig;
import org.simpmc.simppay.config.types.data.BossBarConfig;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.data.milestone.MilestoneType;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.service.database.PaymentLogService;
import org.simpmc.simppay.service.milestone.*;
import org.simpmc.simppay.util.MessageUtil;

import java.util.*;

/**
 * Refactored Milestone Service using new architecture.
 * Orchestrates MilestoneCache, MilestoneRewardExecutor, and MilestoneResetScheduler.
 */
@Slf4j
public class MilestoneService implements IService {

    @Getter
    private final MilestoneCache cache;

    @Getter
    private final MilestoneRewardExecutor rewardExecutor;

    @Getter
    private final MilestoneResetScheduler resetScheduler;

    @Getter
    private final MilestoneNotificationService notificationService;

    private final PaymentLogService paymentLogService;

    // Delay between server milestone reward commands (in ticks)
    private static final long COMMAND_DELAY_TICKS = 1;

    public MilestoneService(DatabaseService databaseService) {
        this.paymentLogService = databaseService.getPaymentLogService();
        this.cache = new MilestoneCache();
        this.rewardExecutor = new MilestoneRewardExecutor(databaseService);
        this.notificationService = new MilestoneNotificationService();
        this.rewardExecutor.setNotificationService(this.notificationService);
        this.resetScheduler = new MilestoneResetScheduler(
                cache,
                this::reloadServerMilestones,
                () -> reloadAllPlayerMilestones()
        );
    }

    @Override
    public void setup() {
        MessageUtil.info("Setting up Milestone Service...");
        cache.clear();
        resetScheduler.setup();
        reloadServerMilestones();
        for (Player player : Bukkit.getOnlinePlayers()) {
            reloadPlayerMilestones(player.getUniqueId());
        }
        MessageUtil.info("Milestone Service initialized successfully");
    }

    @Override
    public void shutdown() {
        MessageUtil.info("Shutting down Milestone Service...");
        cache.clear();
    }

    /**
     * Reload server milestones from configuration (async via FoliaLib)
     */
    public void reloadServerMilestones() {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                long serverAmount = paymentLogService.getEntireServerAmount();
                MocNapServerConfig config = ConfigManager.getInstance().getConfig(MocNapServerConfig.class);

                if (config == null || config.mocnap == null) {
                    MessageUtil.warn("Server milestone config is null or empty");
                    return;
                }

                List<MilestoneConfig> activeMilestones = new ArrayList<>();

                for (Map.Entry<MilestoneType, List<MilestoneConfig>> entry : config.mocnap.entrySet()) {
                    MilestoneType type = entry.getKey();
                    if (type == null) continue;

                    for (MilestoneConfig milestone : entry.getValue()) {
                        if (milestone == null) continue;

                        // Filter out already completed milestones
                        if (serverAmount < milestone.getAmount()) {
                            activeMilestones.add(milestone);
                            MessageUtil.debug("Server milestone loaded: " + milestone.getAmount() + " (" + type + ")");
                        }
                    }
                }

                cache.setServerMilestones(activeMilestones);
                MessageUtil.info("Server milestones reloaded: " + activeMilestones.size() + " active");
                log.info("Server milestones reloaded: {} active out of {} total", activeMilestones.size(), config.mocnap.values().stream().mapToInt(List::size).sum());
            } catch (Exception e) {
                MessageUtil.warn("Error reloading server milestones: " + e.getMessage());
                log.error("Error reloading server milestones", e);
            }
        });
    }

    /**
     * Reload milestones for a specific player (async via FoliaLib)
     */
    public void reloadPlayerMilestones(UUID playerUUID) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                Player player = Bukkit.getPlayer(playerUUID);
                if (player == null) {
                    cache.clearPlayerMilestones(playerUUID);
                    return;
                }

                long playerAmount = paymentLogService.getPlayerTotalAmount(
                        SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(playerUUID)
                ).longValue();

                MocNapConfig config = ConfigManager.getInstance().getConfig(MocNapConfig.class);
                if (config == null || config.mocnap == null) {
                    MessageUtil.warn("Player milestone config is null or empty");
                    return;
                }

                List<MilestoneConfig> activeMilestones = new ArrayList<>();

                for (Map.Entry<MilestoneType, List<MilestoneConfig>> entry : config.mocnap.entrySet()) {
                    MilestoneType type = entry.getKey();
                    if (type == null) continue;

                    for (MilestoneConfig milestone : entry.getValue()) {
                        if (milestone == null) continue;

                        // Filter out already completed milestones
                        if (playerAmount < milestone.getAmount()) {
                            activeMilestones.add(milestone);
                        }
                    }
                }

                cache.setPlayerMilestones(playerUUID, activeMilestones);
                MessageUtil.debug("Player " + player.getName() + " milestones reloaded: " + activeMilestones.size() + " active");
            } catch (Exception e) {
                MessageUtil.warn("Error reloading milestones for player " + playerUUID + ": " + e.getMessage());
                log.error("Error reloading milestones for player {}", playerUUID, e);
            }
        });
    }

    /**
     * Reload all player milestones
     */
    private void reloadAllPlayerMilestones() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            reloadPlayerMilestones(player.getUniqueId());
        }
    }

    /**
     * Check and process player milestone completion (async via FoliaLib)
     */
    public void checkPlayerMilestones(Player player, long newAmount) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                UUID playerUUID = player.getUniqueId();
                List<MilestoneConfig> activeMilestones = cache.getPlayerMilestones(playerUUID);

                if (activeMilestones == null || activeMilestones.isEmpty()) {
                    return;
                }

                List<MilestoneConfig> completedMilestones = new ArrayList<>();

                for (MilestoneConfig milestone : activeMilestones) {
                    if (milestone == null) continue;

                    // Check if this payment crossed the milestone threshold
                    if (newAmount >= milestone.getAmount()) {
                        completedMilestones.add(milestone);
                        MessageUtil.info("Player " + player.getName() + " completed milestone: " + milestone.getAmount());
                        log.info("Player {} completed milestone: {}", player.getName(), milestone.getAmount());
                    }
                }

                // Execute rewards for all completed milestones
                for (MilestoneConfig milestone : completedMilestones) {
                    try {
                        rewardExecutor.executePlayerMilestoneRewards(player, milestone);
                        cache.removePlayerMilestone(playerUUID, milestone);
                    } catch (Exception e) {
                        log.error("Error executing player milestone rewards for {}", player.getName(), e);
                    }
                }
            } catch (Exception e) {
                log.error("Error checking player milestones for {}", player.getUniqueId(), e);
            }
        });
    }

    /**
     * Check and process server milestone completion (async via FoliaLib)
     */
    public void checkServerMilestones(long newServerAmount) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                List<MilestoneConfig> activeMilestones = cache.getServerMilestones();

                if (activeMilestones == null || activeMilestones.isEmpty()) {
                    return;
                }

                List<MilestoneConfig> completedMilestones = new ArrayList<>();

                for (MilestoneConfig milestone : activeMilestones) {
                    if (milestone == null) continue;

                    // Check if server crossed the milestone threshold
                    if (newServerAmount >= milestone.getAmount()) {
                        completedMilestones.add(milestone);
                        MessageUtil.info("Server completed milestone: " + milestone.getAmount());
                        log.info("Server completed milestone: {}", milestone.getAmount());
                    }
                }

                // Execute rewards for all completed milestones
                for (MilestoneConfig milestone : completedMilestones) {
                    try {
                        rewardExecutor.executeServerMilestoneRewards(milestone, COMMAND_DELAY_TICKS);
                        cache.removeServerMilestone(milestone);
                    } catch (Exception e) {
                        log.error("Error executing server milestone rewards", e);
                    }
                }
            } catch (Exception e) {
                log.error("Error checking server milestones", e);
            }
        });
    }

    /**
     * Get player's active milestones
     */
    public List<MilestoneConfig> getPlayerMilestones(UUID playerUUID) {
        return cache.getPlayerMilestones(playerUUID);
    }

    /**
     * Get server's active milestones
     */
    public List<MilestoneConfig> getServerMilestones() {
        return cache.getServerMilestones();
    }

    /**
     * Get the milestone cache for advanced operations
     */
    public MilestoneCache getMilestoneCache() {
        return cache;
    }

    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getCacheStats() {
        return cache.getStats();
    }
}
