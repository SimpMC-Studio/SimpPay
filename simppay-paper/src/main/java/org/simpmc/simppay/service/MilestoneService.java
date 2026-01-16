package org.simpmc.simppay.service;

import it.unimi.dsi.fastutil.objects.ObjectObjectMutablePair;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MilestonesPlayerConfig;
import org.simpmc.simppay.config.types.MilestonesServerConfig;
import org.simpmc.simppay.config.types.MocNapConfig;
import org.simpmc.simppay.config.types.MocNapServerConfig;
import org.simpmc.simppay.config.types.data.BossBarConfig;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.config.types.data.MilestoneEntry;
import org.simpmc.simppay.data.milestone.MilestoneType;
import org.simpmc.simppay.database.entities.SPPlayer;
import org.simpmc.simppay.repository.MilestoneRepository;
import org.simpmc.simppay.service.database.PaymentLogService;
import org.simpmc.simppay.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MilestoneService implements IService {
    //     NOTE: BossBar are static, changing bossbar reflect the changes to player who are added to it
//     Design a central bossbar of the plugin
//     Use one for player
//     one for entire server
//     config example
//     ALL:
//     - amount: 100
//       commands:
//       - "/tell %player_name% Cảm ơn đã ủng hộ server hehe"
//       - "/tell %player_name% Cảm ơn đã ủng hộ server hehe"
//     DAILY:
//     - amount: 100
//       commands:
//       - "/tell %player_name% Cảm ơn đã ủng hộ server hehe"
    public ConcurrentHashMap<UUID, List<ObjectObjectMutablePair<MilestoneConfig, BossBar>>> playerBossBars = new ConcurrentHashMap<>();
    public ConcurrentHashMap<UUID, List<MilestoneConfig>> playerCurrentMilestones = new ConcurrentHashMap<>();
    public List<MilestoneConfig> serverCurrentMilestones = new ArrayList<>();
    public List<ObjectObjectMutablePair<MilestoneConfig, BossBar>> serverBossbars = new ArrayList<>(); // contains all valid loaded milestones

    private MilestoneRepository milestoneRepository;


    @Override
    public void setup() {
        milestoneRepository = new MilestoneRepository();
        playerCurrentMilestones.clear();
        playerBossBars.clear();
        serverCurrentMilestones.clear();
        serverBossbars.clear();
        loadServerMilestone();
        for (Player p : Bukkit.getOnlinePlayers()) { // thread-safe for folia
            loadPlayerMilestone(p.getUniqueId());
        }
    }

    @Override
    public void shutdown() {
        playerCurrentMilestones.clear();
        playerBossBars.clear();
        serverCurrentMilestones.clear();
        serverBossbars.clear();
    }

    // Phase 3.2: Updated to use new MilestonesServerConfig format
    public void loadServerMilestone() {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();
            MilestonesServerConfig milestonesConfig = ConfigManager.getInstance().getConfig(MilestonesServerConfig.class);

            // Process all milestone types
            for (MilestoneType type : MilestoneType.values()) {
                List<MilestoneEntry> entries = switch (type) {
                    case ALL -> milestonesConfig.milestones.alltime;
                    case DAILY -> milestonesConfig.milestones.daily;
                    case WEEKLY -> milestonesConfig.milestones.weekly;
                    case MONTHLY -> milestonesConfig.milestones.monthly;
                    case YEARLY -> milestonesConfig.milestones.yearly;
                    default -> new ArrayList<>();
                };

                MessageUtil.debug("Loading Server Milestones for " + type.name());

                for (MilestoneEntry entry : entries) {
                    MilestoneConfig config = entryToConfig(entry, type);

                    double serverBal = switch (type) {
                        case ALL -> paymentLogService.getEntireServerAmount();
                        case DAILY -> paymentLogService.getEntireServerDailyAmount();
                        case WEEKLY -> paymentLogService.getEntireServerWeeklyAmount();
                        case MONTHLY -> paymentLogService.getEntireServerMonthlyAmount();
                        case YEARLY -> paymentLogService.getEntireServerYearlyAmount();
                        default -> 0;
                    };

                    // Skip if already completed
                    if (config.amount <= serverBal) {
                        continue;
                    }

                    if (config.bossbar.enabled) {
                        BossBar bossBar = BossBar.bossBar(
                                MessageUtil.getComponentParsed(config.bossbar.getTitle(), null),
                                (float) (serverBal / config.amount),
                                config.bossbar.color,
                                config.bossbar.style
                        );
                        serverBossbars.add(new ObjectObjectMutablePair<>(config, bossBar));
                        MessageUtil.debug("Loaded Server BossBar: " + entry.name + " (" + config.amount + ")");
                    }

                    serverCurrentMilestones.add(config);
                    MessageUtil.debug("Loaded Server Milestone: " + entry.name + " (" + config.amount + ")");
                }
            }
        });
    }

    // Phase 3.2: Updated to use new MilestonesPlayerConfig format
    public void loadPlayerMilestone(UUID uuid) {
        playerCurrentMilestones.remove(uuid);
        playerBossBars.remove(uuid);
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            PaymentLogService paymentLogService = SPPlugin.getService(DatabaseService.class).getPaymentLogService();
            SPPlayer player = SPPlugin.getService(DatabaseService.class).getPlayerService().findByUuid(uuid);

            if (player == null) {
                return;
            }

            MilestonesPlayerConfig milestonesConfig = ConfigManager.getInstance().getConfig(MilestonesPlayerConfig.class);
            MessageUtil.debug("Loading Player Milestones for " + player.getName());

            playerBossBars.putIfAbsent(uuid, new ArrayList<>());
            playerCurrentMilestones.putIfAbsent(uuid, new ArrayList<>());

            // Process all milestone types
            for (MilestoneType type : MilestoneType.values()) {
                List<MilestoneEntry> entries = switch (type) {
                    case ALL -> milestonesConfig.milestones.alltime;
                    case DAILY -> milestonesConfig.milestones.daily;
                    case WEEKLY -> milestonesConfig.milestones.weekly;
                    case MONTHLY -> milestonesConfig.milestones.monthly;
                    case YEARLY -> milestonesConfig.milestones.yearly;
                    default -> new ArrayList<>();
                };

                for (MilestoneEntry entry : entries) {
                    MilestoneConfig config = entryToConfig(entry, type);

                    double playerBal = switch (type) {
                        case ALL -> paymentLogService.getPlayerTotalAmount(player);
                        case DAILY -> paymentLogService.getPlayerDailyAmount(player);
                        case WEEKLY -> paymentLogService.getPlayerWeeklyAmount(player);
                        case MONTHLY -> paymentLogService.getPlayerMonthlyAmount(player);
                        case YEARLY -> paymentLogService.getPlayerYearlyAmount(player);
                        default -> 0;
                    };

                    // Skip if already completed
                    if (config.amount <= playerBal) {
                        continue;
                    }

                    if (config.bossbar.enabled) {
                        BossBar bossBar = BossBar.bossBar(
                                MessageUtil.getComponentParsed(config.bossbar.getTitle(), null),
                                (float) (playerBal / config.amount),
                                config.bossbar.color,
                                config.bossbar.style
                        );
                        playerBossBars.get(uuid).add(new ObjectObjectMutablePair<>(config, bossBar));
                        MessageUtil.debug("Loaded Player BossBar: " + entry.name + " (" + config.amount + ")");
                    }

                    playerCurrentMilestones.get(uuid).add(config);
                    MessageUtil.debug("Loaded Player Milestone: " + entry.name + " (" + config.amount + ")");
                }
            }
        });
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

}
