package org.simpmc.simppay.config;

import de.exlll.configlib.ConfigLib;
import de.exlll.configlib.NameFormatters;
import de.exlll.configlib.YamlConfigurationProperties;
import de.exlll.configlib.YamlConfigurationStore;
import lombok.Getter;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.annotations.Folder;
import org.simpmc.simppay.config.migration.MigrationRegistry;
import org.simpmc.simppay.config.serializers.KeySerializer;
import org.simpmc.simppay.config.serializers.SoundComponentSerializer;
import org.simpmc.simppay.config.types.*;
import org.simpmc.simppay.config.types.DiscordConfig;
import org.simpmc.simppay.config.types.banking.PayosConfig;
import org.simpmc.simppay.config.types.banking.SepayConfig;
import org.simpmc.simppay.config.types.banking.Web2mConfig;
import org.simpmc.simppay.config.types.card.*;
import org.simpmc.simppay.config.types.menu.FormsConfig;
import org.simpmc.simppay.config.types.menu.PaymentHistoryMenuConfig;
import org.simpmc.simppay.config.types.menu.ServerPaymentHistoryMenuConfig;
import org.simpmc.simppay.config.types.menu.StreakMenuConfig;
import org.simpmc.simppay.config.types.menu.card.CardListMenuConfig;
import org.simpmc.simppay.config.types.menu.card.CardPriceMenuConfig;
import org.simpmc.simppay.config.types.menu.card.anvil.CardPinMenuConfig;
import org.simpmc.simppay.config.types.menu.card.anvil.CardSerialMenuConfig;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static de.exlll.configlib.DeserializationCoercionType.BOOLEAN_TO_STRING;
import static de.exlll.configlib.DeserializationCoercionType.NUMBER_TO_STRING;

public class ConfigManager {
    @Getter
    private static ConfigManager instance;

    private final Map<Class<?>, Object> configs = new HashMap<>();
    private final SPPlugin plugin;

    private final List<Class<?>> configClasses = List.of(
            // --- Core ---
            MainConfig.class,
            MessageConfig.class,
            DatabaseConfig.class,
            CoinsConfig.class,
            BankingConfig.class,
            CardConfig.class,

            // --- Banking Gateways ---
            PayosConfig.class,
            SepayConfig.class,
            Web2mConfig.class,

            // --- Card Gateways ---
            ThesieutocConfig.class,
            Gachthe1sConfig.class,
            Card2KConfig.class,
            ThesieureConfig.class,
            Doithe1sConfig.class,

            // --- Menus (Java) ---
            CardListMenuConfig.class,
            CardPriceMenuConfig.class,
            CardPinMenuConfig.class,
            CardSerialMenuConfig.class,
            PaymentHistoryMenuConfig.class,
            ServerPaymentHistoryMenuConfig.class,
            StreakMenuConfig.class,

            // --- Menus (Bedrock) ---
            FormsConfig.class,

            // --- Milestones & Rewards ---
            MilestonesPlayerConfig.class,
            MilestonesServerConfig.class,
            StreakConfig.class,
            NaplandauConfig.class,
            MocNapConfig.class,
            MocNapServerConfig.class,

            // --- Integrations ---
            DiscordConfig.class
    );

    private final Map<Class<?>, Path> configPaths = new HashMap<>();

    private final YamlConfigurationProperties properties = ConfigLib.BUKKIT_DEFAULT_PROPERTIES.toBuilder()
            .addSerializer(Key.class, new KeySerializer())
            .addSerializer(Sound.class, new SoundComponentSerializer())
            .setDeserializationCoercionTypes(BOOLEAN_TO_STRING, NUMBER_TO_STRING)
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .header("""
                    SimpPay @ 2025
                    Made by typical.smc
                    """)
            .build();

    public ConfigManager(SPPlugin plugin) {
        this.plugin = plugin;
        instance = this;
        initPaths();
        registerAll();
    }

    private void initPaths() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        for (Class<?> clazz : configClasses) {
            configPaths.put(clazz, getConfigPath(clazz));
        }
    }

    private Path getConfigPath(Class<?> clazz) {
        String fileName = getConfigFileName(clazz.getSimpleName()) + ".yml";
        if (clazz.isAnnotationPresent(Folder.class)) {
            String folderName = clazz.getAnnotation(Folder.class).value();
            File folder = new File(plugin.getDataFolder(), folderName);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            return Paths.get(folder.getPath(), fileName);
        }
        return Paths.get(plugin.getDataFolder().getPath(), fileName);
    }

    @SuppressWarnings("unchecked")
    private void registerAll() {
        plugin.getLogger().info("Loading all configurations");
        for (Class<?> rawClass : configClasses) {
            registerConfig((Class<Object>) rawClass);
        }
        plugin.getLogger().info("All configurations loaded successfully");
    }

    private <T> void registerConfig(Class<T> cfgClass) {
        Path path = configPaths.get(cfgClass);
        try {
            MigrationRegistry.migrateIfNeeded(cfgClass, path);
            YamlConfigurationStore<T> store = new YamlConfigurationStore<>(cfgClass, properties);
            store.update(path);
            T loaded = store.load(path);
            configs.put(cfgClass, loaded);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load config for " + cfgClass.getSimpleName() + ": " + e.getMessage());
            plugin.getLogger().severe("Using default values for " + cfgClass.getSimpleName());
            try {
                configs.put(cfgClass, cfgClass.getDeclaredConstructor().newInstance());
            } catch (Exception ex) {
                plugin.getLogger().severe("Could not create default instance for " + cfgClass.getSimpleName());
            }
        }
    }

    /**
     * Reload all configs (e.g. on /simppayadmin reload)
     */
    public void reloadAll() {
        configs.clear();
        plugin.getLogger().info("Reloading all configurations");
        for (Class<?> rawClass : configClasses) {
            registerConfig(rawClass);
        }
        plugin.getLogger().info("All configurations reloaded successfully");
    }

    /**
     * Retrieve a loaded configuration instance by its class
     */
    @SuppressWarnings("unchecked")
    public <T> T getConfig(Class<T> cls) {
        return (T) configs.get(cls);
    }

    private String getConfigFileName(String name) {
        var builder = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i != 0) {
                    builder.append('-');
                }
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }
}
