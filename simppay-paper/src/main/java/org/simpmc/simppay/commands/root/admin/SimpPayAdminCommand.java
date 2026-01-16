package org.simpmc.simppay.commands.root.admin;

import dev.jorel.commandapi.CommandAPICommand;
import org.simpmc.simppay.commands.sub.admin.*;

public class SimpPayAdminCommand {
    public SimpPayAdminCommand() {
        new CommandAPICommand("simppayadmin")
                .withPermission("simppay.admin")
                .withSubcommands(
                        ReloadCommand.commandCreate(),
                        ViewHistoryCommand.commandCreate(),
                        RechargeBankCommand.commandCreate(),
                        DebugCardCommand.commandCreate(),
                        DeletePlayerCommand.commandCreate(),
                        ReloadServerMilestoneCommand.commandCreate(),
                        ReloadPlayerMilestoneCommand.commandCreate()
                )
                .register();
    }
}
