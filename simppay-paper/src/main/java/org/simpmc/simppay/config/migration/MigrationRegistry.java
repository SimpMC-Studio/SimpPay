package org.simpmc.simppay.config.migration;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps config classes to their migration steps.
 * Framework is in place for future migrations — currently no migrations are registered.
 *
 * Usage example:
 * <pre>
 * MigrationRegistry.register(SomeConfig.class, List.of(new SomeMigrationV1toV2()));
 * </pre>
 */
public class MigrationRegistry {

    private static final Map<Class<?>, List<MigrationStep>> registry = new HashMap<>();

    public static void register(Class<?> configClass, List<MigrationStep> steps) {
        registry.put(configClass, steps);
    }

    public static void migrateIfNeeded(Class<?> configClass, Path path) {
        List<MigrationStep> steps = registry.getOrDefault(configClass, Collections.emptyList());
        if (!steps.isEmpty()) {
            ConfigMigrator.migrate(path, steps);
        }
    }
}
