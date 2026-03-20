package org.simpmc.simppay.forms;

import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.menu.FormsConfig;
import org.simpmc.simppay.database.dto.PaymentRecord;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.service.DatabaseService;

import java.util.List;


public class ViewHistoryForm {
    public static SimpleForm getHistoryForm(Player player) {
        FormsConfig.ViewHistoryFormStrings f = ConfigManager.getInstance().getConfig(FormsConfig.class).viewHistoryForm;
        List<PaymentRecord> paymentRecords = fetchPaymentRecordsAsync(player);
        SimpleForm.Builder simpleForm = SimpleForm.builder().title(f.title);

        if (paymentRecords.isEmpty()) {
            simpleForm.content(f.emptyMessage);
        } else {
            for (PaymentRecord paymentRecord : paymentRecords) {
                String amount = String.format("%,.0f", paymentRecord.getAmount()) + "đ";
                simpleForm.button(String.format(f.amountFormat, amount));
            }
        }
        return simpleForm.build();
    }

    private static List<PaymentRecord> fetchPaymentRecordsAsync(Player player) {
        SPPlayer spPlayer = SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(player.getUniqueId());
        Preconditions.checkNotNull(spPlayer, "Player not found");
        return SPPlugin.getService(DatabaseService.class).getPaymentLogService().getPaymentsByPlayer(spPlayer);
    }
}
