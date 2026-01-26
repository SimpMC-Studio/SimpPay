package org.simpmc.simppay.menu.card;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.CardConfig;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;
import org.simpmc.simppay.config.types.menu.card.CardListMenuConfig;
import org.simpmc.simppay.data.card.CardType;
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
 * Phase 6: InvUI Migration - Card List View
 * <p>
 * Displays a paged list of enabled card types for selection.
 */
public class CardListView {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Opens the card list menu for a player.
     *
     * @param player Player to show menu to
     */
    public static void openMenu(Player player) {
        CardListMenuConfig config = ConfigManager.getInstance().getConfig(CardListMenuConfig.class);
        CardConfig cardConfig = ConfigManager.getInstance().getConfig(CardConfig.class);

        // Create items for each enabled card type
        List<Item> cardItems = new ArrayList<>();
        for (CardType cardType : cardConfig.getEnabledCardTypes()) {
            DisplayItem cardItemConfig = config.cardItem.clone()
                    .replaceStringInName("{card_name}", cardType.toString());

            cardItems.add(new SimpleItem(
                    new ItemBuilder(cardItemConfig.getMaterial())
                            .setDisplayName(mm(cardItemConfig.getName()))
                            .addLoreLines(cardItemConfig.getLores().stream()
                                    .map(CardListView::mm)
                                    .toArray(AdventureComponentWrapper[]::new)),
                    click -> {
                        // Open card price selection menu
                        CardPriceView.openMenu(click.getPlayer(), cardType);
                    }
            ));
        }

        // Build the PagedGui with all ingredients
        String[] layout = config.layout.toArray(new String[0]);
        PagedGui.Builder<Item> builder = PagedGui.items()
                .setStructure(layout)
                .addIngredient('O', xyz.xenondevs.invui.gui.structure.Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(cardItems);

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
     * Helper method to create AdventureComponentWrapper from MiniMessage string.
     */
    private static AdventureComponentWrapper mm(String miniMessage) {
        return new AdventureComponentWrapper(
                MessageUtil.getComponentParsed(miniMessage, null)
        );
    }
}
