package org.simpmc.simppay.service.cache;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Represents a single entry in the leaderboard cache
 */
@Data
@AllArgsConstructor
public class LeaderboardEntry {
    private UUID playerUUID;
    private String playerName;
    private long amount;
    private int rank;
}
