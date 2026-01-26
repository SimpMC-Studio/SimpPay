package org.simpmc.simppay.service;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MainConfig;
import org.simpmc.simppay.config.types.MilestonesPlayerConfig;
import org.simpmc.simppay.config.types.MilestonesServerConfig;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.config.types.data.MilestoneEntry;
import org.simpmc.simppay.data.milestone.MilestoneType;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.repository.MilestoneRepository;
import org.simpmc.simppay.service.database.PaymentLogService;
import org.simpmc.simppay.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MilestoneService implements IService {
    // UNIFIED BOSSBARS (one per player, shows both player and server milestones)
    public ConcurrentHashMap<UUID, BossBar> unifiedBossBar = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Integer> cycleIndex = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, List<MilestoneDisplayData>> activeMilestones = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, Boolean> cyclingActive = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, List<MilestoneConfig>> currentMilestones = new ConcurrentHashMap<>();

    // Countdown timer tracking (elapsed ticks since cycle start)
    public ConcurrentHashMap<UUID, Integer> cycleElapsedTicks = new ConcurrentHashMap<>();

    // Current amounts cache for smooth progress updates
    public ConcurrentHashMap<UUID, Double> currentAmountCache = new ConcurrentHashMap<>();

    // Player-specific bossbar visibility toggle
    public ConcurrentHashMap<UUID, Boolean> bossbarHidden = new ConcurrentHashMap<>();

    private MilestoneRepository milestoneRepository;

    /**
     * Gets the cycle duration in ticks from config.
     */
    private int getCycleDurationTicks() {
        MainConfig config = ConfigManager.getInstance().getConfig(MainConfig.class);
        return config.bossbar.cycleDurationSeconds * 20;
    }

    /**
     * Gets the update frequency in ticks from config.
     */
    private int getUpdateFrequencyTicks() {
        MainConfig config = ConfigManager.getInstance().getConfig(MainConfig.class);
        return config.bossbar.updateFrequencyTicks;
    }

    /**
     * Builds BossBar title: "[Player/Server] Mốc nạp {Type}: {current} / {target}"
     * Uses short-form currency formatting (k, tr, tỷ).
     */
    private String buildBossBarTitle(MilestoneType type, double current, double target, boolean isServerMilestone) {
        String typeDisplay = switch (type) {
            case ALL -> "Toàn Thời Gian";
            case DAILY -> "Daily";
            case WEEKLY -> "Weekly";
            case MONTHLY -> "Monthly";
            case YEARLY -> "Yearly";
        };

        String prefix = isServerMilestone ? "[Server]" : "[Cá Nhân]";

        return String.format("%s Mốc nạp %s: %s / %s",
                prefix,
                typeDisplay,
                MessageUtil.formatShortCurrency(current),
                MessageUtil.formatShortCurrency(target)
        );
    }

    /**
     * Calculates progress value (0.0 to 1.0) for BossBar.
     */
    private float calculateProgress(double current, double target) {
        float progress = (float) (current / target);
        return Math.min(1.0f, Math.max(0.0f, progress));
    }

    /**
     * Updates BossBar progress with countdown timer.
     * Progress bar fills from 0% to 100% over the cycle duration.
     * Async-safe in PaperMC - Adventure API bossbars are thread-safe.
     */
    private void updateBossBarProgress(UUID uuid, int elapsedTicks) {
        BossBar bossBar = unifiedBossBar.get(uuid);
        if (bossBar == null) {
            return;
        }

        List<MilestoneDisplayData> milestones = activeMilestones.get(uuid);
        if (milestones == null || milestones.isEmpty()) {
            return;
        }

        int currentIndex = cycleIndex.getOrDefault(uuid, 0);
        if (currentIndex >= milestones.size()) {
            return;
        }

        MilestoneDisplayData milestone = milestones.get(currentIndex);
        Double cachedAmount = currentAmountCache.get(uuid);

        if (cachedAmount != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                // Async-safe: Adventure API bossbars are thread-safe in PaperMC
                bossBar.name(MessageUtil.getComponentParsed(
                        buildBossBarTitle(milestone.type, cachedAmount, milestone.targetAmount, milestone.isServerMilestone),
                        null
                ));
                // Use countdown progress for smooth refresh countdown visualization
                float countdownProgress = (float) elapsedTicks / getCycleDurationTicks();
                bossBar.progress(Math.min(1.0f, Math.max(0.0f, countdownProgress)));
            }
        }
    }

    /**
     * Cycles to next active milestone (player or server).
     * Called every 15 seconds by scheduled task.
     */
    private void cycleMilestone(UUID uuid) {
        // Check if cycling is still active
        if (!cyclingActive.getOrDefault(uuid, false)) {
            return;
        }

        List<MilestoneDisplayData> milestones = activeMilestones.get(uuid);

        if (milestones == null || milestones.isEmpty()) {
            removeBossBar(uuid);
            return;
        }

        int currentIndex = cycleIndex.getOrDefault(uuid, 0);
        if (currentIndex >= milestones.size()) {
            currentIndex = 0;
            cycleIndex.put(uuid, 0);
        }

        MilestoneDisplayData milestone = milestones.get(currentIndex);

        // Fetch current amount async
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();
            SPPlayer player = SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(uuid);

            if (player == null) {
                return;
            }

            double currentAmount;
            if (milestone.isServerMilestone) {
                // Fetch server-wide total
                currentAmount = switch (milestone.type) {
                    case ALL -> paymentLogService.getEntireServerAmount();
                    case DAILY -> paymentLogService.getEntireServerDailyAmount();
                    case WEEKLY -> paymentLogService.getEntireServerWeeklyAmount();
                    case MONTHLY -> paymentLogService.getEntireServerMonthlyAmount();
                    case YEARLY -> paymentLogService.getEntireServerYearlyAmount();
                };
            } else {
                // Fetch player total
                currentAmount = switch (milestone.type) {
                    case ALL -> paymentLogService.getPlayerTotalAmount(player);
                    case DAILY -> paymentLogService.getPlayerDailyAmount(player);
                    case WEEKLY -> paymentLogService.getPlayerWeeklyAmount(player);
                    case MONTHLY -> paymentLogService.getPlayerMonthlyAmount(player);
                    case YEARLY -> paymentLogService.getPlayerYearlyAmount(player);
                };
            }

            // Cache the amount for smooth updates
            currentAmountCache.put(uuid, currentAmount);

            // Update BossBar asynchronously (thread-safe in PaperMC)
            Player onlinePlayer = Bukkit.getPlayer(uuid);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                updateBossBarDisplay(uuid, milestone, currentAmount);
            }
        });

        // Advance to next milestone
        int nextIndex = (currentIndex + 1) % milestones.size();
        cycleIndex.put(uuid, nextIndex);
    }

    /**
     * Updates the visual display of unified BossBar.
     * Progress bar starts at 0% on cycle start (for countdown visualization).
     * Async-safe in PaperMC - Adventure API bossbars are thread-safe.
     */
    private void updateBossBarDisplay(UUID uuid, MilestoneDisplayData milestone, double currentAmount) {
        // Check if player has hidden bossbar
        if (isBossBarHidden(uuid)) {
            return;
        }

        BossBar bossBar = unifiedBossBar.get(uuid);

        // Cache the current amount for smooth updates
        currentAmountCache.put(uuid, currentAmount);

        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        if (bossBar == null) {
            // Create new bossbar (async-safe in PaperMC)
            bossBar = BossBar.bossBar(
                    MessageUtil.getComponentParsed(buildBossBarTitle(milestone.type, currentAmount, milestone.targetAmount, milestone.isServerMilestone), null),
                    0.0f,  // Start at 0% for countdown
                    milestone.color,
                    milestone.style
            );
            unifiedBossBar.put(uuid, bossBar);
            bossBar.addViewer(player);
        } else {
            // Update existing bossbar (async-safe in PaperMC)
            bossBar.name(MessageUtil.getComponentParsed(buildBossBarTitle(milestone.type, currentAmount, milestone.targetAmount, milestone.isServerMilestone), null));
            bossBar.progress(0.0f);  // Reset to 0% for countdown
            bossBar.color(milestone.color);
            bossBar.overlay(milestone.style);
        }
    }

    /**
     * Starts cycling task with smooth progress updates.
     */
    private void startCyclingTask(UUID uuid) {
        stopCyclingTask(uuid);

        cyclingActive.put(uuid, true);
        cycleElapsedTicks.put(uuid, 0);  // Reset countdown

        int updateFrequency = getUpdateFrequencyTicks();

        SPPlugin.getInstance().getFoliaLib().getScheduler().runTimerAsync(
                task -> {
                    if (!cyclingActive.getOrDefault(uuid, false)) {
                        task.cancel();
                        cycleElapsedTicks.remove(uuid);
                        return;
                    }

                    int elapsed = cycleElapsedTicks.getOrDefault(uuid, 0);
                    int cycleDuration = getCycleDurationTicks();

                    if (elapsed >= cycleDuration) {
                        // Cycle duration passed - cycle to next milestone
                        cycleElapsedTicks.put(uuid, 0);
                        cycleMilestone(uuid);
                    } else {
                        // Update progress smoothly
                        updateBossBarProgress(uuid, elapsed);
                        cycleElapsedTicks.put(uuid, elapsed + updateFrequency);
                    }
                },
                0,              // Run immediately
                updateFrequency // Configurable update frequency
        );
    }

    /**
     * Stops cycling task.
     */
    public void stopCyclingTask(UUID uuid) {
        cyclingActive.put(uuid, false);
    }

    /**
     * Removes BossBar when no active milestones.
     */
    private void removeBossBar(UUID uuid) {
        BossBar bossBar = unifiedBossBar.remove(uuid);
        if (bossBar != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                bossBar.removeViewer(player);
            }
        }
        stopCyclingTask(uuid);
        currentAmountCache.remove(uuid);
    }

    @Override
    public void setup() {
        milestoneRepository = new MilestoneRepository();

        // Load unified milestones for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            loadUnifiedMilestonesForPlayer(player.getUniqueId());
        }
    }

    @Override
    public void shutdown() {
        // Stop all cycling tasks
        for (UUID uuid : new ArrayList<>(cyclingActive.keySet())) {
            stopCyclingTask(uuid);
        }

        // Remove all BossBars
        for (UUID uuid : new ArrayList<>(unifiedBossBar.keySet())) {
            removeBossBar(uuid);
        }

        // Clear all caches
        unifiedBossBar.clear();
        cycleIndex.clear();
        activeMilestones.clear();
        cyclingActive.clear();
        currentMilestones.clear();
        cycleElapsedTicks.clear();
        currentAmountCache.clear();
        bossbarHidden.clear();
    }

    /**
     * Loads both player and server milestones into a unified cycling BossBar.
     */
    public void loadUnifiedMilestonesForPlayer(UUID uuid) {
        currentMilestones.remove(uuid);
        activeMilestones.remove(uuid);

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();
            SPPlayer player = SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(uuid);

            if (player == null) {
                return;
            }

            MilestonesPlayerConfig playerConfig = ConfigManager.getInstance().getConfig(MilestonesPlayerConfig.class);
            MilestonesServerConfig serverConfig = ConfigManager.getInstance().getConfig(MilestonesServerConfig.class);
            MessageUtil.debug("Loading Unified Milestones for " + player.getName());

            currentMilestones.putIfAbsent(uuid, new ArrayList<>());
            List<MilestoneDisplayData> displayList = new ArrayList<>();

            // Load player milestones
            for (MilestoneType type : MilestoneType.values()) {
                List<MilestoneEntry> entries = switch (type) {
                    case ALL -> playerConfig.milestones.alltime;
                    case DAILY -> playerConfig.milestones.daily;
                    case WEEKLY -> playerConfig.milestones.weekly;
                    case MONTHLY -> playerConfig.milestones.monthly;
                    case YEARLY -> playerConfig.milestones.yearly;
                };

                double playerBal = switch (type) {
                    case ALL -> paymentLogService.getPlayerTotalAmount(player);
                    case DAILY -> paymentLogService.getPlayerDailyAmount(player);
                    case WEEKLY -> paymentLogService.getPlayerWeeklyAmount(player);
                    case MONTHLY -> paymentLogService.getPlayerMonthlyAmount(player);
                    case YEARLY -> paymentLogService.getPlayerYearlyAmount(player);
                };

                // Find the NEXT (minimum) uncompleted milestone for this type
                MilestoneEntry nextMilestone = null;
                for (MilestoneEntry entry : entries) {
                    if (entry.amount > playerBal) {
                        if (nextMilestone == null || entry.amount < nextMilestone.amount) {
                            nextMilestone = entry;
                        }
                    }
                }

                // Add all uncompleted milestones to currentMilestones (for retroactive checking)
                for (MilestoneEntry entry : entries) {
                    MilestoneConfig config = entryToConfig(entry, type);
                    if (config.amount > playerBal) {
                        currentMilestones.get(uuid).add(config);
                        MessageUtil.debug("Loaded Player Milestone: " + entry.name + " (" + config.amount + ")");
                    }
                }

                // Only add the NEXT milestone to display list (for BossBar)
                if (nextMilestone != null) {
                    MilestoneConfig config = entryToConfig(nextMilestone, type);
                    if (config.bossbar.enabled) {
                        displayList.add(new MilestoneDisplayData(
                                type,
                                config.amount,
                                config.bossbar.color,
                                config.bossbar.style,
                                false  // isServerMilestone = false
                        ));
                        MessageUtil.debug("Added Player BossBar Milestone: " + nextMilestone.name + " (" + config.amount + ")");
                    }
                }
            }

            // Load server milestones
            for (MilestoneType type : MilestoneType.values()) {
                List<MilestoneEntry> entries = switch (type) {
                    case ALL -> serverConfig.milestones.alltime;
                    case DAILY -> serverConfig.milestones.daily;
                    case WEEKLY -> serverConfig.milestones.weekly;
                    case MONTHLY -> serverConfig.milestones.monthly;
                    case YEARLY -> serverConfig.milestones.yearly;
                };

                double serverBal = switch (type) {
                    case ALL -> paymentLogService.getEntireServerAmount();
                    case DAILY -> paymentLogService.getEntireServerDailyAmount();
                    case WEEKLY -> paymentLogService.getEntireServerWeeklyAmount();
                    case MONTHLY -> paymentLogService.getEntireServerMonthlyAmount();
                    case YEARLY -> paymentLogService.getEntireServerYearlyAmount();
                };

                // Find the NEXT (minimum) uncompleted milestone for this type
                MilestoneEntry nextMilestone = null;
                for (MilestoneEntry entry : entries) {
                    if (entry.amount > serverBal) {
                        if (nextMilestone == null || entry.amount < nextMilestone.amount) {
                            nextMilestone = entry;
                        }
                    }
                }

                // Add all uncompleted milestones to currentMilestones (for retroactive checking)
                for (MilestoneEntry entry : entries) {
                    MilestoneConfig config = entryToConfig(entry, type);
                    if (config.amount > serverBal) {
                        currentMilestones.get(uuid).add(config);
                        MessageUtil.debug("Loaded Server Milestone: " + entry.name + " (" + config.amount + ")");
                    }
                }

                // Only add the NEXT milestone to display list (for BossBar)
                if (nextMilestone != null) {
                    MilestoneConfig config = entryToConfig(nextMilestone, type);
                    if (config.bossbar.enabled) {
                        displayList.add(new MilestoneDisplayData(
                                type,
                                config.amount,
                                config.bossbar.color,
                                config.bossbar.style,
                                true  // isServerMilestone = true
                        ));
                        MessageUtil.debug("Added Server BossBar Milestone: " + nextMilestone.name + " (" + config.amount + ")");
                    }
                }
            }

            activeMilestones.put(uuid, displayList);
            cycleIndex.put(uuid, 0);

            // Start cycling on main thread
            SPPlugin.getInstance().getFoliaLib().getScheduler().runNextTick(t -> {
                Player onlinePlayer = Bukkit.getPlayer(uuid);
                if (onlinePlayer != null && onlinePlayer.isOnline() && !displayList.isEmpty() && !isBossBarHidden(uuid)) {
                    startCyclingTask(uuid);
                } else if (displayList.isEmpty()) {
                    MessageUtil.debug("No active milestones for " + player.getName());
                } else if (isBossBarHidden(uuid)) {
                    MessageUtil.debug("BossBar hidden for " + player.getName());
                }
            });
        });
    }

    /**
     * Refreshes unified milestones after completion.
     */
    public void refreshUnifiedMilestones(UUID uuid) {
        loadUnifiedMilestonesForPlayer(uuid);
    }

    /**
     * Gets all uncompleted player milestones that should be checked for retroactive completion.
     * Phase 3.2: Updated to use new MilestonesPlayerConfig format.
     *
     * @param playerUUID Player UUID
     * @param type       Milestone type to check
     * @return List of uncompleted milestones that can potentially be completed
     */
    public List<MilestoneConfig> getUncompletedPlayerMilestones(UUID playerUUID, MilestoneType type) {
        MilestonesPlayerConfig milestonesConfig = ConfigManager.getInstance().getConfig(MilestonesPlayerConfig.class);

        // Get milestone entries for this type and convert to MilestoneConfig
        List<MilestoneEntry> entries = switch (type) {
            case ALL -> milestonesConfig.milestones.alltime;
            case DAILY -> milestonesConfig.milestones.daily;
            case WEEKLY -> milestonesConfig.milestones.weekly;
            case MONTHLY -> milestonesConfig.milestones.monthly;
            case YEARLY -> milestonesConfig.milestones.yearly;
            default -> new ArrayList<>();
        };

        List<MilestoneConfig> allMilestones = entries.stream()
                .map(entry -> entryToConfig(entry, type))
                .collect(Collectors.toList());

        List<Long> completedAmounts = milestoneRepository.getPlayerCompletedAmounts(playerUUID, type);

        return allMilestones.stream()
                .filter(config -> !completedAmounts.contains((long) config.amount))
                .collect(Collectors.toList());
    }

    /**
     * Gets all uncompleted server milestones that should be checked for retroactive completion.
     * Phase 3.2: Updated to use new MilestonesServerConfig format.
     *
     * @param type Milestone type to check
     * @return List of uncompleted server milestones
     */
    public List<MilestoneConfig> getUncompletedServerMilestones(MilestoneType type) {
        MilestonesServerConfig milestonesConfig = ConfigManager.getInstance().getConfig(MilestonesServerConfig.class);

        // Get milestone entries for this type and convert to MilestoneConfig
        List<MilestoneEntry> entries = switch (type) {
            case ALL -> milestonesConfig.milestones.alltime;
            case DAILY -> milestonesConfig.milestones.daily;
            case WEEKLY -> milestonesConfig.milestones.weekly;
            case MONTHLY -> milestonesConfig.milestones.monthly;
            case YEARLY -> milestonesConfig.milestones.yearly;
            default -> new ArrayList<>();
        };

        List<MilestoneConfig> allMilestones = entries.stream()
                .map(entry -> entryToConfig(entry, type))
                .collect(Collectors.toList());

        List<Long> completedAmounts = milestoneRepository.getServerCompletedAmounts(type);

        return allMilestones.stream()
                .filter(config -> !completedAmounts.contains((long) config.amount))
                .collect(Collectors.toList());
    }

    /**
     * Marks a player milestone as completed in the database.
     *
     * @param playerUUID Player UUID
     * @param type       Milestone type
     * @param amount     Milestone amount
     */
    public void markPlayerMilestoneCompleted(UUID playerUUID, MilestoneType type, long amount) {
        milestoneRepository.markPlayerCompleted(playerUUID, type, amount);
    }

    /**
     * Marks a server milestone as completed in the database.
     *
     * @param type   Milestone type
     * @param amount Milestone amount
     */
    public void markServerMilestoneCompleted(MilestoneType type, long amount) {
        milestoneRepository.markServerCompleted(type, amount);
    }

    /**
     * Converts a MilestoneEntry (new format) to MilestoneConfig (old format).
     * Maintains backwards compatibility with existing code.
     * Phase 3.2 helper method.
     *
     * @param entry Milestone entry from new config
     * @param type  Milestone type
     * @return Converted MilestoneConfig
     */
    private MilestoneConfig entryToConfig(MilestoneEntry entry, MilestoneType type) {
        MilestoneConfig config = new MilestoneConfig();
        config.setType(type);
        config.setAmount(entry.amount);
        config.setBossbar(entry.bossbar);
        config.setCommands(entry.rewards);
        return config;
    }

    /**
     * Gets the milestone repository instance.
     *
     * @return MilestoneRepository instance
     */
    public MilestoneRepository getMilestoneRepository() {
        return milestoneRepository;
    }

    /**
     * Toggles bossbar visibility for a player.
     * Async-safe in PaperMC - Adventure API bossbars are thread-safe.
     *
     * @param uuid Player UUID
     * @return true if bossbar is now visible, false if now hidden
     */
    public boolean toggleBossBarVisibility(UUID uuid) {
        boolean currentlyHidden = bossbarHidden.getOrDefault(uuid, false);
        boolean newState = !currentlyHidden;
        bossbarHidden.put(uuid, newState);

        if (newState) {
            // Hide bossbar (async-safe in PaperMC)
            BossBar bossBar = unifiedBossBar.get(uuid);
            if (bossBar != null) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    bossBar.removeViewer(player);
                }
            }
        } else {
            // Show bossbar - reload milestones asynchronously
            loadUnifiedMilestonesForPlayer(uuid);
        }

        return !newState; // Return true if now visible
    }

    /**
     * Checks if a player has hidden their bossbar.
     *
     * @param uuid Player UUID
     * @return true if bossbar is hidden
     */
    public boolean isBossBarHidden(UUID uuid) {
        return bossbarHidden.getOrDefault(uuid, false);
    }

    /**
     * Helper class to store milestone display information for cycling.
     */
    public static class MilestoneDisplayData {
        public final MilestoneType type;
        public final double targetAmount;
        public final BossBar.Color color;
        public final BossBar.Overlay style;
        public final boolean isServerMilestone;

        public MilestoneDisplayData(MilestoneType type, double targetAmount,
                                    BossBar.Color color, BossBar.Overlay style, boolean isServerMilestone) {
            this.type = type;
            this.targetAmount = targetAmount;
            this.color = color;
            this.style = style;
            this.isServerMilestone = isServerMilestone;
        }
    }

}
