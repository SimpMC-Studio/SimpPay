package org.simpmc.simppay.service;

import org.simpmc.simppay.SPPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class OrderIDService {

    // The filename within the plugin data folder
    private static final String FILE_NAME = "last_id.txt";

    // Thread-safe counter
    private static final AtomicLong counter = new AtomicLong(0);

    // File where the counter is persisted
    private static File dataFile;

    /**
     * Call this once from your plugin's onEnable().
     * It will create the data folder/file if needed and load the last saved ID.
     */
    public static void init(SPPlugin plugin) {
        // Ensure data folder exists
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dataFile = new File(plugin.getDataFolder(), FILE_NAME);

        // If file doesn't exist, create it with a zero
        if (!dataFile.exists()) {
            try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                w.write("0");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create " + FILE_NAME + ": " + e.getMessage());
            }
        }

        // Load last ID
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_8))) {
            String line = r.readLine();
            long last = Long.parseLong(line.trim());
            counter.set(last);
        } catch (Exception e) {
            plugin.getLogger().severe("Could not load last ID from " + FILE_NAME + ": " + e.getMessage());
        }
    }

    /**
     * Gets the next unique ID (thread-safe) and immediately persists it.
     *
     * @return the next ID
     */
    public static long getNextId() {
        long next = counter.incrementAndGet();
        saveCurrent();
        return next;
    }

    // Atomically writes the current counter value to disk
    public static void saveCurrent() {
        if (dataFile == null) {
            return;
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile, false), StandardCharsets.UTF_8)) {
            w.write(Long.toString(counter.get()));
        } catch (IOException e) {
            // Best effort: log to console
            System.err.println("Failed to save ID to " + FILE_NAME + ": " + e.getMessage());
        }
    }
}
