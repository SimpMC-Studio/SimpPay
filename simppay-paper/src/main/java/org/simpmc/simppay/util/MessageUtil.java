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
}
