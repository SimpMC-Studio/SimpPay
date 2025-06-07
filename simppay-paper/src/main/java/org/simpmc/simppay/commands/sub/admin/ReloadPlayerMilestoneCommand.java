package org.simpmc.simppay.commands.sub.admin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.util.MessageUtil;

public class ReloadPlayerMilestoneCommand {
    public static CommandAPICommand commandCreate() {
        return new CommandAPICommand("reloadplayermilestone")
                .withPermission("simppay.admin.reloadplayermilestone")
                .withArguments(
                        new StringArgument("player")
                )
                .executes(ReloadPlayerMilestoneCommand::execute);
    }

    public static void execute(CommandSender player, CommandArguments args) {
        SPPlugin plugin = SPPlugin.getInstance();

        String playerTarget = (String) args.get("player");
        SPPlayer spPlayer = plugin.getDatabaseService().getPlayerService().findByName(playerTarget);
        if (spPlayer == null) {
            MessageUtil.sendMessage(player, "Player not found");
            return;
        }
        plugin.getMilestoneService().loadPlayerMilestone(spPlayer.getUuid());
    }
}
