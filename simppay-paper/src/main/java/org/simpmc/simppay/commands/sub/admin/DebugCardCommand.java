package org.simpmc.simppay.commands.sub.admin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.data.card.CardPrice;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.detail.CardDetail;

import java.util.UUID;

public class DebugCardCommand {

    public static CommandAPICommand commandCreate() {
        return new CommandAPICommand("debugcard")
                .withPermission("simppay.admin.debugcard")
                .withArguments(
                        new BooleanArgument("wrongPrice")
                )
                .executesPlayer(DebugCardCommand::execute);
    }

    public static void execute(Player player, CommandArguments args) {
        SPPlugin plugin = SPPlugin.getInstance();

        Payment payment = new Payment(
                UUID.randomUUID(),
                player.getUniqueId(),
                new CardDetail(
                        "123123123",
                        "123123123",
                        CardPrice._10K,
                        CardType.VIETTEL,
                        "123123123",
                        10000
                )
        );
        boolean wrongPrice = (boolean) args.get("wrongPrice");
        Bukkit.getPluginManager().callEvent(new PaymentSuccessEvent(payment, wrongPrice));
    }
}
