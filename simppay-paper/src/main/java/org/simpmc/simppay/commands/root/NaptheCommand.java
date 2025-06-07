package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.menu.card.CardListView;

public class NaptheCommand {

    public NaptheCommand() {
        new CommandAPICommand("napthe")
                .withPermission("simppay.napthe")
                .executesPlayer((player, args) -> {
                    // start a new napthe session
                    boolean isFloodgateUUID = player.getUniqueId().getMostSignificantBits() == 0;
                    boolean floodgateEnabled = SPPlugin.getInstance().isFloodgateEnabled();
                    if (floodgateEnabled && isFloodgateUUID) {
                        try {
                            Class<?> naptheFormClass = Class.forName("org.simpmc.simppay.forms.NaptheForm");
                            Object form = naptheFormClass.getMethod("getNapTheForm", org.bukkit.entity.Player.class).invoke(null, player);

                            Class<?> floodgateUtilClass = Class.forName("org.simpmc.simppay.util.FloodgateUtil");
                            floodgateUtilClass.getMethod("sendForm", java.util.UUID.class, Object.class)
                                    .invoke(null, player.getUniqueId(), form);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    SPPlugin.getInstance().getViewFrame().open(CardListView.class, player);
                })
                .register();
    }
}
