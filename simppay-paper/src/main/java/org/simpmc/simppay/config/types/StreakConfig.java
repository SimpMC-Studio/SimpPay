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
            new StreakReward(3, "Chuỗi 3 ngày", List.of("give %player_name% diamond 3")),
            new StreakReward(7, "Chuỗi 7 ngày", List.of("give %player_name% diamond 7")),
            new StreakReward(14, "Chuỗi 14 ngày", List.of("give %player_name% diamond 14")),
            new StreakReward(30, "Chuỗi 30 ngày", List.of("give %player_name% diamond 30"))
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
        public String streakIncreaseMessage = "<green>Chuỗi nạp thẻ của bạn hiện tại là <gold>%streak%</gold> ngày!";

        /**
         * Message sent when streak breaks
         */
        public String streakBreakMessage = "<red>Chuỗi nạp thẻ của bạn đã bị reset! Kỷ lục tốt nhất: <gold>%best%</gold> ngày";
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
