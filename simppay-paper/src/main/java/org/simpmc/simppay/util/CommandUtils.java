package org.simpmc.simppay.util;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.simpmc.simppay.SPPlugin;

public class CommandUtils {
    public static void dispatchCommand(CommandSender sender, String command) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(task -> Bukkit.dispatchCommand(sender, command));
    }
}
