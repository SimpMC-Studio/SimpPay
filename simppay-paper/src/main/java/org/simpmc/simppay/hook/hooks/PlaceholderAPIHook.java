package org.simpmc.simppay.hook.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.CoinsConfig;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.service.cache.CacheDataService;
import org.simpmc.simppay.service.cache.LeaderboardEntry;
import org.simpmc.simppay.service.cache.LeaderboardType;
import org.simpmc.simppay.service.database.StreakService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PlaceholderAPI Hook - Phase 2.2 Expansion
 * <p>
 * Expanded Placeholders:
 * - Player timed values: daily, weekly, monthly, yearly (+ formatted)
 * - Leaderboard: top_daily/weekly/monthly/alltime_{rank}_name/value
 * - Streak: streak_current, streak_best (Phase 5)
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {
    // Regex pattern for leaderboard placeholders: top_(daily|weekly|monthly|alltime)_(\d+)_(name|value)
    private static final Pattern LEADERBOARD_PATTERN = Pattern.compile("^top_(daily|weekly|monthly|alltime)_(\\d+)_(name|value)$");
    private final SPPlugin plugin;

    public PlaceholderAPIHook(SPPlugin plugin) {
        this.plugin = plugin;
        register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "simppay";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Typical";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0"; // Phase 2.2 version
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        CacheDataService cacheDataService = SPPlugin.getService(CacheDataService.class);

        // ===== SERVER-WIDE PLACEHOLDERS (work without player) =====

        // %simppay_server_total%
        if (identifier.equalsIgnoreCase("server_total")) {
            return cacheDataService.getServerTotalValue().toString();
        }
        // %simppay_server_total_formatted%
        if (identifier.equalsIgnoreCase("server_total_formatted")) {
            return String.format("%,d", cacheDataService.getServerTotalValue().get());
        }
        // %simppay_server_daily_formatted%
        if (identifier.equalsIgnoreCase("server_daily_formatted")) {
            return String.format("%,d", cacheDataService.getServerDailyTotalValue().get());
        }
        // %simppay_server_weekly_formatted%
        if (identifier.equalsIgnoreCase("server_weekly_formatted")) {
            return String.format("%,d", cacheDataService.getServerWeeklyTotalValue().get());
        }
        // %simppay_server_monthly_formatted%
        if (identifier.equalsIgnoreCase("server_monthly_formatted")) {
            return String.format("%,d", cacheDataService.getServerMonthlyTotalValue().get());
        }
        // %simppay_server_yearly_formatted%
        if (identifier.equalsIgnoreCase("server_yearly_formatted")) {
            return String.format("%,d", cacheDataService.getServerYearlyTotalValue().get());
        }
        // %simppay_bank_total_formatted%
        if (identifier.equalsIgnoreCase("bank_total_formatted")) {
            return String.format("%,d", cacheDataService.getBankTotalValue().get());
        }
        // %simppay_card_total_formatted%
        if (identifier.equalsIgnoreCase("card_total_formatted")) {
            return String.format("%,d", cacheDataService.getCardTotalValue().get());
        }

        // ===== LEADERBOARD PLACEHOLDERS (work without player) =====
        // Pattern: %simppay_top_daily_1_name%, %simppay_top_weekly_5_value%, etc.
        Matcher leaderboardMatcher = LEADERBOARD_PATTERN.matcher(identifier.toLowerCase());
        if (leaderboardMatcher.matches()) {
            String typeStr = leaderboardMatcher.group(1);     // daily/weekly/monthly/alltime
            int rank = Integer.parseInt(leaderboardMatcher.group(2));  // 1, 2, 3...
            String field = leaderboardMatcher.group(3);        // name/value

            return handleLeaderboardPlaceholder(cacheDataService, typeStr, rank, field);
        }

        // ===== PLAYER-SPECIFIC PLACEHOLDERS (require player) =====

        if (player == null) {
            return null;
        }

        UUID uuid = player.getUniqueId();

        // %simppay_total%
        if (identifier.equalsIgnoreCase("total")) {
            return String.valueOf(cacheDataService.getOrLoadPlayerTotal(uuid));
        }
        // %simppay_total_formatted%
        if (identifier.equalsIgnoreCase("total_formatted")) {
            return String.format("%,d", cacheDataService.getOrLoadPlayerTotal(uuid));
        }

        // ===== PLAYER TIMED VALUES (Phase 2.2 NEW) =====

        // %simppay_daily%
        if (identifier.equalsIgnoreCase("daily")) {
            return getPlayerTimedValue(cacheDataService, uuid, "daily");
        }
        // %simppay_daily_formatted%
        if (identifier.equalsIgnoreCase("daily_formatted")) {
            return formatNumber(getPlayerTimedValue(cacheDataService, uuid, "daily"));
        }

        // %simppay_weekly%
        if (identifier.equalsIgnoreCase("weekly")) {
            return getPlayerTimedValue(cacheDataService, uuid, "weekly");
        }
        // %simppay_weekly_formatted%
        if (identifier.equalsIgnoreCase("weekly_formatted")) {
            return formatNumber(getPlayerTimedValue(cacheDataService, uuid, "weekly"));
        }

        // %simppay_monthly%
        if (identifier.equalsIgnoreCase("monthly")) {
            return getPlayerTimedValue(cacheDataService, uuid, "monthly");
        }
        // %simppay_monthly_formatted%
        if (identifier.equalsIgnoreCase("monthly_formatted")) {
            return formatNumber(getPlayerTimedValue(cacheDataService, uuid, "monthly"));
        }

        // %simppay_yearly%
        if (identifier.equalsIgnoreCase("yearly")) {
            return getPlayerTimedValue(cacheDataService, uuid, "yearly");
        }
        // %simppay_yearly_formatted%
        if (identifier.equalsIgnoreCase("yearly_formatted")) {
            return formatNumber(getPlayerTimedValue(cacheDataService, uuid, "yearly"));
        }

        // ===== STREAK PLACEHOLDERS (Phase 5) =====

        // %simppay_streak_current%
        if (identifier.equalsIgnoreCase("streak_current")) {
            StreakService streakService = new StreakService();
            return String.valueOf(streakService.getCurrentStreak(uuid));
        }
        // %simppay_streak_best%
        if (identifier.equalsIgnoreCase("streak_best")) {
            StreakService streakService = new StreakService();
            return String.valueOf(streakService.getBestStreak(uuid));
        }
        // %simppay_end_promo%
        if (identifier.equalsIgnoreCase("end_promo")) {
            CoinsConfig coinsConfig = ConfigManager.getInstance().getConfig(CoinsConfig.class);
            MessageConfig messageConfig = ConfigManager.getInstance().getConfig(MessageConfig.class);

            // check promo time
            try {
                LocalDateTime promoEndTime = LocalDateTime.parse(coinsConfig.promoEndTimeString, coinsConfig.formatter);
                if (promoEndTime.isBefore(LocalDateTime.now())) {
                    return messageConfig.noPromo;
                } else {
                    return coinsConfig.promoEndTimeString;
                }
            } catch (Exception e) {
                // Parse lỗi thời gian -> coi như không có khuyến mãi
                return messageConfig.noPromo;
            }
        }
        return null;
    }

    /**
     * Handles leaderboard placeholder requests
     *
     * @param cacheService Cache service instance
     * @param typeStr      Type string (daily, weekly, monthly, alltime)
     * @param rank         Rank position (1-based)
     * @param field        Field to return (name or value)
     * @return Placeholder value or "N/A" if not found
     */
    private String handleLeaderboardPlaceholder(CacheDataService cacheService, String typeStr, int rank, String field) {
        // Convert string to enum
        LeaderboardType type = switch (typeStr) {
            case "daily" -> LeaderboardType.DAILY;
            case "weekly" -> LeaderboardType.WEEKLY;
            case "monthly" -> LeaderboardType.MONTHLY;
            case "alltime" -> LeaderboardType.ALLTIME;
            default -> null;
        };

        if (type == null) {
            return "N/A";
        }

        // Get leaderboard (cached with 1-minute TTL)
        List<LeaderboardEntry> leaderboard = cacheService.getLeaderboard(type, rank);

        // Check if rank exists
        if (leaderboard.size() < rank) {
            return "N/A";
        }

        LeaderboardEntry entry = leaderboard.get(rank - 1); // Convert 1-based to 0-based

        // Return requested field
        if (field.equals("name")) {
            return entry.getPlayerName();
        } else if (field.equals("value")) {
            return String.format("%,d", entry.getAmount());
        }

        return "N/A";
    }

    /**
     * Gets player timed value from cache
     *
     * @param cacheService Cache service instance
     * @param uuid         Player UUID
     * @param period       Period type (daily, weekly, monthly, yearly)
     * @return Value as string or "0" if not found
     */
    private String getPlayerTimedValue(CacheDataService cacheService, UUID uuid, String period) {
        AtomicLong value = switch (period) {
            case "daily" -> cacheService.getPlayerDailyTotalValue().get(uuid);
            case "weekly" -> cacheService.getPlayerWeeklyTotalValue().get(uuid);
            case "monthly" -> cacheService.getPlayerMonthlyTotalValue().get(uuid);
            case "yearly" -> cacheService.getPlayerYearlyTotalValue().get(uuid);
            default -> null;
        };

        return value != null ? String.valueOf(value.get()) : "0";
    }

    /**
     * Formats a number string with thousand separators
     */
    private String formatNumber(String numberStr) {
        try {
            long number = Long.parseLong(numberStr);
            return String.format("%,d", number);
        } catch (NumberFormatException e) {
            return numberStr; // Return as-is if not a valid number
        }
    }
}
