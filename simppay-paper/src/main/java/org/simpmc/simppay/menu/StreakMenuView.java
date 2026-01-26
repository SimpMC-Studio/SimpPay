package org.simpmc.simppay.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.StreakConfig;
import org.simpmc.simppay.config.types.menu.StreakMenuConfig;
import org.simpmc.simppay.database.entities.PlayerStreakPayment;
import org.simpmc.simppay.service.database.StreakService;
import org.simpmc.simppay.util.MessageUtil;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.item.impl.controlitem.PageItem;
import xyz.xenondevs.invui.window.Window;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Phase 5: Streak Menu View using InvUI with PagedGui
 * <p>
 * Displays player's payment streak information and available rewards with pagination.
 */
public class StreakMenuView {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    /**
     * Opens the streak menu for a player.
     *
     * @param player Player to show menu to
     */
    public static void openMenu(Player player) {
        StreakMenuConfig menuConfig = ConfigManager.getInstance().getConfig(StreakMenuConfig.class);
        StreakConfig streakConfig = ConfigManager.getInstance().getConfig(StreakConfig.class);

        // Async load streak data
        CompletableFuture<PlayerStreakPayment> streakFuture = fetchStreakDataAsync(player.getUniqueId());

        // Create loading placeholder
        Item loadingItem = new SimpleItem(
                new ItemBuilder(Material.PAPER)
                        .setDisplayName(mm("<gray>Loading rewards..."))
        );

        // Build GUI structure first (will be populated when data loads)
        String[] layout = menuConfig.layout.toArray(new String[0]);
        List<Item> rewardItems = new ArrayList<>();
        rewardItems.add(loadingItem); // Temporary loading item

        PagedGui.Builder<Item> builder = PagedGui.items()
                .setStructure(layout)
                .addIngredient('O', xyz.xenondevs.invui.gui.structure.Markers.CONTENT_LIST_SLOT_HORIZONTAL)
                .setContent(rewardItems);

        // Add display items (borders, navigation buttons)
        for (Map.Entry<Character, org.simpmc.simppay.config.types.data.menu.DisplayItem> entry : menuConfig.displayItems.entrySet()) {
            org.simpmc.simppay.config.types.data.menu.DisplayItem item = entry.getValue();

            if (item.getRole() == org.simpmc.simppay.config.types.data.menu.RoleType.PREV_PAGE) {
                builder.addIngredient(entry.getKey(), new PageItem(false) {
                    @Override
                    public xyz.xenondevs.invui.item.ItemProvider getItemProvider(PagedGui<?> gui) {
                        return new xyz.xenondevs.invui.item.ItemWrapper(item.getItemStack(player));
                    }
                });
            } else if (item.getRole() == org.simpmc.simppay.config.types.data.menu.RoleType.NEXT_PAGE) {
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

        // Add streak info item (fixed position, always visible)
        streakFuture.thenAccept(streak -> {
            Item streakInfoItem = createStreakInfoItem(player, streak, menuConfig);
            builder.addIngredient('s', streakInfoItem);
        });

        PagedGui<Item> gui = builder.build();

        // Open window
        Window window = Window.single()
                .setViewer(player)
                .setTitle(mm(menuConfig.title))
                .setGui(gui)
                .build();
        window.open();

        // Load data asynchronously and update GUI
        streakFuture.thenAccept(streak -> {
            rewardItems.clear();

            // Sort rewards by days ascending
            List<StreakConfig.StreakReward> sortedRewards = streakConfig.rewards.stream()
                    .sorted(Comparator.comparingInt(r -> r.days))
                    .collect(Collectors.toList());

            if (sortedRewards.isEmpty()) {
                rewardItems.add(new SimpleItem(
                        new ItemBuilder(Material.BARRIER)
                                .setDisplayName(mm("<red>No rewards configured"))
                ));
            } else {
                for (StreakConfig.StreakReward reward : sortedRewards) {
                    rewardItems.add(createRewardItem(player, reward, streak, menuConfig));
                }
            }

            // Update streak info item on main thread
            Item streakInfoItem = createStreakInfoItem(player, streak, menuConfig);

            // Force GUI refresh on main thread
            SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (window.isOpen()) {
                    gui.setContent(rewardItems);
                    // Update the streak info item
                    for (int i = 0; i < layout.length; i++) {
                        String row = layout[i];
                        for (int j = 0; j < row.length(); j++) {
                            if (row.charAt(j) == 's') {
                                gui.setItem(i * 9 + j, streakInfoItem);
                                break;
                            }
                        }
                    }
                }
            });
        });
    }

