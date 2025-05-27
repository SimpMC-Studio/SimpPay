package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import org.geysermc.floodgate.api.FloodgateApi;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.forms.NaptheForm;
import org.simpmc.simppay.menu.card.CardListView;
import org.simpmc.simppay.util.FloodgateUtil;

public class NaptheCommand {

    public NaptheCommand() {
        new CommandAPICommand("napthe")
                .withPermission("simppay.napthe")
                .executesPlayer((player, args) -> {
                    // start a new napthe session
                    if (FloodgateUtil.isFloodgateUUID(player.getUniqueId()) && FloodgateUtil.enableFloodgate) {
                        FloodgateUtil.sendForm(player.getUniqueId(), NaptheForm.getNapTheForm(player));
                        return;
                    }
                    SPPlugin.getInstance().getViewFrame().open(CardListView.class, player);
                })
                .register();
    }
}
