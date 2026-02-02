package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.menu.card.CardListView;
import org.simpmc.simppay.util.FloodgateUtil;

public class NaptheCommand {

    public NaptheCommand() {
        new CommandAPICommand("napthe")
                .withPermission(CommandPermission.NONE)
                .executesPlayer((player, args) -> {
                    // start a new napthe session
                    // Use FloodgateUtil for accurate Bedrock player detection
                    if (SPPlugin.getInstance().isFloodgateEnabled() && FloodgateUtil.isBedrockPlayer(player)) {
                        try {
                            Class<?> naptheFormClass = Class.forName("org.simpmc.simppay.forms.NaptheForm");
                            Object form = naptheFormClass.getMethod("getNapTheForm", org.bukkit.entity.Player.class)
                                    .invoke(null, player);

                            Class<?> floodgateUtilClass = Class.forName("org.simpmc.simppay.util.FloodgateUtil");
                            floodgateUtilClass.getMethod("sendForm", java.util.UUID.class, Object.class)
                                    .invoke(null, player.getUniqueId(), form);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return;
                    }
                    CardListView.openMenu(player);
                })
                .register();
    }
}
