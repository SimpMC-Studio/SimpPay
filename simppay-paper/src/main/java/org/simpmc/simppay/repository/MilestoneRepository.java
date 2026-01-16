package org.simpmc.simppay.repository;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.data.milestone.MilestoneType;
import org.simpmc.simppay.database.Database;
import org.simpmc.simppay.database.entities.MilestoneCompletion;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.service.DatabaseService;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Repository for milestone completion tracking with database persistence.
 * Provides methods to check, create, and query milestone completions.
 */
public class MilestoneRepository {
    private final Dao<MilestoneCompletion, UUID> milestoneDao;

    public MilestoneRepository() {
        Database database = SPPlugin.getService(DatabaseService.class).getDatabase();
        this.milestoneDao = database.getMilestoneDao();
    }

    /**
     * Checks if a player has already completed a specific milestone.
     *
     * @param playerUUID Player UUID
     * @param type       Milestone type (ALL, DAILY, WEEKLY, etc.)
     * @param amount     Milestone amount threshold
     * @return true if milestone was already completed
     */
    public boolean hasPlayerCompleted(UUID playerUUID, MilestoneType type, long amount) {
        try {
            QueryBuilder<MilestoneCompletion, UUID> queryBuilder = milestoneDao.queryBuilder();
            SPPlayer player = SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(playerUUID);
            if (player == null) {
                return false;
            }

            long count = queryBuilder.where()
                    .eq("player_uuid", player)
                    .and()
                    .eq("milestone_type", type)
                    .and()
                    .eq("milestone_amount", amount)
                    .and()
                    .eq("server_wide", false)
                    .countOf();

            return count > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if a server-wide milestone has already been completed.
     *
     * @param type   Milestone type (ALL, DAILY, WEEKLY, etc.)
     * @param amount Milestone amount threshold
     * @return true if milestone was already completed
     */
    public boolean hasServerCompleted(MilestoneType type, long amount) {
        try {
            QueryBuilder<MilestoneCompletion, UUID> queryBuilder = milestoneDao.queryBuilder();

            long count = queryBuilder.where()
                    .eq("milestone_type", type)
                    .and()
                    .eq("milestone_amount", amount)
                    .and()
                    .eq("server_wide", true)
                    .countOf();

            return count > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Marks a player milestone as completed in the database.
     *
     * @param playerUUID Player UUID
     * @param type       Milestone type
     * @param amount     Milestone amount
     */
    public void markPlayerCompleted(UUID playerUUID, MilestoneType type, long amount) {
        try {
            SPPlayer player = SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(playerUUID);
            if (player == null) {
                return;
            }

            MilestoneCompletion completion = new MilestoneCompletion();
            completion.setPlayer(player);
            completion.setMilestoneType(type);
            completion.setMilestoneAmount(amount);
            completion.setCompletedAt(new Date());
            completion.setServerWide(false);

            milestoneDao.create(completion);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Marks a server-wide milestone as completed in the database.
     *
     * @param type   Milestone type
     * @param amount Milestone amount
     */
    public void markServerCompleted(MilestoneType type, long amount) {
        try {
            MilestoneCompletion completion = new MilestoneCompletion();
            completion.setPlayer(null);
            completion.setMilestoneType(type);
            completion.setMilestoneAmount(amount);
            completion.setCompletedAt(new Date());
            completion.setServerWide(true);

            milestoneDao.create(completion);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets all completed milestone amounts for a player of a specific type.
     * Used to filter out already-completed milestones during retroactive checks.
     *
     * @param playerUUID Player UUID
     * @param type       Milestone type
     * @return List of completed milestone amounts
     */
    public List<Long> getPlayerCompletedAmounts(UUID playerUUID, MilestoneType type) {
        try {
            SPPlayer player = SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(playerUUID);
            if (player == null) {
                return List.of();
            }

            QueryBuilder<MilestoneCompletion, UUID> queryBuilder = milestoneDao.queryBuilder();
            List<MilestoneCompletion> completions = queryBuilder.where()
                    .eq("player_uuid", player)
                    .and()
                    .eq("milestone_type", type)
                    .and()
                    .eq("server_wide", false)
                    .query();

            return completions.stream()
                    .map(MilestoneCompletion::getMilestoneAmount)
                    .toList();
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Gets all completed milestone amounts for server-wide milestones of a specific type.
     *
     * @param type Milestone type
     * @return List of completed milestone amounts
     */
    public List<Long> getServerCompletedAmounts(MilestoneType type) {
        try {
            QueryBuilder<MilestoneCompletion, UUID> queryBuilder = milestoneDao.queryBuilder();
            List<MilestoneCompletion> completions = queryBuilder.where()
                    .eq("milestone_type", type)
                    .and()
                    .eq("server_wide", true)
                    .query();

            return completions.stream()
                    .map(MilestoneCompletion::getMilestoneAmount)
                    .toList();
        } catch (SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Clears all milestone completions for time-based milestones.
     * Should be called at the start of a new day/week/month/year.
     *
     * @param type Milestone type to reset (DAILY, WEEKLY, MONTHLY, YEARLY)
     */
    public void resetTimedMilestones(MilestoneType type) {
        if (type == MilestoneType.ALL) {
            return; // Cannot reset ALL milestones
        }

        try {
            milestoneDao.executeRaw("DELETE FROM milestone_completions WHERE milestone_type = ?", type.name());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
