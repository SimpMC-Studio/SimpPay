package org.simpmc.simppay.service.milestone;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.Particle;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.types.data.MilestoneConfig;
import org.simpmc.simppay.util.MessageUtil;

import java.time.Duration;

/**
 * Handles milestone notifications and celebrations
 * Supports: titles, sounds, particles, and broadcasts
 */
@Slf4j
@RequiredArgsConstructor
public class MilestoneNotificationService {

    /**
     * Notify player of milestone completion
     */
    public void notifyPlayerMilestoneComplete(Player player, MilestoneConfig milestone) {
        try {
            // Send title notification
            sendTitle(player, milestone);

            // Play sound
            playSound(player, milestone);

            // Show particles
            showParticles(player, milestone);

            // Send chat message
            sendChatMessage(player, milestone);

            MessageUtil.debug("Notified player " + player.getName() + " of milestone completion");
        } catch (Exception e) {
            log.error("Error notifying player of milestone completion", e);
        }
    }

    /**
     * Notify all online players of server milestone completion
     */
    public void notifyServerMilestoneComplete(MilestoneConfig milestone) {
        try {
            // Broadcast to all players
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    // Send title
                    Title title = Title.title(
                            Component.text("§e§lServer Milestone!"),
                            Component.text("§f" + milestone.getDisplayName()),
                            Title.Times.of(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    );
                    player.showTitle(title);

                    // Play sound
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.0f);

                    // Show particles
                    player.spawnParticle(Particle.FIREWORK, player.getLocation(), 10);

                } catch (Exception e) {
                    log.debug("Error sending notification to player {}", player.getName());
                }
            }

            // Send global broadcast
            String broadcastMessage = "§6§l[MILESTONE] §r§eServer reached milestone: §f" +
                    String.format("%,d", milestone.getAmount());
            Bukkit.broadcast(Component.text(broadcastMessage));

            MessageUtil.info("Server milestone completed: " + milestone.getAmount());
        } catch (Exception e) {
            log.error("Error notifying server milestone completion", e);
        }
    }

    /**
     * Send title notification to player
     */
    private void sendTitle(Player player, MilestoneConfig milestone) {
        try {
            Title title = Title.title(
                    Component.text("§e§lMilestone!"),
                    Component.text("§f" + milestone.getDisplayName()),
                    Title.Times.of(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
            );
            player.showTitle(title);
        } catch (Exception e) {
            log.debug("Error sending title to player {}", player.getName(), e);
        }
    }

    /**
     * Play sound notification
     */
    private void playSound(Player player, MilestoneConfig milestone) {
        try {
            // Milestone achieved sound
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } catch (Exception e) {
            log.debug("Error playing sound for player {}", player.getName(), e);
        }
    }

    /**
     * Show particle effects
     */
    private void showParticles(Player player, MilestoneConfig milestone) {
        try {
            // Show celebration particles
            player.spawnParticle(Particle.FIREWORK, player.getLocation(), 15);
            player.spawnParticle(Particle.HEART, player.getLocation(), 10);
        } catch (Exception e) {
            log.debug("Error showing particles for player {}", player.getName(), e);
        }
    }

    /**
     * Send chat message
     */
    private void sendChatMessage(Player player, MilestoneConfig milestone) {
        try {
            String message = "§6=== Milestone Completed ===\n" +
                    "§f" + milestone.getDisplayName() + "\n" +
                    "§7" + milestone.getDescription() + "\n" +
                    "§6=====================================";

            MessageUtil.sendMessage(player, message);
        } catch (Exception e) {
            log.debug("Error sending chat message to player {}", player.getName(), e);
        }
    }

    /**
     * Send a simple progress update (non-intrusive)
     */
    public void sendProgressUpdate(Player player, MilestoneConfig milestone, double progressPercent) {
        try {
            int progressBar = (int) (progressPercent / 5); // 20 segments
            String bar = "§2" + "█".repeat(progressBar) + "§7" + "░".repeat(20 - progressBar);

            String message = "§7" + milestone.getDisplayName() + " §8[§e" + String.format("%.1f%%", progressPercent) + "§8]";
            MessageUtil.sendMessage(player, message);
        } catch (Exception e) {
            log.debug("Error sending progress update to player {}", player.getName(), e);
        }
    }
}
