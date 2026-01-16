package org.simpmc.simppay.menu.card;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;
import org.simpmc.simppay.config.types.menu.card.CardPriceMenuConfig;
import org.simpmc.simppay.data.card.CardPrice;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.menu.card.anvil.CardSerialInput;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.util.MessageUtil;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Phase 6: InvUI Migration - Card Price View
 * <p>
 * Displays available card prices for a selected card type.
 */
public class CardPriceView {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Opens the card price selection menu for a player.
     *
     * @param player   Player to show menu to
     * @param cardType Selected card type
     */
    public static void openMenu(Player player, CardType cardType) {
        CardPriceMenuConfig config = ConfigManager.getInstance().getConfig(CardPriceMenuConfig.class);

        // Create items for each card price
        List<Item> priceItems = new ArrayList<>();
        for (String priceString : CardPrice.getAllCardPrices()) {
            String formattedPrice = getFormattedPrice(priceString);
            DisplayItem priceItemConfig = config.priceItem.clone()
                    .replaceStringInName("{price_name}", formattedPrice);

            priceItems.add(new SimpleItem(
                    new ItemBuilder(priceItemConfig.getMaterial())
                            .setDisplayName(mm(priceItemConfig.getName()))
                            .addLoreLines(priceItemConfig.getLores().stream()
                                    .map(CardPriceView::mm)
                                    .toArray(AdventureComponentWrapper[]::new)),
                    click -> {
                        // Build card detail and open serial input
                        CardDetail detail = CardDetail.builder()
                                .type(cardType)
                                .price(CardPrice.fromString(priceString))
                                .build();

                        click.getPlayer().closeInventory();
                        new CardSerialInput(click.getPlayer(), detail);
                    }
            ));
        }

        // Build the PagedGui with all ingredients
        String[] layout = config.layout.toArray(new String[0]);
        PagedGui.Builder<Item> builder = PagedGui.items()
                .setStructure(layout)
                .addIngredient('O', xyz.xenondevs.invui.gui.structure.Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(priceItems);

        // Add display items (borders, decorations)
        Map<Character, DisplayItem> displayItems = config.displayItems;
        for (Map.Entry<Character, DisplayItem> entry : displayItems.entrySet()) {
            DisplayItem item = entry.getValue();
            if (item.getRole() == RoleType.NONE) {
                builder.addIngredient(entry.getKey(), item.getItemStack(player));
            }
        }

        PagedGui<Item> gui = builder.build();

        // Open window
        Window.single()
                .setViewer(player)
                .setTitle(mm(config.title))
                .setGui(gui)
                .build()
                .open();
    }

    /**
     * Formats a price string with thousand separators.
     */
    private static String getFormattedPrice(String price) {
        return String.format("%,d", Integer.valueOf(price)) + "đ";
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
