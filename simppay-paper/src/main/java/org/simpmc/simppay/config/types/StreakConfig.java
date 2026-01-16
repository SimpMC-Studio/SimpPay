package org.simpmc.simppay.config.types;

import de.exlll.configlib.Configuration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 5: Streak system configuration.
 * <p>
 * Defines rewards for consecutive daily payment streaks.
 */
@Configuration
public class StreakConfig {
    public StreakSettings settings = new StreakSettings();
    public List<StreakReward> rewards = new ArrayList<>(List.of(
            new StreakReward(3, "3 Day Streak", List.of("give %player_name% diamond 3")),
            new StreakReward(7, "7 Day Streak", List.of("give %player_name% diamond 7")),
            new StreakReward(14, "14 Day Streak", List.of("give %player_name% diamond 14")),
            new StreakReward(30, "30 Day Streak", List.of("give %player_name% diamond 30"))
    ));

    @Configuration
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakSettings {
        /**
         * Whether streak system is enabled
         */
        public boolean enabled = true;

        /**
         * Whether to send notification when streak increases
         */
        public boolean notifyOnStreak = true;

        /**
         * Message sent when streak increases
         */
        public String streakIncreaseMessage = "<green>Your payment streak is now <gold>%streak%</gold> days!";

        /**
         * Message sent when streak breaks
         */
        public String streakBreakMessage = "<red>Your payment streak has been reset! Previous best: <gold>%best%</gold> days";
    }

    @Configuration
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StreakReward {
        /**
         * Number of consecutive days required
         */
        public int days;

        /**
         * Display name for this reward tier
         */
        public String name;

        /**
         * Commands to execute when reaching this streak
         * Supports PlaceholderAPI placeholders
         */
        public List<String> commands;
    }
}
