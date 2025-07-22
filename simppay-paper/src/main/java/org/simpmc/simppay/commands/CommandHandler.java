package org.simpmc.simppay.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.commands.root.BankingCommand;
import org.simpmc.simppay.commands.root.NaptheCommand;
import org.simpmc.simppay.commands.root.NaptheNhanhCommand;
import org.simpmc.simppay.commands.root.ViewHistoryCommand;
import org.simpmc.simppay.commands.root.admin.ManualChargeCommand;
import org.simpmc.simppay.commands.root.admin.SimpPayAdminCommand;

public class CommandHandler {
    private final SPPlugin plugin;

    public CommandHandler(SPPlugin plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(plugin).shouldHookPaperReload(true).silentLogs(true));
    }

    public void onEnable() {
        CommandAPI.onEnable();
        new ManualChargeCommand();
        new SimpPayAdminCommand();
        new BankingCommand();
        new NaptheNhanhCommand();
        new NaptheCommand();
        new ViewHistoryCommand();
    }

    public void onDisable() {
        CommandAPI.onDisable();
    }

}
