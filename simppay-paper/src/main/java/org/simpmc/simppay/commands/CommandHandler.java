package org.simpmc.simppay.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.commands.root.*;
import org.simpmc.simppay.commands.root.admin.ManualChargeCommand;
import org.simpmc.simppay.commands.root.admin.SimpPayAdminCommand;

public class CommandHandler {
    private final SPPlugin plugin;
    public boolean enabled;

    public CommandHandler(SPPlugin plugin) {
        this.plugin = plugin;
    }

    public void onLoad() {
        // TODO: CommandAPI v11 configuration - needs investigation for proper paper-shade usage
        // For now using default initialization
        CommandAPI.onLoad(new CommandAPIPaperConfig(plugin).silentLogs(true));
    }

    public void onEnable() {
        enabled = true;
        CommandAPI.onEnable();
        new ManualChargeCommand();
        new SimpPayAdminCommand();
        new BankingCommand();
        new NaptheNhanhCommand();
        new NaptheCommand();
        new ViewHistoryCommand();
        new StreakCommand();  // Phase 5
        new ToggleBossBarCommand();  // BossBar toggle
    }

    public void onDisable() {
        enabled = false;
        CommandAPI.onDisable();
    }

}
