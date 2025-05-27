package org.simpmc.simppay.commands.root;

import dev.jorel.commandapi.CommandAPICommand;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.forms.ViewHistoryForm;
import org.simpmc.simppay.menu.PaymentHistoryView;

public class ViewHistoryCommand {
    public ViewHistoryCommand() {
        new CommandAPICommand("lichsunapthe")
                .withPermission("simppay.lichsunapthe")
                .withAliases("napthehistory", "xemgdnapthe")
                .executesPlayer((player, args) -> {
                    if (FloodgateApi.getInstance().isFloodgateId(player.getUniqueId())) {
                        SPPlugin plugin = SPPlugin.getInstance();
                        plugin.getFoliaLib().getScheduler().runAsync(task -> {
                            SimpleForm form = ViewHistoryForm.getHistoryForm(player);
                            plugin.getFoliaLib().getScheduler().runNextTick(task2 -> {
                                FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
                            });
                        });
                        return;
                    }
                    SPPlugin.getInstance().getViewFrame().open(PaymentHistoryView.class, player);
                })
                .register();
    }
}