    /**
     * Creates the main streak information display item.
     */
    private static Item createStreakInfoItem(Player player, PlayerStreakPayment streak, StreakMenuConfig config) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int bestStreak = streak != null ? streak.getBestStreak() : 0;
        Date lastRecharge = streak != null ? streak.getLastRechargeDate() : null;

        String lastPaymentStr;
        if (lastRecharge != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            lastPaymentStr = sdf.format(lastRecharge);
        } else {
            lastPaymentStr = "Never";
        }

        // Replace placeholders in lore
        List<Component> lore = config.streakInfoItem.lore.stream()
                .map(line -> line
                        .replace("{current_streak}", String.valueOf(currentStreak))
                        .replace("{best_streak}", String.valueOf(bestStreak))
                        .replace("{last_payment}", lastPaymentStr))
                .map(line -> MM.deserialize(line))
                .collect(Collectors.toList());

        ItemBuilder builder = new ItemBuilder(config.streakInfoItem.material)
                .setDisplayName(mm(config.streakInfoItem.name));

        for (Component line : lore) {
            builder.addLoreLines(new AdventureComponentWrapper(line));
        }

        return new SimpleItem(builder);
    }

    /**
     * Creates a reward tier display item.
     */
    private static Item createRewardItem(Player player, StreakConfig.StreakReward reward, PlayerStreakPayment streak, StreakMenuConfig config) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int lastRewardTier = streak != null ? streak.getLastRewardTier() : 0;

        boolean achieved = currentStreak >= reward.days;
        boolean claimed = lastRewardTier >= reward.days;

        // Select appropriate template
        StreakMenuConfig.RewardItemTemplate template;
        if (claimed) {
            template = config.rewardItems.claimed;
        } else if (achieved) {
            template = config.rewardItems.completed;
        } else {
            template = config.rewardItems.locked;
        }

        // Build rewards list
        String rewardsList = reward.commands.stream()
                .map(StreakMenuView::parseCommandToDescription)
                .map(desc -> "<dark_gray>• <white>" + desc)
                .collect(Collectors.joining("\n"));

        int remainingDays = Math.max(0, reward.days - currentStreak);

        // Replace placeholders in lore
        List<Component> lore = template.lore.stream()
                .flatMap(line -> {
                    // Handle multi-line rewards_list expansion
                    if (line.contains("{rewards_list}")) {
                        return Arrays.stream(rewardsList.split("\n"))
                                .map(rewardLine -> line.replace("{rewards_list}", rewardLine));
                    }
                    return java.util.stream.Stream.of(line);
                })
                .map(line -> line
                        .replace("{reward_name}", reward.name)
                        .replace("{days}", String.valueOf(reward.days))
                        .replace("{remaining_days}", String.valueOf(remainingDays)))
                .map(line -> MM.deserialize(line))
                .collect(Collectors.toList());

        // Replace placeholders in name
        String name = template.name
                .replace("{reward_name}", reward.name)
                .replace("{days}", String.valueOf(reward.days));

        ItemBuilder builder = new ItemBuilder(template.material)
                .setDisplayName(mm(name));

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
     * Fetches streak data asynchronously.
     */
    private static CompletableFuture<PlayerStreakPayment> fetchStreakDataAsync(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            StreakService streakService = new StreakService();
            return streakService.getStreak(playerUUID);
        });
    }

    /**
     * Helper method to create ComponentWrapper from MiniMessage string.
     */
    private static AdventureComponentWrapper mm(String miniMessage) {
        return new AdventureComponentWrapper(
                MessageUtil.getComponentParsed(miniMessage, null)
        );
    }
}
