package org.simpmc.simppay.config.migration;

import java.util.Map;

/**
 * Represents a single migration step for a config YAML file.
 * Migrations are applied sequentially based on config-version key.
 */
public interface MigrationStep {
    int fromVersion();
    int toVersion();
    void apply(Map<String, Object> yamlData);
}
