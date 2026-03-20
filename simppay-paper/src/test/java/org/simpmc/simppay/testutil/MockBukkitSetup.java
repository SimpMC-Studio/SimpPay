package org.simpmc.simppay.testutil;

import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MainConfig;

import java.lang.reflect.Field;

import static org.mockito.Mockito.*;

/**
 * Utility to inject mock dependencies for unit tests that involve ConfigManager or Bukkit.
 */
public class MockBukkitSetup {

    /**
     * Creates and injects a mock ConfigManager.
     * Always includes a default MainConfig with debug=false so MessageUtil.debug() is a no-op.
     *
     * @param configEntries alternating Class<?>, Object pairs: config class → instance
     * @return the mock ConfigManager
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static ConfigManager mockConfigManager(Object... configEntries) {
        ConfigManager mockCM = mock(ConfigManager.class);

        // Always provide a default MainConfig so MessageUtil.debug() doesn't NPE
        MainConfig defaultMain = new MainConfig();
        defaultMain.debug = false;
        doReturn(defaultMain).when(mockCM).getConfig(MainConfig.class);

        // Apply caller-provided overrides
        for (int i = 0; i + 1 < configEntries.length; i += 2) {
            Class cls = (Class) configEntries[i];
            Object cfg = configEntries[i + 1];
            doReturn(cfg).when(mockCM).getConfig(cls);
        }

        injectConfigManager(mockCM);
        return mockCM;
    }

    /**
     * Sets ConfigManager.instance via reflection.
     */
    public static void injectConfigManager(ConfigManager configManager) {
        try {
            Field field = ConfigManager.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, configManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject ConfigManager mock", e);
        }
    }

    /**
     * Clears the ConfigManager singleton (useful for @AfterEach cleanup).
     */
    public static void clearConfigManager() {
        injectConfigManager(null);
    }
}
