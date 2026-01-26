package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.service.MilestoneService;
import org.simpmc.simppay.util.MessageUtil;

/**
 * Command to toggle milestone bossbar visibility.
 */
public class ToggleBossBarCommand {
    public ToggleBossBarCommand() {
        new CommandAPICommand("togglebossbar")
                .withPermission(CommandPermission.NONE)
                .withAliases("togglebar", "hiddenbossbar", "showbossbar")
                .executesPlayer((player, args) -> {
                    MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
                    boolean isNowVisible = milestoneService.toggleBossBarVisibility(player.getUniqueId());

                    MessageConfig messages = ConfigManager.getInstance().getConfig(MessageConfig.class);
                    if (isNowVisible) {
                        MessageUtil.sendMessage(player, messages.bossbarShown);
                    } else {
                        MessageUtil.sendMessage(player, messages.bossbarHidden);
                    }
                })
                .register();
    }
}
