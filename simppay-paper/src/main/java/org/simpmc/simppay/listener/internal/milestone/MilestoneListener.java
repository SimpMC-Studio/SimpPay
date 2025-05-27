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
import org.simpmc.simppay.service.MilestoneService;
import org.simpmc.simppay.service.database.PaymentLogService;
import org.simpmc.simppay.service.database.PlayerService;
import org.simpmc.simppay.util.MessageUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MilestoneListener implements Listener {
    public MilestoneListener(SPPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(() -> {
            UUID uuid = event.getPlayer().getUniqueId();
            MilestoneService service = SPPlugin.getInstance().getMilestoneService();
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
        MilestoneService service = SPPlugin.getInstance().getMilestoneService();
        service.playerBossBars.remove(event.getPlayer().getUniqueId());
        service.playerCurrentMilestones.remove(event.getPlayer().getUniqueId());
        MessageUtil.debug("Cleared cache bossbar and currentmilestones " + event.getPlayer().getName());
    }

    @EventHandler
    public void givePersonalMilestoneReward(PaymentSuccessEvent event) {
        MilestoneService milestoneService = SPPlugin.getInstance().getMilestoneService();
        PlayerService playerService = SPPlugin.getInstance().getDatabaseService().getPlayerService();
        PaymentLogService paymentLogService = SPPlugin.getInstance().getDatabaseService().getPaymentLogService();
        SPPlayer player = playerService.findByUuid(event.getPlayerUUID());
        double charged = event.getAmount();

        List<MilestoneConfig> list = milestoneService.playerCurrentMilestones.getOrDefault(event.getPlayerUUID(), new ArrayList<>());
        Iterator<MilestoneConfig> iter = list.iterator();

        while (iter.hasNext()) {
            MilestoneConfig config = iter.next();

            double playerNewBal = switch (config.getType()) {
                case ALL -> paymentLogService.getPlayerTotalAmount(player);
                case DAILY -> paymentLogService.getPlayerDailyAmount(player);
                case WEEKLY -> paymentLogService.getPlayerWeeklyAmount(player);
                case MONTHLY -> paymentLogService.getPlayerMonthlyAmount(player);
                case YEARLY -> paymentLogService.getPlayerYearlyAmount(player);
                default -> throw new IllegalStateException("Unexpected value: " + config.getType());
            };
            /*
             * @param playerNewBal số tiền sau khi nạp
             * @param charged số tiền nạp
             */
            if (playerNewBal >= config.amount && playerNewBal - charged < config.amount) {
                // Milestone complete
                for (String command : config.getCommands()) {
                    SPPlugin.getInstance().getFoliaLib().getScheduler().runLater(task2 -> {
                        String formattedCommand = PlaceholderAPI.setPlaceholders(Bukkit.getPlayer(event.getPlayerUUID()), command);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                        MessageUtil.debug("Ran " + formattedCommand);
                    }, 1);
                }
                // reset player current milestone
                iter.remove();
                MessageUtil.debug("Player " + player.getName() + " completed milestone " + config.amount);
                MessageUtil.debug("Player " + player.getName() + " remaining milestone " + milestoneService.playerCurrentMilestones.get(event.getPlayerUUID()).size());
                MessageUtil.debug("Player " + player.getName() + " removed " + config.toString());
                // Call event for player milestone
                SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> {
                    SPPlugin.getInstance().getServer().getPluginManager().callEvent(new PlayerMilestoneEvent(event.getPlayerUUID()));
                });
            }
        }

    }

    @EventHandler
    public void updatePersonalMilestoneBossbar(PaymentSuccessEvent event) {
        MilestoneService service = SPPlugin.getInstance().getMilestoneService();
        PlayerService playerService = SPPlugin.getInstance().getDatabaseService().getPlayerService();
        PaymentLogService paymentLogService = SPPlugin.getInstance().getDatabaseService().getPaymentLogService();
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


    @EventHandler
    public void giveServerMilestoneReward(PaymentSuccessEvent event) {
        MilestoneService milestoneService = SPPlugin.getInstance().getMilestoneService();
        PaymentLogService paymentLogService = SPPlugin.getInstance().getDatabaseService().getPaymentLogService();
        double charged = event.getAmount();
        List<MilestoneConfig> list = milestoneService.serverCurrentMilestones;
        if (list == null) {
            return;
        }
        Iterator<MilestoneConfig> iter = list.iterator();
        while (iter.hasNext()) {
            MilestoneConfig config = iter.next();
            double serverNewBal = switch (config.getType()) {
                case ALL -> paymentLogService.getEntireServerAmount();
                case DAILY -> paymentLogService.getEntireServerDailyAmount();
                case WEEKLY -> paymentLogService.getEntireServerWeeklyAmount();
                case MONTHLY -> paymentLogService.getEntireServerMonthlyAmount();
                case YEARLY -> paymentLogService.getEntireServerYearlyAmount();
                default -> throw new IllegalStateException("Unexpected value: " + config.getType());
            };
            /*
             * @param playerNewBal số tiền sau khi nạp
             * @param charged số tiền nạp
             */
            if (serverNewBal >= config.amount && serverNewBal - charged < config.amount) {
                // Milestone complete
                Deque<String> commands = new ArrayDeque<>();
                for (String command : config.getCommands()) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String formattedCommand = PlaceholderAPI.setPlaceholders(player, command);
                        commands.add(formattedCommand);
                    }
                }

                // queue commands to console using timer
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
                    MessageUtil.debug("Ran " + command);

                }, 1, 20);

                // reset player current milestone
                iter.remove();
                MessageUtil.debug("Server completed milestone " + config.amount);
                MessageUtil.debug("Server remaining milestone " + milestoneService.playerCurrentMilestones.get(event.getPlayerUUID()).size());
                MessageUtil.debug("Server removed " + config.toString());
                // Call event for player milestone
                SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> {
                    SPPlugin.getInstance().getServer().getPluginManager().callEvent(new ServerMilestoneEvent(config));
                });
            }
        }


    }

    @EventHandler
    public void updateServerMilestoneBossbar(PaymentSuccessEvent event) {
        MilestoneService service = SPPlugin.getInstance().getMilestoneService();
        PaymentLogService paymentLogService = SPPlugin.getInstance().getDatabaseService().getPaymentLogService();
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
