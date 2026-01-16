package org.simpmc.simppay.util;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Phase 7: Enhanced Floodgate Utility
 * <p>
 * Provides helper methods for Floodgate integration (Bedrock player support).
 * Includes initialization, player detection, and form sending with proper error handling.
 */
public class FloodgateUtil {
    private static FloodgateApi floodgateApi;
    private static boolean initialized = false;
    private static final Logger LOGGER = Logger.getLogger(FloodgateUtil.class.getName());

    /**
     * Initializes the Floodgate API connection.
     * Should be called during plugin enable if Floodgate is detected.
     *
     * @return true if initialization succeeded, false otherwise
     */
    public static boolean initialize() {
        if (initialized) {
            return true;
        }

        try {
            floodgateApi = FloodgateApi.getInstance();
            initialized = true;
            LOGGER.info("FloodgateUtil initialized successfully");
            return true;
        } catch (Exception e) {
            LOGGER.warning("Failed to initialize FloodgateUtil: " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if FloodgateUtil has been successfully initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Checks if a player is a Bedrock Edition player.
     * <p>
     * This method uses multiple detection strategies:
     * 1. FloodgateApi.isFloodgatePlayer() (most reliable)
     * 2. UUID most significant bits check (fallback)
     *
     * @param player Player to check
     * @return true if the player is from Bedrock Edition
     */
    public static boolean isBedrockPlayer(Player player) {
        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        // Primary method: Use Floodgate API
        if (initialized && floodgateApi != null) {
            try {
                return floodgateApi.isFloodgatePlayer(uuid);
            } catch (Exception e) {
                LOGGER.warning("Error checking if player is Bedrock via API: " + e.getMessage());
            }
        }

        // Fallback method: Check UUID pattern
        // Floodgate UUIDs have most significant bits set to 0
        return uuid.getMostSignificantBits() == 0;
    }

    /**
     * Checks if a UUID belongs to a Bedrock Edition player.
     *
     * @param uuid UUID to check
     * @return true if the UUID belongs to a Bedrock player
     */
    public static boolean isBedrockPlayer(UUID uuid) {
        if (uuid == null) {
            return false;
        }

        if (initialized && floodgateApi != null) {
            try {
                return floodgateApi.isFloodgatePlayer(uuid);
            } catch (Exception e) {
                LOGGER.warning("Error checking UUID via Floodgate API: " + e.getMessage());
            }
        }

        return uuid.getMostSignificantBits() == 0;
    }

    /**
     * Gets the Bedrock username for a player (without the prefix).
     *
     * @param player Player to get username for
     * @return Bedrock username, or player's regular username if not a Bedrock player
     */
    public static String getBedrockUsername(Player player) {
        if (player == null) {
            return null;
        }

        if (!isBedrockPlayer(player)) {
            return player.getName();
        }

        if (initialized && floodgateApi != null) {
            try {
                FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
                if (floodgatePlayer != null) {
                    return floodgatePlayer.getUsername();
                }
            } catch (Exception e) {
                LOGGER.warning("Error getting Bedrock username: " + e.getMessage());
            }
        }

        return player.getName();
    }

    /**
     * Gets the Xbox User ID (XUID) for a Bedrock player.
     *
     * @param player Player to get XUID for
     * @return Optional containing XUID if available
     */
    public static Optional<String> getXboxUID(Player player) {
        if (player == null || !isBedrockPlayer(player)) {
            return Optional.empty();
        }

        if (initialized && floodgateApi != null) {
            try {
                FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
                if (floodgatePlayer != null) {
                    return Optional.of(floodgatePlayer.getXuid());
                }
            } catch (Exception e) {
                LOGGER.warning("Error getting Xbox UID: " + e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Gets the FloodgatePlayer object for detailed information.
     *
     * @param player Player to get FloodgatePlayer for
     * @return Optional containing FloodgatePlayer if available
     */
    public static Optional<FloodgatePlayer> getFloodgatePlayer(Player player) {
        if (player == null || !initialized || floodgateApi == null) {
            return Optional.empty();
        }

        try {
            FloodgatePlayer floodgatePlayer = floodgateApi.getPlayer(player.getUniqueId());
            return Optional.ofNullable(floodgatePlayer);
        } catch (Exception e) {
            LOGGER.warning("Error getting FloodgatePlayer: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Sends a Cumulus form to a Bedrock player.
     *
     * @param uuid UUID of the player
     * @param form Form to send
     * @return true if form was sent successfully
     */
    public static boolean sendForm(UUID uuid, Form form) {
        if (uuid == null || form == null) {
            LOGGER.warning("Cannot send form: UUID or form is null");
            return false;
        }

        if (!initialized || floodgateApi == null) {
            LOGGER.warning("Cannot send form: FloodgateUtil not initialized");
            return false;
        }

        if (!isBedrockPlayer(uuid)) {
            LOGGER.warning("Cannot send form: Player " + uuid + " is not a Bedrock player");
            return false;
        }

        try {
            floodgateApi.sendForm(uuid, form);
            return true;
        } catch (Exception e) {
            LOGGER.severe("Error sending form to " + uuid + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a form to a Bedrock player (convenience method).
     *
     * @param player Player to send form to
     * @param form   Form to send
     * @return true if form was sent successfully
     */
    public static boolean sendForm(Player player, Form form) {
        if (player == null) {
            LOGGER.warning("Cannot send form: Player is null");
            return false;
        }

        return sendForm(player.getUniqueId(), form);
    }

    /**
     * Legacy method for backward compatibility.
     * Accepts Object form and casts it to Form.
     *
     * @param uuid UUID of the player
     * @param form Form object (will be cast to Form)
     * @deprecated Use {@link #sendForm(UUID, Form)} instead
     */
    @Deprecated
    public static void sendForm(UUID uuid, Object form) {
        if (form instanceof Form) {
            sendForm(uuid, (Form) form);
        } else {
            LOGGER.warning("Invalid form object type: " + (form != null ? form.getClass().getName() : "null"));
        }
    }
}
