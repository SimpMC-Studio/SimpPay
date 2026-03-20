package org.simpmc.simppay.menu;

import org.bukkit.entity.Player;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;
import org.simpmc.simppay.util.MessageUtil;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.ItemProvider;
import xyz.xenondevs.invui.item.ItemWrapper;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;

import java.util.List;
import java.util.Map;

/**
 * Shared helpers for all InvUI menu views.
 */
public class MenuBuilder {

    /**
     * Parses a MiniMessage string into an AdventureComponentWrapper for InvUI.
     */
    public static AdventureComponentWrapper mm(String miniMessage) {
        return new AdventureComponentWrapper(MessageUtil.getComponentParsed(miniMessage, null));
    }

    /**
     * Builds a PagedGui with layout, display items (borders/navigation), and content.
     * Handles PREV_PAGE, NEXT_PAGE, and NONE role types automatically.
     *
     * @param layout       Layout strings from config
     * @param displayItems Border/navigation items keyed by layout character
     * @param content      Content items to populate 'O' slots
     * @param player       Player for head-based display items
     * @return Built PagedGui
     */
    public static PagedGui<Item> buildPagedGui(String[] layout, Map<Character, DisplayItem> displayItems,
                                               List<Item> content, Player player) {
        PagedGui.Builder<Item> builder = PagedGui.items()
                .setStructure(layout)
                .addIngredient('O', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(content);

        for (Map.Entry<Character, DisplayItem> entry : displayItems.entrySet()) {
            DisplayItem item = entry.getValue();

            if (item.getRole() == RoleType.PREV_PAGE) {
                builder.addIngredient(entry.getKey(), new PageItem(false) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new ItemWrapper(item.getItemStack(player));
                    }
                });
            } else if (item.getRole() == RoleType.NEXT_PAGE) {
                builder.addIngredient(entry.getKey(), new PageItem(true) {
                    @Override
                    public ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new ItemWrapper(item.getItemStack(player));
                    }
                });
            } else {
                builder.addIngredient(entry.getKey(), item.getItemStack(player));
            }
        }

        return builder.build();
    }
}
