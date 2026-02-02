package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.menu.PaymentHistoryView;
import org.simpmc.simppay.util.FloodgateUtil;

public class ViewHistoryCommand {
    public ViewHistoryCommand() {
        new CommandAPICommand("lichsunapthe")
                .withPermission(CommandPermission.NONE)
                .withAliases("napthehistory", "xemgdnapthe")
                .executesPlayer((player, args) -> {
                    // Use FloodgateUtil for accurate Bedrock player detection
                    if (SPPlugin.getInstance().isFloodgateEnabled() && FloodgateUtil.isBedrockPlayer(player)) {
                        try {
                            Class<?> viewHistoryFormClass = Class.forName("org.simpmc.simppay.forms.ViewHistoryForm");
                            Object form = viewHistoryFormClass.getMethod("getHistoryForm", Player.class).invoke(null,
                                    player);

                            Class<?> floodgateUtilClass = Class.forName("org.simpmc.simppay.util.FloodgateUtil");
                            floodgateUtilClass.getMethod("sendForm", java.util.UUID.class, Object.class)
                                    .invoke(null, player.getUniqueId(), form);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    PaymentHistoryView.openMenu(player, null);
                })
                .register();
    }
}
