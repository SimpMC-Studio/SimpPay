package org.simpmc.simppay.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.DiscordConfig;
import org.simpmc.simppay.config.types.DiscordConfig.EventConfig;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.event.MilestoneCompleteEvent;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.util.MessageUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DiscordService implements IService, Listener {

    private OkHttpClient client;

    @Override
    public void setup() {
        client = new OkHttpClient();
    }

    @Override
    public void shutdown() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPaymentSuccess(PaymentSuccessEvent event) {
        DiscordConfig config = ConfigManager.getInstance().getConfig(DiscordConfig.class);
        if (!config.enabled) return;

        UUID playerUUID = event.getPlayerUUID();
        String playerName = Objects.requireNonNullElse(Bukkit.getOfflinePlayer(playerUUID).getName(), playerUUID.toString());

        if (event.getPaymentType() == PaymentType.CARD) {
            CardDetail cardDetail = (CardDetail) event.getPaymentDetail();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{player}", playerName);
            placeholders.put("{amount}", String.valueOf((long) event.getAmount()));
            placeholders.put("{trueAmount}", String.valueOf((long) event.getTrueAmount()));
            placeholders.put("{gateway}", cardDetail.getType().toString());
            placeholders.put("{paymentId}", event.getPaymentID().toString());
            placeholders.put("{paymentType}", "CARD");
            dispatch(config.cardPayment, placeholders);
        } else if (event.getPaymentType() == PaymentType.BANKING) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("{player}", playerName);
            placeholders.put("{amount}", String.valueOf((long) event.getAmount()));
            placeholders.put("{trueAmount}", String.valueOf((long) event.getAmount()));
            placeholders.put("{gateway}", "BANKING");
            placeholders.put("{paymentId}", event.getPaymentID().toString());
            placeholders.put("{paymentType}", "BANKING");
            dispatch(config.bankPayment, placeholders);
        }
    }

    @EventHandler
    public void onMilestoneComplete(MilestoneCompleteEvent event) {
        DiscordConfig config = ConfigManager.getInstance().getConfig(DiscordConfig.class);
        if (!config.enabled) return;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{milestoneType}", event.getType().name());
        placeholders.put("{milestoneAmount}", String.valueOf(event.getMilestoneConfig().amount));
        placeholders.put("{currentAmount}", String.valueOf(event.getCurrentAmount()));

        if (event.getPlayerUUID() == null) {
            dispatch(config.serverMilestone, placeholders);
        } else {
            String playerName = Objects.requireNonNullElse(
                    Bukkit.getOfflinePlayer(event.getPlayerUUID()).getName(),
                    event.getPlayerUUID().toString()
            );
            placeholders.put("{player}", playerName);
            dispatch(config.playerMilestone, placeholders);
        }
    }

    private void dispatch(EventConfig eventConfig, Map<String, String> placeholders) {
        if (!eventConfig.enabled || eventConfig.webhookUrl.isBlank()) return;

        String payload = buildPayload(eventConfig, placeholders);

        SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
            RequestBody body = RequestBody.create(payload, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(eventConfig.webhookUrl)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    MessageUtil.warn("[DiscordService] Webhook returned non-2xx: " + response.code());
                }
            } catch (IOException e) {
                MessageUtil.warn("[DiscordService] Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String buildPayload(EventConfig eventConfig, Map<String, String> placeholders) {
        JsonObject root = new JsonObject();
        if (!eventConfig.botUsername.isBlank()) {
            root.addProperty("username", eventConfig.botUsername);
        }
        if (!eventConfig.avatarUrl.isBlank()) {
            root.addProperty("avatar_url", eventConfig.avatarUrl);
        }

        JsonObject embed = new JsonObject();
        embed.addProperty("title", applyPlaceholders(eventConfig.embed.title, placeholders));
        embed.addProperty("description", applyPlaceholders(eventConfig.embed.description, placeholders));
        embed.addProperty("color", parseColor(eventConfig.embed.color));

        if (!eventConfig.embed.thumbnailUrl.isBlank()) {
            JsonObject thumbnail = new JsonObject();
            thumbnail.addProperty("url", eventConfig.embed.thumbnailUrl);
            embed.add("thumbnail", thumbnail);
        }

        if (!eventConfig.embed.footerText.isBlank()) {
            JsonObject footer = new JsonObject();
            footer.addProperty("text", applyPlaceholders(eventConfig.embed.footerText, placeholders));
            embed.add("footer", footer);
        }

        if (eventConfig.embed.fields != null && !eventConfig.embed.fields.isEmpty()) {
            JsonArray fields = new JsonArray();
            for (DiscordConfig.FieldConfig field : eventConfig.embed.fields) {
                JsonObject fieldObj = new JsonObject();
                fieldObj.addProperty("name", applyPlaceholders(field.name, placeholders));
                fieldObj.addProperty("value", applyPlaceholders(field.value, placeholders));
                fieldObj.addProperty("inline", field.inline);
                fields.add(fieldObj);
            }
            embed.add("fields", fields);
        }

        JsonArray embeds = new JsonArray();
        embeds.add(embed);
        root.add("embeds", embeds);

        return root.toString();
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) return "";
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace(entry.getKey(), entry.getValue());
        }
        return text;
    }

    private int parseColor(String hex) {
        try {
            String clean = hex.startsWith("#") ? hex.substring(1) : hex;
            return Integer.parseInt(clean, 16);
        } catch (NumberFormatException e) {
            return 5763719; // default green
        }
    }
}
