package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.simpmc.simppay.menu.StreakMenuView;

/**
 * Phase 5: Command to open the streak menu.
 */
public class StreakCommand {
    public StreakCommand() {
        new CommandAPICommand("streak")
                .withPermission(CommandPermission.NONE)
                .withAliases("streaks", "naplientiep", "lientiep")
                .executesPlayer((player, args) -> {
                    StreakMenuView.openMenu(player);
                })
                .register();
    }
}
