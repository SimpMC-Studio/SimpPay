package org.simpmc.simppay.commands.sub.banking;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.listener.internal.player.BankPromptListener;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.MessageUtil;

public class CancelCommand {
    public static CommandAPICommand commandCreate() {
        return new CommandAPICommand("cancel")
                .executesPlayer(CancelCommand::execute);
    }

    public static void execute(Player player, CommandArguments args) {
        MessageConfig messageConfig = ConfigManager.getInstance().getConfig(MessageConfig.class);

        if (!SPPlugin.getService(PaymentService.class).getPlayerBankingSessionPayment().containsKey(player.getUniqueId())) {
            MessageUtil.sendMessage(player, messageConfig.noExistBankingSession);
        } else {
            MessageUtil.sendMessage(player, messageConfig.cancelBanking);
            SPPlugin.getService(PaymentService.class).cancelBankPayment(player.getUniqueId());
            BankPromptListener.removeItemFrame(player.getUniqueId());
            player.updateInventory(); // restore hand slot after fake QR map
        }
    }
}
