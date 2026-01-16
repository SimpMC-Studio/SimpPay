package org.simpmc.simppay.config.types.data;

import de.exlll.configlib.Configuration;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Human-readable milestone entry with descriptive name.
 * Phase 3.2: New config format for easier milestone configuration.
 */
@Configuration
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MilestoneEntry {
    /**
     * Descriptive name for this milestone (e.g., "First 100k VND", "Monthly Supporter")
     */
    public String name;

    /**
     * Amount threshold required to complete this milestone
     */
    public int amount;

    /**
     * Boss bar configuration for progress tracking
     */
    public BossBarConfig bossbar;

    /**
     * List of commands to execute when milestone is completed
     * Supports PlaceholderAPI placeholders
     */
    public List<String> rewards;
}
