package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.data.milestone.MilestoneType;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.MilestoneService;
import org.simpmc.simppay.util.MessageUtil;

import java.util.List;
import java.util.UUID;

/**
 * Main /milestone command with subcommands for progress, list, history, and leaderboard
 */
public class MilestoneCommand {
    public MilestoneCommand() {
        new CommandAPICommand("milestone")
                .withPermission(CommandPermission.NONE)
                .withAliases("moc", "mocnap", "milestone")
                .withSubcommands(
                        createProgressSubcommand(),
                        createListSubcommand(),
                        createHistorySubcommand(),
                        createLeaderboardSubcommand()
                )
                .register();
    }

    /**
     * /milestone progress - View current milestone progress
     */
    private CommandAPICommand createProgressSubcommand() {
        return new CommandAPICommand("progress")
                .executesPlayer((player, args) -> {
                    try {
                        MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
                        if (milestoneService == null) {
                            MessageUtil.sendMessage(player, "§cMilestone system is not ready");
                            return;
                        }

                        DatabaseService databaseService = SPPlugin.getService(DatabaseService.class);
                        long playerAmount = databaseService.getPaymentLogService().getPlayerTotalAmount(
                                databaseService.getPlayerService().findByUuid(player.getUniqueId())
                        ).longValue();

                        List<MilestoneConfig> activeMilestones = milestoneService.getPlayerMilestones(player.getUniqueId());

                        if (activeMilestones == null || activeMilestones.isEmpty()) {
                            MessageUtil.sendMessage(player, "§7You have no active milestones!");
                            return;
                        }

                        MessageUtil.sendMessage(player, "§6=== Your Milestone Progress ===");
                        MessageUtil.sendMessage(player, "§7Current Total: §a" + String.format("%,d", playerAmount));
                        MessageUtil.sendMessage(player, "");

                        for (MilestoneConfig milestone : activeMilestones) {
                            long remaining = Math.max(0, milestone.getAmount() - playerAmount);
                            double progress = Math.min(1.0, (double) playerAmount / milestone.getAmount());
                            int progressBar = (int) (progress * 20);

                            String displayName = milestone.getDisplayName();
                            String bar = "§2" + "█".repeat(progressBar) + "§7" + "░".repeat(20 - progressBar);

                            MessageUtil.sendMessage(player, "§f" + displayName);
                            MessageUtil.sendMessage(player, bar + " §a" + String.format("%.1f%%", progress * 100));
                            MessageUtil.sendMessage(player, "§7Remaining: §c" + String.format("%,d", remaining));
                            MessageUtil.sendMessage(player, "");
                        }
                    } catch (Exception e) {
                        MessageUtil.sendMessage(player, "§cError loading milestone progress: " + e.getMessage());
                    }
                });
    }

    /**
     * /milestone list - List all milestones
     */
    private CommandAPICommand createListSubcommand() {
        return new CommandAPICommand("list")
                .withOptionalArguments(new StringArgument("type"))
                .executesPlayer((player, args) -> {
                    try {
                        MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
                        if (milestoneService == null) {
                            MessageUtil.sendMessage(player, "§cMilestone system is not ready");
                            return;
                        }

                        String typeFilter = args.getOptional("type").isPresent() ?
                                (String) args.getOptional("type").get() : null;

                        List<MilestoneConfig> allMilestones = milestoneService.getPlayerMilestones(player.getUniqueId());
                        if (allMilestones == null || allMilestones.isEmpty()) {
                            MessageUtil.sendMessage(player, "§7No milestones available");
                            return;
                        }

                        MessageUtil.sendMessage(player, "§6=== Available Milestones ===");

                        for (MilestoneConfig milestone : allMilestones) {
                            if (typeFilter != null && !milestone.getType().toString().equalsIgnoreCase(typeFilter)) {
                                continue;
                            }
                            MessageUtil.sendMessage(player,
                                    "§f" + milestone.getDisplayName() + " §7[" + milestone.getType() + "] §e" +
                                    String.format("%,d", milestone.getAmount()));
                        }
                    } catch (Exception e) {
                        MessageUtil.sendMessage(player, "§cError loading milestones: " + e.getMessage());
                    }
                });
    }

    /**
     * /milestone history - View completed milestones
     */
    private CommandAPICommand createHistorySubcommand() {
        return new CommandAPICommand("history")
                .withOptionalArguments(new IntegerArgument("page", 1, 100))
                .executesPlayer((player, args) -> {
                    try {
                        int page = args.getOptional("page").isPresent() ?
                                (Integer) args.getOptional("page").get() : 1;

                        DatabaseService databaseService = SPPlugin.getService(DatabaseService.class);
                        UUID playerUUID = player.getUniqueId();

                        // Query milestone completions from database
                        // For now, show a placeholder message
                        MessageUtil.sendMessage(player, "§6=== Your Milestone History ===");
                        MessageUtil.sendMessage(player, "§7Page: " + page);
                        MessageUtil.sendMessage(player, "");
                        MessageUtil.sendMessage(player, "§cFeature coming soon!");
                        MessageUtil.sendMessage(player, "§7Track your completed milestones here");
                    } catch (Exception e) {
                        MessageUtil.sendMessage(player, "§cError loading history: " + e.getMessage());
                    }
                });
    }

    /**
     * /milestone leaderboard - View top milestone achievers
     */
    private CommandAPICommand createLeaderboardSubcommand() {
        return new CommandAPICommand("leaderboard")
                .withOptionalArguments(new StringArgument("type"))
                .executesPlayer((player, args) -> {
                    try {
                        String typeFilter = args.getOptional("type").isPresent() ?
                                (String) args.getOptional("type").get() : "ALL";

                        MessageUtil.sendMessage(player, "§6=== Milestone Leaderboard ===");
                        MessageUtil.sendMessage(player, "§7Type: §e" + typeFilter);
                        MessageUtil.sendMessage(player, "");
                        MessageUtil.sendMessage(player, "§cFeature coming soon!");
                        MessageUtil.sendMessage(player, "§7See top milestone achievers on the server");
                    } catch (Exception e) {
                        MessageUtil.sendMessage(player, "§cError loading leaderboard: " + e.getMessage());
                    }
                });
    }
}
