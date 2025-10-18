package org.simpmc.simppay.commands.root.admin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.migration.MigrationResult;
import org.simpmc.simppay.migration.MigrationService;
import org.simpmc.simppay.util.MessageUtil;

import java.io.File;

/**
 * Admin command for importing data from legacy donation plugins
 * Usage:
 * - /simppay migrate thesieutoc [path]
 * - /simppay migrate dotman [path]
 * - /simppay migrate status
 */
public class MigrationCommand {

    public MigrationCommand() {
        register();
    }

    private void register() {
        new CommandAPICommand("simppay")
                .withSubcommand(
                        new CommandAPICommand("migrate")
                                .withPermission("simppay.admin.migrate")
                                .withSubcommand(
                                        new CommandAPICommand("thesieutoc")
                                                .withOptionalArguments(new StringArgument("path"))
                                                .executes(this::migrateTheSieuToc)
                                )
                                .withSubcommand(
                                        new CommandAPICommand("dotman")
                                                .withOptionalArguments(new StringArgument("path"))
                                                .executes(this::migrateDotMan)
                                )
                                .withSubcommand(
                                        new CommandAPICommand("status")
                                                .executes(this::showStatus)
                                )
                )
                .register();
    }

    /**
     * Migrate from TheSieuToc plugin
     * /simppay migrate thesieutoc [path]
     */
    private void migrateTheSieuToc(CommandSender sender, CommandArguments args) {
        SPPlugin plugin = SPPlugin.getInstance();
        String path = (String) args.getOptional("path")
                .orElse("plugins/TheSieuToc");

        // Validate path
        File sourceFolder = new File(path);
        if (!sourceFolder.exists()) {
            MessageUtil.sendMessage(sender, "§cError: Path does not exist: " + path);
            return;
        }

        MessageUtil.sendMessage(sender, "§e§lStarting TheSieuToc migration...");
        MessageUtil.sendMessage(sender, "§7Source: " + sourceFolder.getAbsolutePath());

        // Run migration async
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            try {
                MigrationService migrationService = MigrationService.getInstance();
                MigrationResult result = migrationService.migrateFromTheSieuToc(path);

                // Display results
                sendMigrationResults(sender, result);
            } catch (Exception e) {
                MessageUtil.sendMessage(sender, "§c§lError: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Migrate from DotMan plugin
     * /simppay migrate dotman [path]
     */
    private void migrateDotMan(CommandSender sender, CommandArguments args) {
        SPPlugin plugin = SPPlugin.getInstance();
        String path = (String) args.getOptional("path")
                .orElse("plugins/DotMan");

        // Validate path
        File sourceFolder = new File(path);
        if (!sourceFolder.exists()) {
            MessageUtil.sendMessage(sender, "§cError: Path does not exist: " + path);
            return;
        }

        File configFile = new File(sourceFolder, "config.yml");
        if (!configFile.exists()) {
            MessageUtil.sendMessage(sender, "§cError: config.yml not found in " + path);
            return;
        }

        MessageUtil.sendMessage(sender, "§e§lStarting DotMan migration...");
        MessageUtil.sendMessage(sender, "§7Source: " + sourceFolder.getAbsolutePath());

        // Run migration async
        plugin.getFoliaLib().getScheduler().runAsync(task -> {
            try {
                MigrationService migrationService = MigrationService.getInstance();
                MigrationResult result = migrationService.migrateFromDotMan(path);

                // Display results
                sendMigrationResults(sender, result);
            } catch (Exception e) {
                MessageUtil.sendMessage(sender, "§c§lError: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Show status of last migration
     * /simppay migrate status
     */
    private void showStatus(CommandSender sender, CommandArguments args) {
        MigrationService migrationService = MigrationService.getInstance();
        MigrationResult result = migrationService.getLastMigrationResult();

        if (result == null) {
            MessageUtil.sendMessage(sender, "§eNo migration has been run yet");
            return;
        }

        // Display result summary
        sendMigrationResults(sender, result);
    }

    /**
     * Send migration results to command sender
     */
    private void sendMigrationResults(CommandSender sender, MigrationResult result) {
        // Show summary
        String[] summaryLines = result.getSummary().split("\n");
        for (String line : summaryLines) {
            MessageUtil.sendMessage(sender, line);
        }

        // Show detailed statistics
        MessageUtil.sendMessage(sender, "");
        MessageUtil.sendMessage(sender, "§e§lDetailed Statistics:");
        MessageUtil.sendMessage(sender, "§f• Total records: §a" + result.totalRecords());
        MessageUtil.sendMessage(sender, "§f• Successfully imported: §a" + result.successCount());
        MessageUtil.sendMessage(sender, "§f• Skipped (duplicates): §e" + result.skippedCount());
        MessageUtil.sendMessage(sender, "§f• Errors: §c" + result.errorCount());
        MessageUtil.sendMessage(sender, "§f• Success rate: §a" + String.format("%.1f%%", result.getSuccessRate()));
        MessageUtil.sendMessage(sender, "§f• Duration: §a" + result.durationMs() + "ms");

        // Show errors if any
        if (result.hasErrors()) {
            MessageUtil.sendMessage(sender, "");
            MessageUtil.sendMessage(sender, "§c§lErrors encountered:");
            String errorsSummary = result.getErrorsSummary(10);
            for (String line : errorsSummary.split("\n")) {
                if (!line.trim().isEmpty()) {
                    MessageUtil.sendMessage(sender, line);
                }
            }
        } else {
            MessageUtil.sendMessage(sender, "");
            MessageUtil.sendMessage(sender, "§a✓ No errors");
        }
    }
}
