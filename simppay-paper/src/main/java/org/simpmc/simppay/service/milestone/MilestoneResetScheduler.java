package org.simpmc.simppay.service.milestone;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.data.milestone.MilestoneType;
import org.simpmc.simppay.util.MessageUtil;

import java.time.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles automatic reset of time-based milestones (DAILY, WEEKLY, MONTHLY, YEARLY).
 * Resets are scheduled at appropriate times and tracked to prevent re-triggering.
 */
@Slf4j
@RequiredArgsConstructor
public class MilestoneResetScheduler {

    private final MilestoneCache cache;
    private final Runnable onPlayerMilestonesReset;
    private final Runnable onServerMilestonesReset;

    private static final String DAILY_RESET_TIME = "00:00"; // Midnight
    private static final DayOfWeek WEEKLY_RESET_DAY = DayOfWeek.MONDAY;
    private static final int MONTHLY_RESET_DAY = 1; // First day of month
    private static final MonthDay YEARLY_RESET_DATE = MonthDay.of(Month.JANUARY, 1); // January 1st

    /**
     * Initialize the reset scheduler - registers all reset tasks
     */
    public void setup() {
        MessageUtil.info("Setting up milestone reset scheduler...");

        // Schedule daily reset at midnight
        scheduleDailyReset();

        // Schedule weekly reset every Monday at midnight
        scheduleWeeklyReset();

        // Schedule monthly reset on first day of month at midnight
        scheduleMonthlyReset();

        // Schedule yearly reset on January 1st at midnight
        scheduleYearlyReset();

        MessageUtil.info("Milestone reset scheduler initialized");
    }

    /**
     * Schedule daily milestone resets at midnight
     */
    private void scheduleDailyReset() {
        SPPlugin.getInstance().getServer().getScheduler().scheduleAsyncRepeatingTask(
                SPPlugin.getInstance(),
                () -> {
                    try {
                        LocalDate today = LocalDate.now();
                        String cycleId = today.toString(); // "2024-10-19"

                        // Update reset cycles and trigger reload
                        cache.setServerResetCycle(MilestoneType.DAILY, cycleId);
                        onServerMilestonesReset.run();

                        MessageUtil.info("Triggered daily milestone reset for cycle: " + cycleId);
                        log.info("Daily milestone reset triggered for cycle: {}", cycleId);
                    } catch (Exception e) {
                        log.error("Error during daily milestone reset", e);
                    }
                },
                getTicksUntilNextMidnight(),
                20 * 60 * 60 * 24 // Every 24 hours
        );
    }

    /**
     * Schedule weekly milestone resets every Monday at midnight
     */
    private void scheduleWeeklyReset() {
        SPPlugin.getInstance().getServer().getScheduler().scheduleAsyncRepeatingTask(
                SPPlugin.getInstance(),
                () -> {
                    try {
                        LocalDate today = LocalDate.now();
                        int weekOfYear = today.get(java.time.temporal.WeekFields.ISO.weekOfYear());
                        int year = today.getYear();
                        String cycleId = String.format("week-%d-%d", weekOfYear, year); // "week-42-2024"

                        // Update reset cycles and trigger reload
                        cache.setServerResetCycle(MilestoneType.WEEKLY, cycleId);
                        onServerMilestonesReset.run();

                        MessageUtil.info("Triggered weekly milestone reset for cycle: " + cycleId);
                        log.info("Weekly milestone reset triggered for cycle: {}", cycleId);
                    } catch (Exception e) {
                        log.error("Error during weekly milestone reset", e);
                    }
                },
                getTicksUntilNextWeeklyReset(),
                20 * 60 * 60 * 24 * 7 // Every 7 days
        );
    }

    /**
     * Schedule monthly milestone resets on the first of each month at midnight
     */
    private void scheduleMonthlyReset() {
        SPPlugin.getInstance().getServer().getScheduler().scheduleAsyncRepeatingTask(
                SPPlugin.getInstance(),
                () -> {
                    try {
                        LocalDate today = LocalDate.now();
                        int year = today.getYear();
                        int month = today.getMonthValue();
                        String cycleId = String.format("%d-%02d", year, month); // "2024-10"

                        // Update reset cycles and trigger reload
                        cache.setServerResetCycle(MilestoneType.MONTHLY, cycleId);
                        onServerMilestonesReset.run();

                        MessageUtil.info("Triggered monthly milestone reset for cycle: " + cycleId);
                        log.info("Monthly milestone reset triggered for cycle: {}", cycleId);
                    } catch (Exception e) {
                        log.error("Error during monthly milestone reset", e);
                    }
                },
                getTicksUntilNextMonthlyReset(),
                20 * 60 * 60 * 24 * 28 // Approximate monthly
        );
    }

