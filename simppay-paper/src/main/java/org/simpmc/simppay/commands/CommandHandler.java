package org.simpmc.simppay.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.commands.root.BankingCommand;
import org.simpmc.simppay.commands.root.MilestoneCommand;
import org.simpmc.simppay.commands.root.NaptheCommand;
import org.simpmc.simppay.commands.root.NaptheNhanhCommand;
import org.simpmc.simppay.commands.root.ViewHistoryCommand;
import org.simpmc.simppay.commands.root.admin.ManualChargeCommand;
import org.simpmc.simppay.commands.root.admin.MigrationCommand;
import org.simpmc.simppay.commands.root.admin.SimpPayAdminCommand;

public class CommandHandler {
    private final SPPlugin plugin;

    public CommandHandler(SPPlugin plugin) {
        this.plugin = plugin;
    }
    public boolean enabled;

    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(plugin).shouldHookPaperReload(true).silentLogs(true));
    }

    public void onEnable() {
        enabled = true;
        CommandAPI.onEnable();
        new ManualChargeCommand();
        new SimpPayAdminCommand();
        new MigrationCommand();
        new BankingCommand();
        new NaptheNhanhCommand();
        new NaptheCommand();
        new ViewHistoryCommand();
        new MilestoneCommand();
    }

    public void onDisable() {
        enabled = false;
        CommandAPI.onDisable();
    }

}
