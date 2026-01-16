package org.simpmc.simppay.commands.sub.admin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.detail.BankingDetail;

import java.util.UUID;

public class RechargeBankCommand {
    public static CommandAPICommand commandCreate() {
        return new CommandAPICommand("rechargebank")
                .withPermission("simppay.admin.rechargebank")
                .withArguments(
                        new LongArgument("amount")
                )
                .executesPlayer(RechargeBankCommand::execute);
    }

    public static void execute(Player player, CommandArguments args) {
        SPPlugin plugin = SPPlugin.getInstance();

        long amount = (long) args.get("amount");
        Payment payment = new Payment(
                UUID.randomUUID(),
                player.getUniqueId(),
                new BankingDetail(
                        amount,
                        "1234567890123456",
                        "1234",
                        "1234567890"
                )
        );
        Bukkit.getPluginManager().callEvent(new PaymentSuccessEvent(payment));
    }
}
