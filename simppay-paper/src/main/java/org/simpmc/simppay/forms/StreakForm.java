package org.simpmc.simppay.forms;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.StreakConfig;
import org.simpmc.simppay.config.types.menu.FormsConfig;
import org.simpmc.simppay.database.entities.PlayerStreakPayment;
import org.simpmc.simppay.service.database.StreakService;
import org.simpmc.simppay.util.CommandDescriptionUtil;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bedrock Forms support for Streak Menu.
 * Shows streak stats and reward tiers in a SimpleForm for Bedrock players.
 */
public class StreakForm {

    public static SimpleForm getStreakForm(Player player) {
        StreakService streakService = new StreakService();
        PlayerStreakPayment streak = streakService.getStreak(player.getUniqueId());
        StreakConfig config = ConfigManager.getInstance().getConfig(StreakConfig.class);
        FormsConfig.StreakFormStrings f = ConfigManager.getInstance().getConfig(FormsConfig.class).streakForm;

        String content = buildStreakContent(streak, f);

        SimpleForm.Builder formBuilder = SimpleForm.builder()
                .title(f.title)
                .content(content);

        List<StreakConfig.StreakReward> sortedRewards = config.rewards.stream()
                .sorted(Comparator.comparingInt(r -> r.days))
                .collect(Collectors.toList());

        for (StreakConfig.StreakReward reward : sortedRewards) {
            formBuilder.button(createRewardButtonLabel(reward, streak, f));
        }

        formBuilder.validResultHandler((simpleForm, result) -> {
            int index = result.clickedButtonId();
            if (index >= 0 && index < sortedRewards.size()) {
                sendRewardDetails(player, sortedRewards.get(index), streak, f);
            }
        });

        return formBuilder.build();
    }

    private static String buildStreakContent(PlayerStreakPayment streak, FormsConfig.StreakFormStrings f) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int bestStreak = streak != null ? streak.getBestStreak() : 0;
        Date lastRecharge = streak != null ? streak.getLastRechargeDate() : null;

        String lastPaymentStr = lastRecharge != null
                ? new SimpleDateFormat(f.dateFormat).format(lastRecharge)
                : f.neverText;

        return String.format(f.currentStreakFormat, currentStreak) + "\n"
                + String.format(f.bestStreakFormat, bestStreak) + "\n"
                + String.format(f.lastPaymentFormat, lastPaymentStr);
    }

    private static String createRewardButtonLabel(StreakConfig.StreakReward reward, PlayerStreakPayment streak,
                                                  FormsConfig.StreakFormStrings f) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int lastRewardTier = streak != null ? streak.getLastRewardTier() : 0;

        boolean achieved = currentStreak >= reward.days;
        boolean claimed = lastRewardTier >= reward.days;

        String statusText;
        if (claimed) {
            statusText = f.statusClaimed;
        } else if (achieved) {
            statusText = f.statusCompleted;
        } else {
            statusText = String.format(f.statusLockedFormat, reward.days - currentStreak);
        }

        return reward.name + " - " + statusText;
    }

    private static void sendRewardDetails(Player player, StreakConfig.StreakReward reward,
                                          PlayerStreakPayment streak, FormsConfig.StreakFormStrings f) {
        int currentStreak = streak != null ? streak.getCurrentStreak() : 0;
        int lastRewardTier = streak != null ? streak.getLastRewardTier() : 0;

        boolean achieved = currentStreak >= reward.days;
        boolean claimed = lastRewardTier >= reward.days;

        player.sendMessage(f.detailSeparator);
        player.sendMessage(reward.name);
        player.sendMessage(String.format(f.daysRequiredFormat, reward.days));
        player.sendMessage("");

        if (claimed) {
            player.sendMessage(f.claimedText);
        } else if (achieved) {
            player.sendMessage(f.completedText);
        } else {
            player.sendMessage(f.lockedText);
            player.sendMessage(String.format(f.remainingDaysFormat, reward.days - currentStreak));
        }

        player.sendMessage("");
        player.sendMessage(f.rewardsLabel);

        for (String command : reward.commands) {
            player.sendMessage(" - " + CommandDescriptionUtil.parseCommandToDescription(command));
        }

        player.sendMessage(f.detailSeparator);
    }
}
