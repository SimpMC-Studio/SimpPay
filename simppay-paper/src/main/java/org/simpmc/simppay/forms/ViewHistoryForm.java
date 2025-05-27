package org.simpmc.simppay.forms;

import com.google.common.base.Preconditions;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.database.dto.PaymentRecord;
import org.simpmc.simppay.database.entities.SPPlayer;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public class ViewHistoryForm {
    public static SimpleForm getHistoryForm(Player player) {
        List<PaymentRecord> paymentRecords = fetchPaymentRecordsAsync(player);
        SimpleForm.Builder simpleForm = SimpleForm.builder();
        for (PaymentRecord paymentRecord : paymentRecords) {
            String message = String.format(ChatColor.DARK_GREEN + "Số tiền: %s",
                    String.format("%,.0f", paymentRecord.getAmount()) + "đ");
            simpleForm.button(message); // TODO: make form configurable
        }
        return simpleForm.build();
    }

    private static List<PaymentRecord> fetchPaymentRecordsAsync(Player player) {
            SPPlayer spPlayer;
            spPlayer = SPPlugin.getInstance().getDatabaseService().getPlayerService().findByUuid(player.getUniqueId());
            Preconditions.checkNotNull(spPlayer, "Player not found");
            List<PaymentRecord> paymentRecords = SPPlugin.getInstance().getDatabaseService().getPaymentLogService().getPaymentsByPlayer(spPlayer);
            return paymentRecords;
    }
}

