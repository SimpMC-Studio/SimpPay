package org.simpmc.simppay.menu;

import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.menu.ServerPaymentHistoryMenuConfig;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.database.dto.PaymentRecord;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.util.CalendarUtil;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Displays the entire server's payment history with pagination.
 */
public class ServerPaymentHistoryView {

    public static void openMenu(Player player) {
        ServerPaymentHistoryMenuConfig config = ConfigManager.getInstance().getConfig(ServerPaymentHistoryMenuConfig.class);

        CompletableFuture<List<PaymentRecord>> recordsFuture = fetchPaymentRecordsAsync();

        Item loadingItem = new SimpleItem(
                new ItemBuilder(org.bukkit.Material.PAPER)
                        .setDisplayName(MenuBuilder.mm("<gray>Loading server payment history..."))
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

    private static Item createPaymentItem(Player player, PaymentRecord record, ServerPaymentHistoryMenuConfig config) {
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
                            .replace("{transaction_id}", record.getRefId())
                            .replace("{name}", record.getPlayerName()))
                    .toList();
        } else {
            itemConfig = config.bankItem.clone()
                    .replaceStringInName("{amount}", String.format("%,.0f", record.getAmount()) + "đ");

            lore = itemConfig.getLores().stream()
                    .map(line -> line
                            .replace("{time}", CalendarUtil.getFormattedTimestamp(record.getTimestamp().getTime()))
                            .replace("{api}", record.getProvider())
                            .replace("{transaction_id}", record.getRefId())
                            .replace("{name}", record.getPlayerName()))
                    .toList();
        }

        ItemBuilder builder = new ItemBuilder(itemConfig.getMaterial())
                .setDisplayName(MenuBuilder.mm(itemConfig.getName()));
        for (String loreLine : lore) {
            builder.addLoreLines(MenuBuilder.mm(loreLine));
        }

        return new SimpleItem(builder);
    }

    private static CompletableFuture<List<PaymentRecord>> fetchPaymentRecordsAsync() {
        return CompletableFuture.supplyAsync(() ->
                SPPlugin.getService(DatabaseService.class)
                        .getPaymentLogService()
                        .getEntireServerPayments()
        );
    }
}
