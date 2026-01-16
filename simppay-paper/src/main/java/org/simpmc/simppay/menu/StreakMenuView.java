package org.simpmc.simppay.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.StreakConfig;
import org.simpmc.simppay.database.entities.PlayerStreakPayment;
import org.simpmc.simppay.service.database.StreakService;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Phase 5: Streak Menu View using InvUI
 * <p>
 * Displays player's payment streak information and available rewards.
 */
public class StreakMenuView {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Opens the streak menu for a player.
     *
     * @param player Player to show menu to
     */
    public static void openMenu(Player player) {
        StreakService streakService = new StreakService();
        PlayerStreakPayment streak = streakService.getStreak(player.getUniqueId());
        StreakConfig config = ConfigManager.getInstance().getConfig(StreakConfig.class);

        // Create streak info item
        Item streakInfo = createStreakInfoItem(player, streak);

        // Create reward items
        List<Item> rewardItems = new ArrayList<>();
        for (StreakConfig.StreakReward reward : config.rewards) {
            rewardItems.add(createRewardItem(player, reward, streak));
        }

        // Create border item
        Item border = new SimpleItem(new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .setDisplayName(mm("")));

        // Build GUI
        Gui.Builder.Normal guiBuilder = Gui.normal()
                .setStructure(
                        "# # # # # # # # #",
                        "# s # # # # # # #",
                        "# r r r r r r r #",
                        "# # # # # # # # #")
                .addIngredient('#', border)
                .addIngredient('s', streakInfo);

        // Add reward items dynamically (up to 7 slots)
        for (int i = 0; i < Math.min(7, rewardItems.size()); i++) {
            guiBuilder.addIngredient('r', rewardItems.get(i));
        }

        Gui gui = guiBuilder.build();

        // Open window
        Window.single()
                .setViewer(player)
                .setTitle(mm("<gold><bold>Payment Streak</bold></gold>"))
                .setGui(gui)
                .build()
                .open();
    }

    /**
     * Creates the main streak information display item.
     */
    private static Item createStreakInfoItem(Player player, PlayerStreakPayment streak) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int bestStreak = streak != null ? streak.getBestStreak() : 0;
        Date lastRecharge = streak != null ? streak.getLastRechargeDate() : null;

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<gray>Your consecutive payment streak"));
        lore.add(MM.deserialize(""));
        lore.add(MM.deserialize("<yellow>Current Streak: <gold>" + currentStreak + "</gold> days"));
        lore.add(MM.deserialize("<yellow>Best Streak: <gold>" + bestStreak + "</gold> days"));

        if (lastRecharge != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            lore.add(MM.deserialize(""));
            lore.add(MM.deserialize("<gray>Last Payment: <white>" + sdf.format(lastRecharge) + "</white>"));
        }

        lore.add(MM.deserialize(""));
        lore.add(MM.deserialize("<dark_gray>Make a payment every day to"));
        lore.add(MM.deserialize("<dark_gray>increase your streak!"));

        ItemBuilder builder = new ItemBuilder(Material.NETHER_STAR)
                .setDisplayName(mm("<gold><bold>Your Streak</bold></gold>"));

        for (Component line : lore) {
            builder.addLoreLines(new AdventureComponentWrapper(line));
        }

        return new SimpleItem(builder);
    }

    /**
     * Creates a reward tier display item.
     */
    private static Item createRewardItem(Player player, StreakConfig.StreakReward reward, PlayerStreakPayment streak) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int lastRewardTier = streak != null ? streak.getLastRewardTier() : 0;

        boolean achieved = currentStreak >= reward.days;
        boolean claimed = lastRewardTier >= reward.days;

        Material material;
        String status;

        if (claimed) {
            material = Material.LIME_WOOL;
            status = "<green>✓ Claimed</green>";
        } else if (achieved) {
            material = Material.YELLOW_WOOL;
            status = "<yellow>✓ Completed (will be awarded)</yellow>";
        } else {
            material = Material.RED_WOOL;
            status = "<red>✗ Locked</red>";
        }

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<gray>Streak required: <yellow>" + reward.days + "</yellow> days"));
        lore.add(MM.deserialize(""));
        lore.add(MM.deserialize(status));
        lore.add(MM.deserialize(""));
        lore.add(MM.deserialize("<gray>Rewards:"));

        for (String command : reward.commands) {
            // Parse command to show readable reward
            String rewardDesc = parseCommandToDescription(command);
            lore.add(MM.deserialize("<dark_gray>• <white>" + rewardDesc));
        }

        if (!achieved) {
            int remaining = reward.days - currentStreak;
            lore.add(MM.deserialize(""));
            lore.add(MM.deserialize("<gray>" + remaining + " more day" + (remaining == 1 ? "" : "s") + " needed!"));
        }

        ItemBuilder builder = new ItemBuilder(material)
                .setDisplayName(mm("<gold>" + reward.name));

        for (Component line : lore) {
            builder.addLoreLines(new AdventureComponentWrapper(line));
        }

        return new SimpleItem(builder);
    }

    /**
     * Parses a command into a human-readable description.
     * Example: "give %player_name% diamond 5" -> "5x Diamond"
     */
    private static String parseCommandToDescription(String command) {
        // Simple parsing - you can enhance this
        if (command.contains("give")) {
            String[] parts = command.split(" ");
            if (parts.length >= 4) {
                String item = parts[2];
                String amount = parts.length >= 4 ? parts[3] : "1";
                return amount + "x " + formatItemName(item);
            }
        }

        // Fallback: just return the command
        return command;
    }

    /**
     * Formats item material names to be more readable.
     * Example: "diamond" -> "Diamond", "gold_ingot" -> "Gold Ingot"
     */
    private static String formatItemName(String materialName) {
        String[] words = materialName.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }

        return result.toString();
    }

    /**
     * Helper method to create ComponentWrapper from MiniMessage string.
     */
    private static AdventureComponentWrapper mm(String miniMessage) {
        return new AdventureComponentWrapper(MM.deserialize(miniMessage));
    }
}
