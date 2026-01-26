package org.simpmc.simppay.forms;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.StreakConfig;
import org.simpmc.simppay.database.entities.PlayerStreakPayment;
import org.simpmc.simppay.service.database.StreakService;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bedrock Forms support for Streak Menu
 * <p>
 * Shows streak stats and reward tiers in a SimpleForm for Bedrock players.
 */
public class StreakForm {

    /**
     * Creates and returns the streak form for a Bedrock player.
     *
     * @param player Player to show form to
     * @return SimpleForm with streak information
     */
    public static SimpleForm getStreakForm(Player player) {
        StreakService streakService = new StreakService();
        PlayerStreakPayment streak = streakService.getStreak(player.getUniqueId());
        StreakConfig config = ConfigManager.getInstance().getConfig(StreakConfig.class);

        // Build form content (header showing streak stats)
        String content = buildStreakContent(player, streak);

        SimpleForm.Builder formBuilder = SimpleForm.builder()
                .title("Payment Streak")
                .content(content);

        // Sort rewards by days ascending
        List<StreakConfig.StreakReward> sortedRewards = config.rewards.stream()
                .sorted(Comparator.comparingInt(r -> r.days))
                .collect(Collectors.toList());

        // Add button for each reward tier
        for (StreakConfig.StreakReward reward : sortedRewards) {
            String buttonLabel = createRewardButtonLabel(reward, streak);
            formBuilder.button(buttonLabel);
        }

        // Handle button clicks
        formBuilder.validResultHandler((simpleForm, result) -> {
            int index = result.clickedButtonId();
            if (index >= 0 && index < sortedRewards.size()) {
                StreakConfig.StreakReward selectedReward = sortedRewards.get(index);
                sendRewardDetails(player, selectedReward, streak);
            }
        });

        return formBuilder.build();
    }

    /**
     * Builds the content section showing current streak stats.
     */
    private static String buildStreakContent(Player player, PlayerStreakPayment streak) {
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

        return String.format(
                "%sCurrent Streak: %s%d%s days\n%sBest Streak: %s%d%s days\n%sLast Payment: %s%s",
                ChatColor.YELLOW, ChatColor.GOLD, currentStreak, ChatColor.YELLOW,
                ChatColor.YELLOW, ChatColor.GOLD, bestStreak, ChatColor.YELLOW,
                ChatColor.GRAY, ChatColor.WHITE, lastPaymentStr
        );
    }

    /**
     * Creates a button label for a reward tier showing its status.
     */
    private static String createRewardButtonLabel(StreakConfig.StreakReward reward, PlayerStreakPayment streak) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int lastRewardTier = streak != null ? streak.getLastRewardTier() : 0;

        boolean achieved = currentStreak >= reward.days;
        boolean claimed = lastRewardTier >= reward.days;

        String statusColor;
        String statusText;

        if (claimed) {
            statusColor = ChatColor.GREEN.toString();
            statusText = "✓ Claimed";
        } else if (achieved) {
            statusColor = ChatColor.YELLOW.toString();
            statusText = "✓ Completed";
        } else {
            statusColor = ChatColor.RED.toString();
            int remaining = reward.days - currentStreak;
            statusText = "✗ Locked (" + remaining + " days)";
        }

        return String.format(
                "%s%s - %s",
                statusColor,
                reward.name,
                statusText
        );
    }

    /**
     * Sends detailed information about a reward to the player's chat.
     */
    private static void sendRewardDetails(Player player, StreakConfig.StreakReward reward, PlayerStreakPayment streak) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int lastRewardTier = streak != null ? streak.getLastRewardTier() : 0;

        boolean achieved = currentStreak >= reward.days;
        boolean claimed = lastRewardTier >= reward.days;

        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + reward.name);
        player.sendMessage(ChatColor.GRAY + "Streak required: " + ChatColor.YELLOW + reward.days + " days");
        player.sendMessage("");

        // Status
        if (claimed) {
            player.sendMessage(ChatColor.GREEN + "✓ Claimed");
        } else if (achieved) {
            player.sendMessage(ChatColor.YELLOW + "✓ Completed (will be awarded)");
        } else {
            int remaining = reward.days - currentStreak;
            player.sendMessage(ChatColor.RED + "✗ Locked");
            player.sendMessage(ChatColor.GRAY + "" + remaining + " more day(s) needed!");
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Rewards:");

        // List rewards
        for (String command : reward.commands) {
            String rewardDesc = parseCommandToDescription(command);
            player.sendMessage(ChatColor.DARK_GRAY + " • " + ChatColor.WHITE + rewardDesc);
        }

        player.sendMessage(ChatColor.GOLD + "═══════════════════════════════");
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
}
