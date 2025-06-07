package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.menu.PaymentHistoryView;

public class ViewHistoryCommand {
    public ViewHistoryCommand() {
        new CommandAPICommand("lichsunapthe")
                .withPermission("simppay.lichsunapthe")
                .withAliases("napthehistory", "xemgdnapthe")
                .executesPlayer((player, args) -> {
                    boolean isFloodgateUUID = player.getUniqueId().getMostSignificantBits() == 0;
                    boolean floodgateEnabled = SPPlugin.getInstance().isFloodgateEnabled();
                    if (floodgateEnabled && isFloodgateUUID) {
                        try {
                            Class<?> viewHistoryFormClass = Class.forName("org.simpmc.simppay.forms.ViewHistoryForm");
                            Object form = viewHistoryFormClass.getMethod("getHistoryForm", Player.class).invoke(null, player);

                            Class<?> floodgateUtilClass = Class.forName("org.simpmc.simppay.util.FloodgateUtil");
                            floodgateUtilClass.getMethod("sendForm", java.util.UUID.class, Object.class)
                                    .invoke(null, player.getUniqueId(), form);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    SPPlugin.getInstance().getViewFrame().open(PaymentHistoryView.class, player);
                })
                .register();
    }
}
