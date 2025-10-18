package org.simpmc.simppay.service.milestone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.database.entities.MilestoneCompletion;
import org.simpmc.simppay.database.entities.ServerMilestoneCompletion;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.util.MessageUtil;

import java.sql.SQLException;
import java.util.*;

/**
 * Handles execution of milestone reward commands with proper error handling and logging.
 */
@Slf4j
@RequiredArgsConstructor
public class MilestoneRewardExecutor {

    private static final Gson gson = new GsonBuilder().create();
    private final DatabaseService databaseService;
    private MilestoneNotificationService notificationService;

    /**
     * Initialize the notification service
     */
    public void setNotificationService(MilestoneNotificationService service) {
        this.notificationService = service;
    }

    /**
     * Execute reward commands for a player milestone (async via FoliaLib)
     */
    public void executePlayerMilestoneRewards(Player player, MilestoneConfig milestoneConfig) {
        // Schedule async execution using FoliaLib for cross-server compatibility
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                List<String> executedCommands = new ArrayList<>();

                for (String command : milestoneConfig.getCommands()) {
                    try {
                        String processedCommand = PlaceholderAPI.setPlaceholders(player, command);
                        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);

                        executedCommands.add(command);

                        if (success) {
                            MessageUtil.debug("Executed player milestone command: " + processedCommand);
                        } else {
                            MessageUtil.warn("Failed to execute player milestone command: " + processedCommand);
                            log.warn("Milestone command returned false: {} for player {}", processedCommand, player.getName());
                        }
                    } catch (Exception e) {
                        MessageUtil.warn("Error executing milestone command for " + player.getName() + ": " + e.getMessage());
                        log.error("Error executing milestone command for player {}: {}", player.getName(), command, e);
                    }
                }

                // Persist to database
                persistPlayerMilestoneCompletion(player.getUniqueId(), milestoneConfig, executedCommands);

                // Send notification
                if (notificationService != null) {
                    notificationService.notifyPlayerMilestoneComplete(player, milestoneConfig);
                }
            } catch (Exception e) {
                MessageUtil.warn("Unexpected error in milestone reward execution: " + e.getMessage());
                log.error("Unexpected error in milestone reward execution", e);
            }
        });
    }

    /**
     * Execute reward commands for a server milestone (broadcasts to all players)
     */
    public void executeServerMilestoneRewards(MilestoneConfig milestoneConfig, long delayBetweenCommandsTicks) {
        // Schedule async execution using FoliaLib
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            try {
                List<String> executedCommands = new ArrayList<>();
                Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

                int commandIndex = 0;
                for (String command : milestoneConfig.getCommands()) {
                    // Schedule each command with a delay using Bukkit scheduler
                    final int index = commandIndex;
                    final String cmd = command;

                    Bukkit.getScheduler().runTaskLaterAsynchronously(
                            SPPlugin.getInstance(),
                            () -> {
                                try {
                                    // For each online player, parse placeholders
                                    for (Player player : onlinePlayers) {
                                        try {
                                            String processedCommand = PlaceholderAPI.setPlaceholders(player, cmd);
                                            boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);

                                            if (!success) {
                                                log.warn("Server milestone command returned false: {} for player {}", processedCommand, player.getName());
                                            }
                                        } catch (Exception e) {
                                            log.debug("Error processing placeholder for player {}: {}", player.getName(), e.getMessage());
                                        }
                                    }

                                    // Also execute once without player placeholder replacement
                                    boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                                    if (success) {
                                        MessageUtil.debug("Executed server milestone command: " + cmd);
                                    } else {
                                        MessageUtil.warn("Failed to execute server milestone command: " + cmd);
                                        log.warn("Server milestone command returned false: {}", cmd);
                                    }
                                } catch (Exception e) {
                                    MessageUtil.warn("Error executing server milestone command: " + e.getMessage());
                                    log.error("Error executing server milestone command: {}", cmd, e);
                                }
                            },
                            index * delayBetweenCommandsTicks
                    );

                    executedCommands.add(command);
                    commandIndex++;
                }

                // Persist to database (async)
                persistServerMilestoneCompletion(milestoneConfig, executedCommands, onlinePlayers.size());

                // Send notification to all players
                if (notificationService != null) {
                    notificationService.notifyServerMilestoneComplete(milestoneConfig);
                }
            } catch (Exception e) {
                MessageUtil.warn("Unexpected error in server milestone reward execution: " + e.getMessage());
                log.error("Unexpected error in server milestone reward execution", e);
            }
        });
    }

    /**
     * Persist player milestone completion to database
     */
    private void persistPlayerMilestoneCompletion(UUID playerUUID, MilestoneConfig milestoneConfig, List<String> executedCommands) {
        try {
            SPPlayer spPlayer = databaseService.getDatabase().getPlayerDao().queryForId(playerUUID);
            if (spPlayer == null) {
                log.warn("Cannot persist milestone completion: SPPlayer not found for UUID {}", playerUUID);
                return;
            }

            String commandsJson = gson.toJson(executedCommands);
            String milestoneId = UUID.randomUUID().toString(); // Generate unique ID for each completion
            MilestoneCompletion completion = new MilestoneCompletion(
                    spPlayer,
                    milestoneId,
                    milestoneConfig.getType(),
                    milestoneConfig.getAmount(),
                    commandsJson
            );

            databaseService.getDatabase().getMilestoneCompletionDao().create(completion);
            MessageUtil.debug("Persisted milestone completion for player " + playerUUID);
        } catch (SQLException e) {
            log.error("Failed to persist player milestone completion for {}", playerUUID, e);
        }
    }

    /**
     * Persist server milestone completion to database
     */
    private void persistServerMilestoneCompletion(MilestoneConfig milestoneConfig, List<String> executedCommands, int playersOnlineCount) {
        try {
            String commandsJson = gson.toJson(executedCommands);
            String milestoneId = UUID.randomUUID().toString(); // Generate unique ID
            ServerMilestoneCompletion completion = new ServerMilestoneCompletion(
                    milestoneId,
                    milestoneConfig.getType(),
                    milestoneConfig.getAmount(),
                    commandsJson,
                    playersOnlineCount
            );

            databaseService.getDatabase().getServerMilestoneCompletionDao().create(completion);
            MessageUtil.debug("Persisted server milestone completion");
        } catch (SQLException e) {
            log.error("Failed to persist server milestone completion", e);
        }
    }
}
