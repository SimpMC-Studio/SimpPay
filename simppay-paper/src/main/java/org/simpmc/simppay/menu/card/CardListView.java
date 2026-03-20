package org.simpmc.simppay.menu.card;

import org.bukkit.entity.Player;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.CardConfig;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;
import org.simpmc.simppay.config.types.menu.card.CardListMenuConfig;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.menu.MenuBuilder;
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
 * Displays a paged list of enabled card types for selection.
 */
public class CardListView {

    public static void openMenu(Player player) {
        CardListMenuConfig config = ConfigManager.getInstance().getConfig(CardListMenuConfig.class);
        CardConfig cardConfig = ConfigManager.getInstance().getConfig(CardConfig.class);

        List<Item> cardItems = new ArrayList<>();
        for (CardType cardType : cardConfig.getEnabledCardTypes()) {
            DisplayItem cardItemConfig = config.cardItem.clone()
                    .replaceStringInName("{card_name}", cardType.toString());

            cardItems.add(new SimpleItem(
                    new ItemBuilder(cardItemConfig.getMaterial())
                            .setDisplayName(MenuBuilder.mm(cardItemConfig.getName()))
                            .addLoreLines(cardItemConfig.getLores().stream()
                                    .map(MenuBuilder::mm)
                                    .toArray(xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper[]::new)),
                    click -> CardPriceView.openMenu(click.getPlayer(), cardType)
            ));
        }

        String[] layout = config.layout.toArray(new String[0]);
        PagedGui.Builder<Item> builder = PagedGui.items()
                .setStructure(layout)
                .addIngredient('O', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(cardItems);

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
