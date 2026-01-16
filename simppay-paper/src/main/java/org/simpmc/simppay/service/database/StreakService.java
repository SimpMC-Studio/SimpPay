package org.simpmc.simppay.service.database;

import com.j256.ormlite.dao.Dao;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.StreakConfig;
import org.simpmc.simppay.database.Database;
import org.simpmc.simppay.database.entities.PlayerStreakPayment;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.util.MessageUtil;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * Phase 5: Streak Service Implementation
 * <p>
 * Manages consecutive daily payment streaks for players.
 * <p>
 * Streak Logic:
 * - Day 1 payment = streak 1
 * - Day 2 payment = streak 2
 * - Gap > 1 day = reset to 1
 * - Same day multiple payments = no change
 */
public class StreakService {
    private final Dao<PlayerStreakPayment, UUID> streakDao;

    public StreakService() {
        Database database = SPPlugin.getService(DatabaseService.class).getDatabase();
        this.streakDao = database.getStreakDao();
    }

    /**
     * Updates player streak when they make a payment.
     * Called from SuccessDatabaseHandlingListener on PaymentSuccessEvent.
     *
     * @param playerUUID Player who made the payment
     */
    public void updateStreak(UUID playerUUID) {
        StreakConfig config = ConfigManager.getInstance().getConfig(StreakConfig.class);

        if (!config.settings.enabled) {
            return;
        }

        try {
            PlayerStreakPayment streak = streakDao.queryForId(playerUUID);
            LocalDate today = LocalDate.now();

            if (streak == null) {
                // First payment ever - create new streak
                streak = createNewStreak(playerUUID, today);
                MessageUtil.debug("[Streak] Created new streak for " + playerUUID + ": Day 1");
            } else {
                LocalDate lastPaymentDate = streak.getLastRechargeDate().toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();

                if (lastPaymentDate.isEqual(today)) {
                    // Same day payment - no change to streak
                    MessageUtil.debug("[Streak] Same day payment for " + playerUUID + ", no streak change");
                    return;
                }

                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastPaymentDate, today);

                if (daysBetween == 1) {
                    // Consecutive day - increment streak
                    streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                    streak.setLastRechargeDate(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));

                    // Update best streak if current exceeds it
                    if (streak.getCurrentStreak() > streak.getBestStreak()) {
                        streak.setBestStreak(streak.getCurrentStreak());
                    }

                    streakDao.update(streak);

                    MessageUtil.debug("[Streak] Incremented streak for " + playerUUID + ": Day " + streak.getCurrentStreak());

                    // Notify player
                    if (config.settings.notifyOnStreak) {
                        notifyPlayer(playerUUID, config.settings.streakIncreaseMessage
                                .replace("%streak%", String.valueOf(streak.getCurrentStreak())));
                    }

                    // Check for streak rewards
                    checkStreakRewards(playerUUID, streak);

                } else {
                    // Streak broken - reset to 1
                    int oldBest = streak.getBestStreak();
                    streak.setCurrentStreak(1);
                    streak.setLastRechargeDate(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));
                    streak.setLastRewardTier(0); // Reset reward tier
                    streak.setClaimedToday(false);

                    streakDao.update(streak);

                    MessageUtil.debug("[Streak] Reset streak for " + playerUUID + ", was " + oldBest + " best");

                    // Notify player of break
                    notifyPlayer(playerUUID, config.settings.streakBreakMessage
                            .replace("%best%", String.valueOf(oldBest)));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            MessageUtil.debug("[Streak] Database error updating streak for " + playerUUID);
        }
    }

    /**
     * Gets player's current streak data.
     *
     * @param playerUUID Player UUID
     * @return PlayerStreakPayment or null if player has no streak
     */
    public PlayerStreakPayment getStreak(UUID playerUUID) {
        try {
            return streakDao.queryForId(playerUUID);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets player's current streak count.
     *
     * @param playerUUID Player UUID
     * @return Current streak (0 if no streak data)
     */
    public int getCurrentStreak(UUID playerUUID) {
        PlayerStreakPayment streak = getStreak(playerUUID);
        return streak != null ? streak.getCurrentStreak() : 0;
    }

    /**
     * Gets player's best streak count.
     *
     * @param playerUUID Player UUID
     * @return Best streak (0 if no streak data)
     */
    public int getBestStreak(UUID playerUUID) {
        PlayerStreakPayment streak = getStreak(playerUUID);
        return streak != null ? streak.getBestStreak() : 0;
    }

    /**
     * Creates a new streak entry for a player.
     */
    private PlayerStreakPayment createNewStreak(UUID playerUUID, LocalDate today) throws SQLException {
        PlayerStreakPayment streak = new PlayerStreakPayment();
        streak.setPlayerUUID(playerUUID);
        streak.setCurrentStreak(1);
        streak.setBestStreak(1);
        streak.setLastRechargeDate(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        streak.setLastRewardTier(0);
        streak.setClaimedToday(false);

        streakDao.create(streak);
        return streak;
    }

    /**
     * Checks if player has reached any streak reward milestones.
     */
    private void checkStreakRewards(UUID playerUUID, PlayerStreakPayment streak) {
        StreakConfig config = ConfigManager.getInstance().getConfig(StreakConfig.class);
        int currentStreak = streak.getCurrentStreak();
        int lastRewardTier = streak.getLastRewardTier();

        for (StreakConfig.StreakReward reward : config.rewards) {
            // Check if player reached this tier and hasn't claimed it yet
            if (currentStreak >= reward.days && reward.days > lastRewardTier) {
                // Award rewards
                Player player = Bukkit.getPlayer(playerUUID);
                for (String command : reward.commands) {
                    String formattedCommand = command.replace("%player_name%", player != null ? player.getName() : "Unknown");
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                }

                // Update last reward tier
                streak.setLastRewardTier(reward.days);
                try {
                    streakDao.update(streak);
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                MessageUtil.debug("[Streak] Awarded " + reward.name + " to " + playerUUID);

                // Notify player
                if (player != null) {
                    notifyPlayer(playerUUID, "<green>Streak Reward: <gold>" + reward.name + "</gold>!");
                }
            }
        }
    }

    /**
     * Sends a notification message to the player.
     */
    private void notifyPlayer(UUID playerUUID, String message) {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null && player.isOnline()) {
            player.sendMessage(MessageUtil.getComponentParsed(message, player));
        }
    }
}
