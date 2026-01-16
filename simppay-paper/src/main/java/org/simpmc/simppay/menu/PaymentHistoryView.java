package org.simpmc.simppay.menu;

import com.google.common.base.Preconditions;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;
import org.simpmc.simppay.config.types.menu.PaymentHistoryMenuConfig;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.database.dto.PaymentRecord;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.util.CalendarUtil;
import org.simpmc.simppay.util.MessageUtil;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Phase 6: InvUI Migration - Payment History View
 * <p>
 * Displays a player's payment history with pagination.
 */
public class PaymentHistoryView {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Opens the payment history menu for a player.
     *
     * @param player     Player to show menu to
     * @param playerName Optional player name to look up (null for current player)
     */
    public static void openMenu(Player player, String playerName) {
        PaymentHistoryMenuConfig config = ConfigManager.getInstance().getConfig(PaymentHistoryMenuConfig.class);

        // Async load payment records
        CompletableFuture<List<PaymentRecord>> recordsFuture = fetchPaymentRecordsAsync(player.getUniqueId(), playerName);

        // Create loading placeholder
        Item loadingItem = new SimpleItem(
                new ItemBuilder(org.bukkit.Material.PAPER)
                        .setDisplayName(mm("<gray>Loading payment history..."))
        );

        // Build GUI structure first (will be populated when data loads)
        String[] layout = config.layout.toArray(new String[0]);
        List<Item> paymentItems = new ArrayList<>();
        paymentItems.add(loadingItem); // Temporary loading item

        PagedGui.Builder<Item> builder = PagedGui.items()
                .setStructure(layout)
                .addIngredient('O', xyz.xenondevs.invui.gui.structure.Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(paymentItems);

        // Add display items (borders, navigation buttons)
        Map<Character, DisplayItem> displayItems = config.displayItems;
        for (Map.Entry<Character, DisplayItem> entry : displayItems.entrySet()) {
            DisplayItem item = entry.getValue();

            if (item.getRole() == RoleType.PREV_PAGE) {
                builder.addIngredient(entry.getKey(), new PageItem(false) {
                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new xyz.xenondevs.invui.item.ItemWrapper(item.getItemStack(player));
                    }
                });
            } else if (item.getRole() == RoleType.NEXT_PAGE) {
                builder.addIngredient(entry.getKey(), new PageItem(true) {
                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new xyz.xenondevs.invui.item.ItemWrapper(item.getItemStack(player));
                    }
                });
            } else {
                builder.addIngredient(entry.getKey(), item.getItemStack(player));
            }
        }

        PagedGui<Item> gui = builder.build();

        // Open window
        Window window = Window.single()
                .setViewer(player)
                .setTitle(mm(config.title))
                .setGui(gui)
                .build();
        window.open();

        // Load data asynchronously and update GUI
        recordsFuture.thenAccept(records -> {
            paymentItems.clear();

            if (records.isEmpty()) {
                paymentItems.add(new SimpleItem(
                        new ItemBuilder(org.bukkit.Material.BARRIER)
                                .setDisplayName(mm("<red>No payment history found"))
                ));
            } else {
                for (PaymentRecord record : records) {
                    paymentItems.add(createPaymentItem(player, record, config));
                }
            }

            // Force GUI refresh on main thread
            SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (window.isOpen()) {
                    gui.setContent(paymentItems);
                }
            });
        });
    }

    /**
     * Creates a payment item for a PaymentRecord.
     */
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
        } else { // BANKING
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
                .setDisplayName(mm(itemConfig.getName()));

        for (String loreLine : lore) {
            builder.addLoreLines(mm(loreLine));
        }

        return new SimpleItem(builder);
    }

    /**
     * Fetches payment records asynchronously.
     */
    private static CompletableFuture<List<PaymentRecord>> fetchPaymentRecordsAsync(UUID playerUUID, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            SPPlayer spPlayer;
            if (playerName == null) {
                spPlayer = SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(playerUUID);
            } else {
                spPlayer = SPPlugin.getService(DatabaseService.class).getPlayerService().findByName(playerName);
            }
            Preconditions.checkNotNull(spPlayer, "Player not found");

            return SPPlugin.getService(DatabaseService.class)
                    .getPaymentLogService()
                    .getPaymentsByPlayer(spPlayer);
        });
    }

    /**
     * Helper method to create AdventureComponentWrapper from MiniMessage string.
     */
    private static AdventureComponentWrapper mm(String miniMessage) {
        return new AdventureComponentWrapper(
                MessageUtil.getComponentParsed(miniMessage, null)
        );
    }
}
