package org.simpmc.simppay.menu;

import com.google.common.base.Preconditions;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.menu.PaymentHistoryMenuConfig;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.database.dto.PaymentRecord;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.util.CalendarUtil;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Displays a player's payment history with pagination.
 */
public class PaymentHistoryView {

    public static void openMenu(Player player, String playerName) {
        PaymentHistoryMenuConfig config = ConfigManager.getInstance().getConfig(PaymentHistoryMenuConfig.class);

        CompletableFuture<List<PaymentRecord>> recordsFuture = fetchPaymentRecordsAsync(player.getUniqueId(), playerName);

        Item loadingItem = new SimpleItem(
                new ItemBuilder(org.bukkit.Material.PAPER)
                        .setDisplayName(MenuBuilder.mm("<gray>Loading payment history..."))
        );

        String[] layout = config.layout.toArray(new String[0]);
        List<Item> paymentItems = new ArrayList<>();
        paymentItems.add(loadingItem);

        PagedGui<Item> gui = MenuBuilder.buildPagedGui(layout, config.displayItems, paymentItems, player);

        Window window = Window.single()
                .setViewer(player)
                .setTitle(MenuBuilder.mm(config.title))
                .setGui(gui)
                .build();
        window.open();

        recordsFuture.thenAccept(records -> {
            paymentItems.clear();

            if (records.isEmpty()) {
                paymentItems.add(new SimpleItem(
                        new ItemBuilder(org.bukkit.Material.BARRIER)
                                .setDisplayName(MenuBuilder.mm("<red>No payment history found"))
                ));
            } else {
                for (PaymentRecord record : records) {
                    paymentItems.add(createPaymentItem(player, record, config));
                }
            }

            SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (window.isOpen()) {
                    gui.setContent(paymentItems);
                }
            });
        });
    }

    private static Item createPaymentItem(Player player, PaymentRecord record, PaymentHistoryMenuConfig config) {
        DisplayItem itemConfig;
        List<String> lore;

        if (record.getPaymentType() == PaymentType.CARD) {
            itemConfig = config.cardItem.clone()
                    .replaceStringInName("{amount}", String.format("%,.0f", record.getAmount()) + "đ")
                    .replaceStringInName("{card_type}", record.getTelco());

            lore = itemConfig.getLores().stream()
                    .map(line -> line
                            .replace("{time}", CalendarUtil.getFormattedTimestamp(record.getTimestamp().getTime()))
                            .replace("{serial}", record.getSerial().orElse("0"))
                            .replace("{pin}", record.getPin().orElse("0"))
                            .replace("{api}", record.getProvider())
                            .replace("{transaction_id}", record.getRefId()))
                    .toList();
        } else {
            itemConfig = config.bankItem.clone()
                    .replaceStringInName("{amount}", String.format("%,.0f", record.getAmount()) + "đ");

            lore = itemConfig.getLores().stream()
                    .map(line -> line
                            .replace("{time}", CalendarUtil.getFormattedTimestamp(record.getTimestamp().getTime()))
                            .replace("{api}", record.getProvider())
                            .replace("{transaction_id}", record.getRefId()))
                    .toList();
        }

        ItemBuilder builder = new ItemBuilder(itemConfig.getMaterial())
                .setDisplayName(MenuBuilder.mm(itemConfig.getName()));
        for (String loreLine : lore) {
            builder.addLoreLines(MenuBuilder.mm(loreLine));
        }

        return new SimpleItem(builder);
    }

    private static CompletableFuture<List<PaymentRecord>> fetchPaymentRecordsAsync(UUID playerUUID, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            SPPlayer spPlayer = playerName == null
                    ? SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(playerUUID)
                    : SPPlugin.getService(DatabaseService.class).getPlayerService().findByName(playerName);
            Preconditions.checkNotNull(spPlayer, "Player not found");
            return SPPlugin.getService(DatabaseService.class).getPaymentLogService().getPaymentsByPlayer(spPlayer);
        });
    }
}