    /**
     * Schedule yearly milestone resets on January 1st at midnight
     */
    private void scheduleYearlyReset() {
        SPPlugin.getInstance().getServer().getScheduler().scheduleAsyncRepeatingTask(
                SPPlugin.getInstance(),
                () -> {
                    try {
                        LocalDate today = LocalDate.now();
                        int year = today.getYear();
                        String cycleId = String.valueOf(year); // "2024"

                        // Update reset cycles and trigger reload
                        cache.setServerResetCycle(MilestoneType.YEARLY, cycleId);
                        onServerMilestonesReset.run();

                        MessageUtil.info("Triggered yearly milestone reset for cycle: " + cycleId);
                        log.info("Yearly milestone reset triggered for cycle: {}", cycleId);
                    } catch (Exception e) {
                        log.error("Error during yearly milestone reset", e);
                    }
                },
                getTicksUntilNextYearlyReset(),
                20 * 60 * 60 * 24 * 365 // Approximate yearly
        );
    }

    /**
     * Calculate ticks until next midnight
     */
    private long getTicksUntilNextMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        Duration duration = Duration.between(now, midnight);
        return TimeUnit.MILLISECONDS.toSeconds(duration.toMillis()) * 20 / 1000 + 1;
    }

    /**
     * Calculate ticks until next Monday at midnight
     */
    private long getTicksUntilNextWeeklyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMonday = now.toLocalDate()
                .with(java.time.temporal.TemporalAdjusters.next(WEEKLY_RESET_DAY))
                .atStartOfDay();

        // If today is Monday and it's past midnight, set to next Monday
        if (now.getDayOfWeek() == WEEKLY_RESET_DAY && now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) {
            nextMonday = nextMonday.plusDays(7);
        }

        Duration duration = Duration.between(now, nextMonday);
        return TimeUnit.MILLISECONDS.toSeconds(duration.toMillis()) * 20 / 1000 + 1;
    }

    /**
     * Calculate ticks until next month at midnight on the first day
     */
    private long getTicksUntilNextMonthlyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextFirst = now.toLocalDate()
                .withDayOfMonth(MONTHLY_RESET_DAY)
                .plusMonths(1)
                .atStartOfDay();

        // If today is the first and it's past midnight, we already reset, use next month
        if (now.getDayOfMonth() > MONTHLY_RESET_DAY) {
            nextFirst = now.toLocalDate()
                    .withDayOfMonth(MONTHLY_RESET_DAY)
                    .plusMonths(1)
                    .atStartOfDay();
        } else if (now.getDayOfMonth() == MONTHLY_RESET_DAY && now.toLocalTime().isAfter(LocalTime.MIDNIGHT)) {
            nextFirst = now.toLocalDate()
                    .withDayOfMonth(MONTHLY_RESET_DAY)
                    .plusMonths(1)
                    .atStartOfDay();
        } else {
            nextFirst = now.toLocalDate()
                    .withDayOfMonth(MONTHLY_RESET_DAY)
                    .atStartOfDay();
        }

        Duration duration = Duration.between(now, nextFirst);
        return TimeUnit.MILLISECONDS.toSeconds(duration.toMillis()) * 20 / 1000 + 1;
    }

    /**
     * Calculate ticks until next January 1st at midnight
     */
    private long getTicksUntilNextYearlyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextJan1 = now.toLocalDate()
                .withMonth(Month.JANUARY.getValue())
                .withDayOfMonth(1)
                .plusYears(1)
                .atStartOfDay();

        // If we're before Jan 1 this year, reset to this year's Jan 1
        if (now.getMonthValue() == Month.JANUARY.getValue() && now.getDayOfMonth() < 1) {
            nextJan1 = now.toLocalDate()
                    .withMonth(Month.JANUARY.getValue())
                    .withDayOfMonth(1)
                    .atStartOfDay();
        }

        Duration duration = Duration.between(now, nextJan1);
        return TimeUnit.MILLISECONDS.toSeconds(duration.toMillis()) * 20 / 1000 + 1;
    }

    /**
     * Get the reset cycle ID for a given milestone type (current cycle)
     */
    public String getCurrentCycleId(MilestoneType type) {
        LocalDate today = LocalDate.now();

        return switch (type) {
            case DAILY -> today.toString();
            case WEEKLY -> {
                int weekOfYear = today.get(java.time.temporal.WeekFields.ISO.weekOfYear());
                yield String.format("week-%d-%d", weekOfYear, today.getYear());
            }
            case MONTHLY -> String.format("%d-%02d", today.getYear(), today.getMonthValue());
            case YEARLY -> String.valueOf(today.getYear());
            case ALL -> "all-time";
        };
    }
}
