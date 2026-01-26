package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.simpmc.simppay.forms.StreakForm;
import org.simpmc.simppay.menu.StreakMenuView;
import org.simpmc.simppay.util.FloodgateUtil;

/**
 * Phase 5: Command to open the streak menu.
 * Supports both Java Edition (GUI) and Bedrock Edition (Forms).
 */
public class StreakCommand {
    public StreakCommand() {
        new CommandAPICommand("streak")
                .withPermission(CommandPermission.NONE)
                .withAliases("streaks", "naplientiep", "chuoinapthe")
                .executesPlayer((player, args) -> {
                    // Check if player is from Bedrock Edition
                    if (FloodgateUtil.isBedrockPlayer(player)) {
                        // Send Bedrock form
                        FloodgateUtil.sendForm(player.getUniqueId(), StreakForm.getStreakForm(player));
                    } else {
                        // Open Java GUI
                        StreakMenuView.openMenu(player);
                    }
                })
                .register();
    }
}
