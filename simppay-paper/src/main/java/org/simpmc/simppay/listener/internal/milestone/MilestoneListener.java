package org.simpmc.simppay.listener.internal.milestone;

import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.bossbar.BossBar;
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
import org.simpmc.simppay.util.MessageUtil;

import java.util.*;

public class MilestoneListener implements Listener {
    public MilestoneListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(() -> {
            UUID uuid = event.getPlayer().getUniqueId();
            MilestoneService service = SPPlugin.getService(MilestoneService.class);
            MessageUtil.debug("Loading player milestone for " + event.getPlayer().getName());

            List<BossBar> serverBossbars = service.serverBossbars.stream().map(ObjectObjectMutablePair::right).toList();
            SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(task -> {
                service.loadPlayerMilestone(uuid);
            }, 20 * 2).thenAccept(task -> {
                List<BossBar> playerBossbars = service.playerBossBars.get(uuid).stream().map(ObjectObjectMutablePair::right).toList();
                for (BossBar bar : playerBossbars) {
                    SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(event.getPlayer(), task2 -> {
                        bar.addViewer(event.getPlayer());
                    });
                }
            });

            // have to load after player milestone is loaded
            for (BossBar bar : serverBossbars) {
                bar.addViewer(event.getPlayer());
            }

        }, 20);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MilestoneService service = SPPlugin.getService(MilestoneService.class);
        service.playerBossBars.remove(event.getPlayer().getUniqueId());
        service.playerCurrentMilestones.remove(event.getPlayer().getUniqueId());
        MessageUtil.debug("Cleared cache bossbar and currentmilestones " + event.getPlayer().getName());
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
        MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
        PlayerService playerService = SPPlugin.getService(DatabaseService.class).getPlayerService();
        PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();
        SPPlayer player = playerService.findByUuid(event.getPlayerUUID());

        if (player == null) {
            return;
        }

        // Check all milestone types for retroactive completion
        for (MilestoneType type : MilestoneType.values()) {
            // Get current player balance for this milestone type
            double playerBalance = switch (type) {
                case ALL -> paymentLogService.getPlayerTotalAmount(player);
                case DAILY -> paymentLogService.getPlayerDailyAmount(player);
                case WEEKLY -> paymentLogService.getPlayerWeeklyAmount(player);
                case MONTHLY -> paymentLogService.getPlayerMonthlyAmount(player);
                case YEARLY -> paymentLogService.getPlayerYearlyAmount(player);
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

                    // Award rewards
                    for (String command : config.getCommands()) {
                        String formattedCommand = PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(event.getPlayerUUID()), command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                        MessageUtil.debug("Ran milestone command: " + formattedCommand);
                    }

                    // Remove from in-memory cache
                    List<MilestoneConfig> cachedMilestones = milestoneService.playerCurrentMilestones.get(event.getPlayerUUID());
                    if (cachedMilestones != null) {
                        cachedMilestones.removeIf(m -> m.type == type && m.amount == config.amount);
                    }

                    MessageUtil.debug("Player " + player.getName() + " completed milestone: " + type.name() + " " + config.amount);

                    // Fire player milestone event
                    SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> {
                        SPPlugin.getInstance().getServer().getPluginManager().callEvent(new PlayerMilestoneEvent(event.getPlayerUUID()));
                    });
                }
            }
        }
    }

    @EventHandler
    public void updatePersonalMilestoneBossbar(PaymentSuccessEvent event) {
        MilestoneService service = SPPlugin.getService(MilestoneService.class);
        PlayerService playerService = SPPlugin.getService(DatabaseService.class).getPlayerService();
        PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();
        SPPlayer player = playerService.findByUuid(event.getPlayerUUID());
        Iterator<ObjectObjectMutablePair<MilestoneConfig, BossBar>> iter = service.playerBossBars.get(event.getPlayerUUID()).iterator();
        while (iter.hasNext()) {
            ObjectObjectMutablePair<MilestoneConfig, BossBar> pair = iter.next();
            if (pair == null) {
                continue;
            }
            double playerNewBal = switch (pair.left().type) {
                case MilestoneType.ALL -> paymentLogService.getPlayerTotalAmount(player);
                case MilestoneType.DAILY -> paymentLogService.getPlayerDailyAmount(player);
                case MilestoneType.WEEKLY -> paymentLogService.getPlayerWeeklyAmount(player);
                case MilestoneType.MONTHLY -> paymentLogService.getPlayerMonthlyAmount(player);
                case MilestoneType.YEARLY -> paymentLogService.getPlayerYearlyAmount(player);
                default -> throw new IllegalStateException("Unexpected value: " + pair);
            };
            BossBar bar = pair.right();
            double milestone = pair.left().amount;
            double newProgress = (playerNewBal) / milestone;

            if (newProgress >= 1) {
                // Milestone complete
                iter.remove();
                SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(() -> {
                    bar.removeViewer(Bukkit.getPlayer(event.getPlayerUUID()));
                    Bukkit.getPluginManager().callEvent(new PlayerMilestoneEvent(event.getPlayerUUID()));
                }, 1);

            } else {
                SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(() -> {
                    bar.progress((float) newProgress);
                }, 1);
            }
        }

    }


    /**
     * Phase 3.1: Redesigned server milestone detection with persistence and retroactive completion.
     * <p>
     * Same improvements as player milestones - retroactive, persistent, synchronous.
     */
    @EventHandler
    public void giveServerMilestoneReward(PaymentSuccessEvent event) {
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
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        MessageUtil.debug("Ran server milestone command: " + command);
                    }, 1, 20);

                    // Remove from in-memory cache
                    milestoneService.serverCurrentMilestones.removeIf(m -> m.type == type && m.amount == config.amount);

                    MessageUtil.debug("Server completed milestone: " + type.name() + " " + config.amount);

                    // Fire server milestone event
                    SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> {
                        SPPlugin.getInstance().getServer().getPluginManager().callEvent(new ServerMilestoneEvent(config));
                    });
                }
            }
        }
    }

    @EventHandler
    public void updateServerMilestoneBossbar(PaymentSuccessEvent event) {
        MilestoneService service = SPPlugin.getService(MilestoneService.class);
        PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();
        Iterator<ObjectObjectMutablePair<MilestoneConfig, BossBar>> iter = service.serverBossbars.iterator();
        while (iter.hasNext()) {
            ObjectObjectMutablePair<MilestoneConfig, BossBar> pair = iter.next();
            if (pair == null) {
                continue;
            }
            double serverNewBal = switch (pair.left().type) {
                case ALL -> paymentLogService.getEntireServerAmount();
                case DAILY -> paymentLogService.getEntireServerDailyAmount();
                case WEEKLY -> paymentLogService.getEntireServerWeeklyAmount();
                case MONTHLY -> paymentLogService.getEntireServerMonthlyAmount();
                case YEARLY -> paymentLogService.getEntireServerYearlyAmount();
                default -> throw new IllegalStateException("Unexpected value: " + pair);
            };
            BossBar bar = pair.right();
            double milestone = pair.left().amount;
            double newProgress = (serverNewBal) / milestone;

            if (newProgress >= 1) {
                // Milestone complete
                iter.remove();
                SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        bar.removeViewer(player);
                    }
                });
            } else {
                SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> {
                    bar.progress((float) newProgress);
                });
            }
        }
    }

}
