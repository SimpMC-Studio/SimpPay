package org.simpmc.simppay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MainConfig;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.util.MessageUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpdateCheckerService implements IService, Listener {

    private static final String STABLE_VERSION_URL =
            "https://raw.githubusercontent.com/SimpMC-Studio/SimpPay/main/version.json";
    private static final String RELEASES_API_URL =
            "https://api.github.com/repos/SimpMC-Studio/SimpPay/releases";

    private OkHttpClient client;
    private volatile boolean hasUpdate = false;
    private volatile String latestVersion = null;
    private final List<String> changelog = new ArrayList<>();

    @Override
    public void setup() {
        MainConfig mainConfig = ConfigManager.getInstance().getConfig(MainConfig.class);
        if (!mainConfig.updateChecker) return;

        client = new OkHttpClient();
        String currentVersion = SPPlugin.getInstance().getDescription().getVersion();
        boolean devChannel = "dev".equalsIgnoreCase(mainConfig.updateChannel);

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            if (devChannel) {
                checkForDevUpdate(currentVersion);
            } else {
                checkForUpdate(currentVersion);
            }
        });
    }

    @Override
    public void shutdown() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }

    private void checkForUpdate(String currentVersion) {
        Request request = new Request.Builder().url(STABLE_VERSION_URL).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                MessageUtil.debug("[UpdateChecker] Không thể lấy thông tin phiên bản: HTTP " + response.code());
                return;
            }

            String body = response.body().string();
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String fetchedVersion = json.get("version").getAsString();

            JsonArray changelogArray = json.getAsJsonArray("changelog");
            if (changelogArray != null) {
                for (int i = 0; i < changelogArray.size(); i++) {
                    changelog.add(changelogArray.get(i).getAsString());
                }
            }

            if (isNewerVersion(fetchedVersion, currentVersion)) {
                latestVersion = fetchedVersion;
                hasUpdate = true;
                MessageUtil.info("[SimpPay] Có bản cập nhật mới: v" + latestVersion + " (hiện tại: v" + currentVersion + ")");
                for (String line : changelog) {
                    MessageUtil.info("  - " + line);
                }
            } else {
                MessageUtil.debug("[UpdateChecker] Plugin đang ở phiên bản mới nhất: v" + currentVersion);
            }

        } catch (IOException e) {
            MessageUtil.debug("[UpdateChecker] Không thể kiểm tra cập nhật: " + e.getMessage());
        }
    }

    private void checkForDevUpdate(String currentVersion) {
        Request request = new Request.Builder()
                .url(RELEASES_API_URL)
                .header("Accept", "application/vnd.github+json")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                MessageUtil.debug("[UpdateChecker] Không thể lấy danh sách releases: HTTP " + response.code());
                return;
            }

            String body = response.body().string();
            JsonArray releases = JsonParser.parseString(body).getAsJsonArray();

            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                boolean isPreRelease = release.get("prerelease").getAsBoolean();
                if (!isPreRelease) continue;

                String tagName = release.get("tag_name").getAsString();
                if (!tagName.startsWith("dev-")) continue;

                // Found latest dev pre-release
                if (isNewerVersion(tagName, currentVersion)) {
                    latestVersion = tagName;
                    hasUpdate = true;
                    MessageUtil.info("[SimpPay] Có bản dev mới: " + latestVersion + " (hiện tại: v" + currentVersion + ")");
                } else {
                    MessageUtil.debug("[UpdateChecker] Đang dùng bản dev mới nhất: v" + currentVersion);
                }
                return;
            }

        } catch (IOException e) {
            MessageUtil.debug("[UpdateChecker] Không thể kiểm tra cập nhật dev: " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!hasUpdate || latestVersion == null) return;
        if (!event.getPlayer().hasPermission("simppay.admin")) return;

        MessageConfig messages = ConfigManager.getInstance().getConfig(MessageConfig.class);
        String currentVersion = SPPlugin.getInstance().getDescription().getVersion();

        String notification = messages.updateAvailable
                .replace("{latest}", latestVersion)
                .replace("{current}", currentVersion);
        MessageUtil.sendMessage(event.getPlayer(), notification);

        for (String line : changelog) {
            MessageUtil.sendMessage(event.getPlayer(), messages.updateChangelogLine.replace("{line}", line));
        }
    }

    private boolean isNewerVersion(String latest, String current) {
        // Strip leading non-numeric prefix (e.g. "dev-26.03.5-abc1234" -> compare numerics)
        String latestClean = latest.replaceAll("[^0-9.]", "");
        String currentClean = current.replaceAll("[^0-9.]", "");

        String[] latestParts = latestClean.split("\\.");
        String[] currentParts = currentClean.split("\\.");
        int maxLen = Math.max(latestParts.length, currentParts.length);

        for (int i = 0; i < maxLen; i++) {
            int latestNum = i < latestParts.length ? parseIntSafe(latestParts[i]) : 0;
            int currentNum = i < currentParts.length ? parseIntSafe(currentParts[i]) : 0;
            if (latestNum > currentNum) return true;
            if (latestNum < currentNum) return false;
        }
        return false;
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
