package org.simpmc.simppay.commands.sub.admin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.convert.ConverterRegistry;
import org.simpmc.simppay.convert.ImportResult;
import org.simpmc.simppay.convert.PluginConverter;
import org.simpmc.simppay.util.MessageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImportCommand {

    public static CommandAPICommand commandCreate() {
        List<CommandAPICommand> converterCommands = new ArrayList<>();
        for (ConverterRegistry registry : ConverterRegistry.values()) {
            PluginConverter probe = registry.factory.get();
            List<CommandAPICommand> subCommands = new ArrayList<>();
            if (probe.supportsMySQL()) subCommands.add(mysqlSubCommand(registry));
            if (probe.supportsFlatfile()) subCommands.add(flatfileSubCommand(registry));
            if (probe.supportsCredentials()) subCommands.add(credentialsSubCommand(registry));
            if (!subCommands.isEmpty()) {
                converterCommands.add(new CommandAPICommand(registry.commandName)
                        .withSubcommands(subCommands.toArray(new CommandAPICommand[0])));
            }
        }
        return new CommandAPICommand("import")
                .withPermission("simppay.admin.import")
                .withSubcommands(converterCommands.toArray(new CommandAPICommand[0]));
    }

    private static CommandAPICommand mysqlSubCommand(ConverterRegistry registry) {
        return new CommandAPICommand("mysql")
                .withArguments(
                        new StringArgument("host"),
                        new IntegerArgument("port", 1, 65535),
                        new StringArgument("database"),
                        new StringArgument("username"),
                        new GreedyStringArgument("password")
                )
                .executes((CommandExecutor) (sender, args) -> executeMysql(sender, args, registry));
    }

    private static CommandAPICommand flatfileSubCommand(ConverterRegistry registry) {
        return new CommandAPICommand("flatfile")
                .withArguments(new GreedyStringArgument("filepath"))
                .executes((CommandExecutor) (sender, args) -> executeFlatfile(sender, args, registry));
    }

    private static CommandAPICommand credentialsSubCommand(ConverterRegistry registry) {
        return new CommandAPICommand("credentials")
                .executes((CommandExecutor) (sender, args) -> executeCredentials(sender, args, registry));
    }

    private static void executeMysql(CommandSender sender, CommandArguments args, ConverterRegistry registry) {
        String host = (String) args.get("host");
        int port = (int) args.get("port");
        String database = (String) args.get("database");
        String username = (String) args.get("username");
        String password = (String) args.get("password");

        MessageConfig messages = ConfigManager.getInstance().getConfig(MessageConfig.class);
        MessageUtil.sendMessage(sender, messages.importStarting);

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            PluginConverter converter = registry.factory.get();
            ImportResult result = converter.importFromMySQL(host, port, database, username, password);
            sendImportResult(sender, result, messages);
        });
    }

    private static void executeFlatfile(CommandSender sender, CommandArguments args, ConverterRegistry registry) {
        String filepath = (String) args.get("filepath");
        MessageConfig messages = ConfigManager.getInstance().getConfig(MessageConfig.class);
        MessageUtil.sendMessage(sender, messages.importStarting);

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            PluginConverter converter = registry.factory.get();
            ImportResult result = converter.importFromFlatfile(new File(filepath));
            sendImportResult(sender, result, messages);
        });
    }

    private static void executeCredentials(CommandSender sender, CommandArguments args, ConverterRegistry registry) {
        MessageConfig messages = ConfigManager.getInstance().getConfig(MessageConfig.class);
        MessageUtil.sendMessage(sender, messages.importStarting);

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            PluginConverter converter = registry.factory.get();
            ImportResult result = converter.importCredentials();
            sendImportResult(sender, result, messages);
        });
    }

    private static void sendImportResult(CommandSender sender, ImportResult result, MessageConfig messages) {
        String summary = messages.importComplete
                .replace("{imported}", String.valueOf(result.getImported()))
                .replace("{skipped}", String.valueOf(result.getSkipped()))
                .replace("{failed}", String.valueOf(result.getFailed()));
        MessageUtil.sendMessage(sender, summary);

        if (!result.getErrors().isEmpty()) {
            int maxDisplay = Math.min(result.getErrors().size(), 10);
            for (int i = 0; i < maxDisplay; i++) {
                MessageUtil.sendMessage(sender, messages.importError.replace("{error}", result.getErrors().get(i)));
            }
            if (result.getErrors().size() > 10) {
                MessageUtil.sendMessage(sender, messages.importMoreErrors
                        .replace("{count}", String.valueOf(result.getErrors().size() - 10)));
            }
        }
    }
}
