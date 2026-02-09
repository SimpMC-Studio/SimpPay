package org.simpmc.simppay.listener.internal.milestone;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.data.milestone.MilestoneType;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.event.PlayerMilestoneEvent;
import org.simpmc.simppay.event.ServerMilestoneEvent;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.MilestoneService;
import org.simpmc.simppay.service.database.PaymentLogService;
import org.simpmc.simppay.service.database.PlayerService;
import org.simpmc.simppay.util.CommandUtils;
import org.simpmc.simppay.util.MessageUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MilestoneListener implements Listener {
    public MilestoneListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(() -> {
            UUID uuid = event.getPlayer().getUniqueId();
            MilestoneService service = SPPlugin.getService(MilestoneService.class);
            MessageUtil.debug("Loading unified milestones for " + event.getPlayer().getName());

            // Load unified milestones (both player and server) and start cycling
            SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(task -> {
                service.loadUnifiedMilestonesForPlayer(uuid);
            }, 20 * 2);

        }, 20);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MilestoneService service = SPPlugin.getService(MilestoneService.class);
        UUID uuid = event.getPlayer().getUniqueId();

        // Stop cycling task
        service.stopCyclingTask(uuid);

        // Remove BossBar
        service.unifiedBossBar.remove(uuid);

        // Clear cached data
        service.activeMilestones.remove(uuid);
        service.cycleIndex.remove(uuid);
        service.currentMilestones.remove(uuid);
        service.currentAmountCache.remove(uuid);
        service.bossbarHidden.remove(uuid);

        MessageUtil.debug("Cleared unified cycling BossBar for " + event.getPlayer().getName());
    }

    /**
     * Phase 3.1: Redesigned player milestone detection with persistence and retroactive completion.
     * <p>
     * Key improvements:
     * - Checks ALL uncompleted milestones (retroactive completion support)
     * - Database-backed tracking prevents duplicate rewards
     * - Synchronous execution (no race conditions)
     * - Works correctly for large payments that cross multiple milestones
     */
    @EventHandler
    public void givePersonalMilestoneReward(PaymentSuccessEvent event) {
        // Run all DB-heavy milestone checks async to avoid blocking the global/main thread
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
            PlayerService playerService = SPPlugin.getService(DatabaseService.class).getPlayerService();
            PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();
            SPPlayer player = playerService.findByUuid(event.getPlayerUUID());

            if (player == null) {
                return;
            }

            // Batch query: fetch all time period amounts in 2 DB queries instead of 10
            Map<String, Long> amounts = paymentLogService.getPlayerAmountsBatch(player);

            // Check all milestone types for retroactive completion
            for (MilestoneType type : MilestoneType.values()) {
                // Get current player balance for this milestone type from batch results
                double playerBalance = switch (type) {
                    case ALL -> amounts.getOrDefault("total", 0L);
                    case DAILY -> amounts.getOrDefault("daily", 0L);
                    case WEEKLY -> amounts.getOrDefault("weekly", 0L);
                    case MONTHLY -> amounts.getOrDefault("monthly", 0L);
                    case YEARLY -> amounts.getOrDefault("yearly", 0L);
                    default -> 0;
                };

                // Get all uncompleted milestones for this type
                List<MilestoneConfig> uncompletedMilestones = milestoneService.getUncompletedPlayerMilestones(event.getPlayerUUID(), type);

                // Check each uncompleted milestone
                for (MilestoneConfig config : uncompletedMilestones) {
                    // Simple check: if balance >= milestone amount, complete it
                    if (playerBalance >= config.amount) {
                        // Mark as completed FIRST (prevents duplicate rewards if plugin crashes during execution)
                        milestoneService.markPlayerMilestoneCompleted(event.getPlayerUUID(), type, config.amount);

                        // Award rewards - CommandUtils.dispatchCommand already uses runNextTick()
                        for (String command : config.getCommands()) {
                            String formattedCommand = PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(event.getPlayerUUID()), command);
                            CommandUtils.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                            MessageUtil.debug("Ran milestone command: " + formattedCommand);
                        }

                        // Remove from in-memory cache
                        List<MilestoneConfig> cachedMilestones = milestoneService.currentMilestones.get(event.getPlayerUUID());
                        if (cachedMilestones != null) {
                            cachedMilestones.removeIf(m -> m.type == type && m.amount == config.amount);
                        }

                        MessageUtil.debug("Player " + player.getName() + " completed milestone: " + type.name() + " " + config.amount);

                        // Fire player milestone event on main/global thread
                        SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(t2 -> {
                            SPPlugin.getInstance().getServer().getPluginManager().callEvent(new PlayerMilestoneEvent(event.getPlayerUUID()));
                        });
                    }
                }
            }

            // Refresh unified milestones after completion (includes both player and server milestones)
            SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(t -> {
                milestoneService.refreshUnifiedMilestones(event.getPlayerUUID());
            });
        });
    }


    /**
     * Phase 3.1: Redesigned server milestone detection with persistence and retroactive completion.
     * <p>
     * Same improvements as player milestones - retroactive, persistent, synchronous.
     */
    @EventHandler
    public void giveServerMilestoneReward(PaymentSuccessEvent event) {
        // Run all DB-heavy server milestone checks async to avoid blocking the global/main thread
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
            PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();

            // Check all milestone types for retroactive completion
            for (MilestoneType type : MilestoneType.values()) {
                // Get current server balance for this milestone type
                double serverBalance = switch (type) {
                    case ALL -> paymentLogService.getEntireServerAmount();
                    case DAILY -> paymentLogService.getEntireServerDailyAmount();
                    case WEEKLY -> paymentLogService.getEntireServerWeeklyAmount();
                    case MONTHLY -> paymentLogService.getEntireServerMonthlyAmount();
                    case YEARLY -> paymentLogService.getEntireServerYearlyAmount();
                    default -> 0;
                };

                // Get all uncompleted milestones for this type
                List<MilestoneConfig> uncompletedMilestones = milestoneService.getUncompletedServerMilestones(type);

                // Check each uncompleted milestone
                for (MilestoneConfig config : uncompletedMilestones) {
                    // Simple check: if balance >= milestone amount, complete it
                    if (serverBalance >= config.amount) {
                        // Mark as completed FIRST (prevents duplicate rewards)
                        milestoneService.markServerMilestoneCompleted(type, config.amount);

                        // Award rewards to all online players (queued execution)
                        Deque<String> commands = new ArrayDeque<>();
                        for (String command : config.getCommands()) {
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                String formattedCommand = PlaceholderAPI.setPlaceholders(player, command);
                                commands.add(formattedCommand);
                            }
                        }

                        // Execute commands with delay (1 command per second)
                        SPPlugin.getInstance().getFoliaLib().getScheduler().runTimer(task2 -> {
                            if (commands.isEmpty()) {
                                task2.cancel();
                                return;
                            }
                            String command = commands.poll();
                            if (command == null) {
                                task2.cancel();
                                return;
                            }
                            CommandUtils.dispatchCommand(Bukkit.getConsoleSender(), command);
                            MessageUtil.debug("Ran server milestone command: " + command);
                        }, 1, 20);

                        MessageUtil.debug("Server completed milestone: " + type.name() + " " + config.amount);

                        // Fire server milestone event on main/global thread
                        SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(t2 -> {
                            SPPlugin.getInstance().getServer().getPluginManager().callEvent(new ServerMilestoneEvent(config));
                        });
                    }
                }
            }

            // Refresh unified milestones for all online players after server milestone completion
            SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(t -> {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    milestoneService.refreshUnifiedMilestones(onlinePlayer.getUniqueId());
                }
            });
        });
    }

}
