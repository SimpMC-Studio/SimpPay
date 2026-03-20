package org.simpmc.simppay.menu.card;

import org.bukkit.entity.Player;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;
import org.simpmc.simppay.config.types.menu.card.CardPriceMenuConfig;
import org.simpmc.simppay.data.card.CardPrice;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.menu.MenuBuilder;
import org.simpmc.simppay.menu.card.anvil.CardSerialInput;
import org.simpmc.simppay.model.detail.CardDetail;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Displays available card prices for a selected card type.
 */
public class CardPriceView {

    public static void openMenu(Player player, CardType cardType) {
        CardPriceMenuConfig config = ConfigManager.getInstance().getConfig(CardPriceMenuConfig.class);

        List<Item> priceItems = new ArrayList<>();
        for (String priceString : CardPrice.getAllCardPrices()) {
            String formattedPrice = String.format("%,d", Integer.valueOf(priceString)) + "đ";
            DisplayItem priceItemConfig = config.priceItem.clone()
                    .replaceStringInName("{price_name}", formattedPrice);

            priceItems.add(new SimpleItem(
                    new ItemBuilder(priceItemConfig.getMaterial())
                            .setDisplayName(MenuBuilder.mm(priceItemConfig.getName()))
                            .addLoreLines(priceItemConfig.getLores().stream()
                                    .map(MenuBuilder::mm)
                                    .toArray(xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper[]::new)),
                    click -> {
                        CardDetail detail = CardDetail.builder()
                                .type(cardType)
                                .price(CardPrice.fromString(priceString))
                                .build();
                        new CardSerialInput(click.getPlayer(), detail);
                    }
            ));
        }

        String[] layout = config.layout.toArray(new String[0]);
        PagedGui.Builder<Item> builder = PagedGui.items()
                .setStructure(layout)
                .addIngredient('O', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(priceItems);

        for (Map.Entry<Character, DisplayItem> entry : config.displayItems.entrySet()) {
            DisplayItem item = entry.getValue();
            if (item.getRole() == RoleType.NONE) {
                builder.addIngredient(entry.getKey(), item.getItemStack(player));
            }
        }

        PagedGui<Item> gui = builder.build();

        Window.single()
                .setViewer(player)
                .setTitle(MenuBuilder.mm(config.title))
                .setGui(gui)
                .build()
                .open();
    }
}
