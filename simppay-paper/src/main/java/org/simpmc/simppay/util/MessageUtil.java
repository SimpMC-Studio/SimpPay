package org.simpmc.simppay.util;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MainConfig;
import org.simpmc.simppay.config.types.MessageConfig;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.UUID;

public class MessageUtil {
    public static void sendMessage(Player player, String message) {
        taskMessage(message, player);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender instanceof Player) {
            taskMessage(message, (Player) sender);
        } else {
            SPPlugin.getInstance().getLogger().info(message);
        }
    }

    public static void sendMessage(UUID playerUuid, String message) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return;
        }
        taskMessage(message, player);
    }

    public static Component getComponentParsed(String message, Player player) {
        MiniMessage mm = MiniMessage.builder()
                .tags(TagResolver.builder()
                        .resolver(StandardTags.defaults())
                        .resolver(papiTag(player))
                        .build()
                )
                .build();

        Component s = mm.deserialize(message);
        return s;
    }

    private static void taskMessage(String message, Player player) {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
            MessageConfig messageConfig = ConfigManager.getInstance().getConfig(MessageConfig.class);
            MiniMessage mm = MiniMessage.builder()
                    .tags(TagResolver.builder()
                            .resolver(StandardTags.defaults())
                            .resolver(papiTag(player))
                            .build()
                    )
                    .build();

            Component s = mm.deserialize(message);
            Component prefix = mm.deserialize(messageConfig.prefix);
            player.sendMessage(prefix.append(s));
        });
    }

    public static void debug(String message) {

        MainConfig mainConfig = ConfigManager.getInstance().getConfig(MainConfig.class);
        if (mainConfig.debug) {
            SPPlugin.getInstance().getLogger().info(message);
        }
    }

    public static void info(String message) {
        SPPlugin.getInstance().getLogger().info(message);
    }

    public static void warn(String message) {
        SPPlugin.getInstance().getLogger().warning(message);
    }

    /**
     * Logs an error message that always appears in console (not debug-dependent).
     * Use for critical payment failures that require troubleshooting.
     *
     * @param message The error message to log
     */
    public static void error(String message) {
        SPPlugin.getInstance().getLogger().severe(message);
    }

    /**
     * Logs an error message with exception details.
     * Use for critical payment failures with exception context.
     *
     * @param message The error message to log
     * @param e       The exception that caused the error
     */
    public static void error(String message, Throwable e) {
        SPPlugin.getInstance().getLogger().severe(message + " | Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    /**
     * Creates a tag resolver capable of resolving PlaceholderAPI tags for a given player.
     *
     * @param player the player
     * @return the tag resolver
     */
    private static TagResolver papiTag(final Player player) {
        return TagResolver.resolver("papi", (argumentQueue, context) -> {
            // Get the string placeholder that they want to use.
            final String papiPlaceholder = argumentQueue.popOr("papi tag requires an argument").value();

            // Then get PAPI to parse the placeholder for the given player.
            final String parsedPlaceholder = PlaceholderAPI.setPlaceholders(player, '%' + papiPlaceholder + '%');

            // We need to turn this ugly legacy string into a nice component.
            final Component componentPlaceholder = LegacyComponentSerializer.legacySection().deserialize(parsedPlaceholder);

            // Finally, return the tag instance to insert the placeholder!
            return Tag.selfClosingInserting(componentPlaceholder);
        });
    }

    /**
     * Formats amount in Vietnamese currency format with dot separators.
     * Example: 50000 -> "50.000đ"
     *
     * @param amount The amount to format
     * @return Formatted currency string
     */
    public static String formatVietnameseCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        formatter.setDecimalFormatSymbols(symbols);
        return formatter.format(amount) + "đ";
    }

    /**
     * Formats amount in short Vietnamese currency format.
     * Examples: 500đ, 1k, 50k, 1.5tr, 15.2tr, 1.23tỷ
     *
     * @param amount The amount to format
     * @return Formatted short currency string
     */
    public static String formatShortCurrency(double amount) {
        if (amount < 1_000) {
            return String.format("%.0fđ", amount);
        } else if (amount < 1_000_000) {
            double k = amount / 1_000;
            if (k == Math.floor(k)) {
                return String.format("%.0fk", k);
            } else if (k < 10) {
                return String.format("%.1fk", k);
            } else {
                return String.format("%.0fk", k);
            }
        } else if (amount < 1_000_000_000) {
            double tr = amount / 1_000_000;
            if (tr == Math.floor(tr)) {
                return String.format("%.0ftr", tr);
            } else if (tr < 10) {
                return String.format("%.2ftr", tr);
            } else {
                return String.format("%.1ftr", tr);
            }
        } else {
            double ty = amount / 1_000_000_000;
            if (ty == Math.floor(ty)) {
                return String.format("%.0ftỷ", ty);
            } else {
                return String.format("%.2ftỷ", ty);
            }
        }
    }
}
