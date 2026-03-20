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
import org.simpmc.simppay.util.CommandDescriptionUtil;
import xyz.xenondevs.inventoryaccess.component.AdventureComponentWrapper;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.builder.ItemBuilder;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Displays player's payment streak information and available rewards with pagination.
 */
public class StreakMenuView {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    public static void openMenu(Player player) {
        StreakMenuConfig menuConfig = ConfigManager.getInstance().getConfig(StreakMenuConfig.class);
        StreakConfig streakConfig = ConfigManager.getInstance().getConfig(StreakConfig.class);

        CompletableFuture<PlayerStreakPayment> streakFuture = fetchStreakDataAsync(player.getUniqueId());

        Item loadingItem = new SimpleItem(
                new ItemBuilder(Material.PAPER)
                        .setDisplayName(MenuBuilder.mm("<gray>Loading rewards..."))
        );

        String[] layout = menuConfig.layout.toArray(new String[0]);
        List<Item> rewardItems = new ArrayList<>();
        rewardItems.add(loadingItem);

        PagedGui<Item> gui = MenuBuilder.buildPagedGui(layout, menuConfig.displayItems, rewardItems, player);

        Window window = Window.single()
                .setViewer(player)
                .setTitle(MenuBuilder.mm(menuConfig.title))
                .setGui(gui)
                .build();
        window.open();

        streakFuture.thenAccept(streak -> {
            rewardItems.clear();

            List<StreakConfig.StreakReward> sortedRewards = streakConfig.rewards.stream()
                    .sorted(Comparator.comparingInt(r -> r.days))
                    .collect(Collectors.toList());

            if (sortedRewards.isEmpty()) {
                rewardItems.add(new SimpleItem(
                        new ItemBuilder(Material.BARRIER)
                                .setDisplayName(MenuBuilder.mm("<red>No rewards configured"))
                ));
            } else {
                for (StreakConfig.StreakReward reward : sortedRewards) {
                    rewardItems.add(createRewardItem(player, reward, streak, menuConfig));
                }
            }

            Item streakInfoItem = createStreakInfoItem(player, streak, menuConfig);

            SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
                if (window.isOpen()) {
                    gui.setContent(rewardItems);
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

    private static Item createStreakInfoItem(Player player, PlayerStreakPayment streak, StreakMenuConfig config) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int bestStreak = streak != null ? streak.getBestStreak() : 0;
        Date lastRecharge = streak != null ? streak.getLastRechargeDate() : null;

        String lastPaymentStr = lastRecharge != null
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(lastRecharge)
                : "Never";

        List<Component> lore = config.streakInfoItem.lore.stream()
                .map(line -> line
                        .replace("{current_streak}", String.valueOf(currentStreak))
                        .replace("{best_streak}", String.valueOf(bestStreak))
                        .replace("{last_payment}", lastPaymentStr))
                .map(MM::deserialize)
                .collect(Collectors.toList());

        ItemBuilder builder = new ItemBuilder(config.streakInfoItem.material)
                .setDisplayName(MenuBuilder.mm(config.streakInfoItem.name));
        for (Component line : lore) {
            builder.addLoreLines(new AdventureComponentWrapper(line));
        }

        return new SimpleItem(builder);
    }

    private static Item createRewardItem(Player player, StreakConfig.StreakReward reward,
                                         PlayerStreakPayment streak, StreakMenuConfig config) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int lastRewardTier = streak != null ? streak.getLastRewardTier() : 0;

        boolean achieved = currentStreak >= reward.days;
        boolean claimed = lastRewardTier >= reward.days;

        StreakMenuConfig.RewardItemTemplate template = claimed ? config.rewardItems.claimed
                : achieved ? config.rewardItems.completed
                : config.rewardItems.locked;

        String rewardsList = reward.commands.stream()
                .map(CommandDescriptionUtil::parseCommandToDescription)
                .map(desc -> "<dark_gray>• <white>" + desc)
                .collect(Collectors.joining("\n"));

        int remainingDays = Math.max(0, reward.days - currentStreak);

        List<Component> lore = template.lore.stream()
                .flatMap(line -> {
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
                .map(MM::deserialize)
                .collect(Collectors.toList());

        String name = template.name
                .replace("{reward_name}", reward.name)
                .replace("{days}", String.valueOf(reward.days));

        ItemBuilder builder = new ItemBuilder(template.material)
                .setDisplayName(MenuBuilder.mm(name));
        for (Component line : lore) {
            builder.addLoreLines(new AdventureComponentWrapper(line));
        }

        return new SimpleItem(builder);
    }

    private static CompletableFuture<PlayerStreakPayment> fetchStreakDataAsync(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> new StreakService().getStreak(playerUUID));
    }
}
