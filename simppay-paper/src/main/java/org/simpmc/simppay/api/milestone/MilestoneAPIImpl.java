package org.simpmc.simppay.api.milestone;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.data.milestone.MilestoneType;
import org.simpmc.simppay.service.DatabaseService;
import org.simpmc.simppay.service.MilestoneService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of MilestoneAPI for external plugin access
 */
@RequiredArgsConstructor
public class MilestoneAPIImpl implements MilestoneAPI {

    private final MilestoneService milestoneService;
    private final DatabaseService databaseService;

    @Override
    public List<MilestoneConfig> getPlayerMilestones(UUID playerUUID) {
        return milestoneService.getPlayerMilestones(playerUUID);
    }

    @Override
    public List<MilestoneConfig> getServerMilestones() {
        return milestoneService.getServerMilestones();
    }

    @Override
    public long getPlayerTotalAmount(UUID playerUUID) {
        try {
            return databaseService.getPaymentLogService().getPlayerTotalAmount(
                    databaseService.getPlayerService().findByUuid(playerUUID)
            ).longValue();
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public long getPlayerAmountByType(UUID playerUUID, MilestoneType type) {
        try {
            var player = databaseService.getPlayerService().findByUuid(playerUUID);
            return switch (type) {
                case ALL -> databaseService.getPaymentLogService().getPlayerTotalAmount(player).longValue();
                case DAILY -> databaseService.getPaymentLogService().getPlayerDailyAmount(player);
                case WEEKLY -> databaseService.getPaymentLogService().getPlayerWeeklyAmount(player);
                case MONTHLY -> databaseService.getPaymentLogService().getPlayerMonthlyAmount(player);
                case YEARLY -> databaseService.getPaymentLogService().getPlayerYearlyAmount(player);
            };
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public long getServerTotalAmount() {
        return databaseService.getPaymentLogService().getEntireServerAmount();
    }

    @Override
    public long getServerAmountByType(MilestoneType type) {
        try {
            return switch (type) {
                case ALL -> databaseService.getPaymentLogService().getEntireServerAmount();
                case DAILY -> databaseService.getPaymentLogService().getEntireServerDailyAmount();
                case WEEKLY -> databaseService.getPaymentLogService().getEntireServerWeeklyAmount();
                case MONTHLY -> databaseService.getPaymentLogService().getEntireServerMonthlyAmount();
                case YEARLY -> databaseService.getPaymentLogService().getEntireServerYearlyAmount();
            };
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public boolean isPlayerMilestoneCompleted(UUID playerUUID, String milestoneId) {
        try {
            // Query database for milestone completion
            // For now, return false (would need to query MilestoneCompletion table)
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isServerMilestoneCompleted(String milestoneId) {
        try {
            // Query database for server milestone completion
            // For now, return false (would need to query ServerMilestoneCompletion table)
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void sendProgressUpdate(Player player, MilestoneConfig milestone, double progressPercent) {
        milestoneService.getNotificationService().sendProgressUpdate(player, milestone, progressPercent);
    }

    @Override
    public void notifyPlayerMilestoneComplete(Player player, MilestoneConfig milestone) {
        milestoneService.getNotificationService().notifyPlayerMilestoneComplete(player, milestone);
    }

    @Override
    public void notifyServerMilestoneComplete(MilestoneConfig milestone) {
        milestoneService.getNotificationService().notifyServerMilestoneComplete(milestone);
    }

    @Override
    public Map<String, Object> getCacheStats() {
        return milestoneService.getCacheStats();
    }

    /**
     * Get the API instance for external plugins
     */
    public static MilestoneAPI getInstance() {
        try {
            MilestoneService milestoneService = SPPlugin.getService(MilestoneService.class);
            DatabaseService databaseService = SPPlugin.getService(DatabaseService.class);
            return new MilestoneAPIImpl(milestoneService, databaseService);
        } catch (Exception e) {
            throw new RuntimeException("Milestone API not available", e);
        }
    }
}
