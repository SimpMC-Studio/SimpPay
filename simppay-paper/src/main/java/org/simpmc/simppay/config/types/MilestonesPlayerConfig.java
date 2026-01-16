package org.simpmc.simppay.config.types;

import de.exlll.configlib.Configuration;
import net.kyori.adventure.bossbar.BossBar;
import org.simpmc.simppay.config.types.data.BossBarConfig;
import org.simpmc.simppay.config.types.data.MilestoneEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3.2: Human-readable player milestones configuration.
 * <p>
 * Example YAML output:
 * <pre>
 * milestones:
 *   alltime:
 *     - name: "First 100k VND"
 *       amount: 100000
 *       bossbar:
 *         enabled: true
 *         title: "<gold>Progress: 100k VND"
 *         color: YELLOW
 *         style: PROGRESS
 *       rewards:
 *         - "tell %player_name% Thank you for your support!"
 *   daily:
 *     - name: "Daily 50k"
 *       amount: 50000
 *       rewards:
 *         - "give %player_name% diamond 5"
 * </pre>
 */
@Configuration
public class MilestonesPlayerConfig {
    public Milestones milestones = new Milestones();

    @Configuration
    public static class Milestones {
        public List<MilestoneEntry> alltime = new ArrayList<>(List.of(
                new MilestoneEntry(
                        "First 100k VND",
                        100000,
                        new BossBarConfig(true, "Mốc Nạp Toàn Thời Gian 100k",
                                BossBar.Color.YELLOW,
                                BossBar.Overlay.PROGRESS),
                        List.of("tell %player_name% Chúc mừng bạn đã đạt Mốc Nạp Toàn Thời Gian 100k!")
                ),
                new MilestoneEntry(
                        "Total 200k VND",
                        200000,
                        new BossBarConfig(true, "Mốc Nạp Toàn Thời Gian 200k",
                                BossBar.Color.YELLOW,
                                BossBar.Overlay.PROGRESS),
                        List.of("tell %player_name% Chúc mừng bạn đã đạt Mốc Nạp Toàn Thời Gian 200k!")
                )
        ));

        public List<MilestoneEntry> daily = new ArrayList<>(List.of(
                new MilestoneEntry(
                        "Daily 50k VND",
                        50000,
                        new BossBarConfig(true, "Mốc Nạp Hàng Ngày 50k",
                                BossBar.Color.GREEN,
                                BossBar.Overlay.PROGRESS),
                        List.of("tell %player_name% Chúc mừng bạn đã đạt Mốc Nạp Hàng Ngày 50k!")
                )
        ));

        public List<MilestoneEntry> weekly = new ArrayList<>();

        public List<MilestoneEntry> monthly = new ArrayList<>();

        public List<MilestoneEntry> yearly = new ArrayList<>();
    }
}
