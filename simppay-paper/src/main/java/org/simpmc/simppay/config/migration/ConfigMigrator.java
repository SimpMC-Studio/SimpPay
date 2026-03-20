package org.simpmc.simppay.config.migration;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Reads a raw YAML config, applies sequential MigrationSteps, then writes it back.
 * Called before ConfigLib loads each config file.
 */
public class ConfigMigrator {

    private static final String VERSION_KEY = "config-version";

    public static void migrate(Path path, List<MigrationStep> steps) {
        if (steps.isEmpty()) return;
        if (!Files.exists(path)) return;

        Map<String, Object> data = readYaml(path);
        if (data == null) return;

        int currentVersion = ((Number) data.getOrDefault(VERSION_KEY, 0)).intValue();
        boolean changed = false;

        for (MigrationStep step : steps) {
            if (step.fromVersion() == currentVersion) {
                step.apply(data);
                currentVersion = step.toVersion();
                data.put(VERSION_KEY, currentVersion);
                changed = true;
            }
        }

        if (changed) {
            writeYaml(path, data);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readYaml(Path path) {
        try (InputStream is = Files.newInputStream(path)) {
            Yaml yaml = new Yaml();
            Object loaded = yaml.load(is);
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private static void writeYaml(Path path, Map<String, Object> data) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(path))) {
            yaml.dump(data, writer);
        } catch (IOException ignored) {
        }
    }
}
