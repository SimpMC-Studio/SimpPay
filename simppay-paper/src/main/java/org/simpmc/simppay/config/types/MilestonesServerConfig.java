package org.simpmc.simppay.config.types;

import de.exlll.configlib.Configuration;
import net.kyori.adventure.bossbar.BossBar;
import org.simpmc.simppay.config.types.data.BossBarConfig;
import org.simpmc.simppay.config.types.data.MilestoneEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3.2: Human-readable server-wide milestones configuration.
 * <p>
 * Server milestones are triggered when the entire server reaches a payment threshold.
 * Rewards are executed for all online players.
 */
@Configuration
public class MilestonesServerConfig {
    public Milestones milestones = new Milestones();

    @Configuration
    public static class Milestones {
        public List<MilestoneEntry> alltime = new ArrayList<>(List.of(
                new MilestoneEntry(
                        "Server 100k VND Total",
                        100000,
                        new BossBarConfig(true, "Mốc Nạp Toàn Server 100k Toàn Thời Gian",
                                BossBar.Color.YELLOW,
                                BossBar.Overlay.PROGRESS),
                        List.of("say Chúc mừng! Server đã đạt Mốc Nạp Toàn Server 100k!")
                ),
                new MilestoneEntry(
                        "Server 200k VND Total",
                        200000,
                        new BossBarConfig(true, "Mốc Nạp Toàn Server 200k Toàn Thời Gian",
                                BossBar.Color.YELLOW,
                                BossBar.Overlay.PROGRESS),
                        List.of("say Chúc mừng! Server đã đạt Mốc Nạp Toàn Server 200k!")
                )
        ));

        public List<MilestoneEntry> daily = new ArrayList<>();

        public List<MilestoneEntry> weekly = new ArrayList<>();

        public List<MilestoneEntry> monthly = new ArrayList<>();

        public List<MilestoneEntry> yearly = new ArrayList<>();
    }
}
