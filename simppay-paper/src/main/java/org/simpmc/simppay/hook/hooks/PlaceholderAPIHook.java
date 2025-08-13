package org.simpmc.simppay.hook.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.CoinsConfig;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.service.cache.CacheDataService;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final SPPlugin plugin;

    public PlaceholderAPIHook(SPPlugin plugin) {
        this.plugin = plugin;
        register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "simppay";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Typical";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true; //
    }


    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {

        CacheDataService cacheDataService = SPPlugin.getService(CacheDataService.class);

        // get server_total
        // %simppay_server_total%
        if (identifier.equalsIgnoreCase("server_total")) {
            return cacheDataService.getServerTotalValue().toString(); // cached
        }
        // %simppay_server_total_formatted%
        if (identifier.equalsIgnoreCase("server_total_formatted")) {
            return String.format("%,d", cacheDataService.getServerTotalValue().get());
        }
        // %simppay_bank_total_formatted%
        if (identifier.equalsIgnoreCase("bank_total_formatted")) {
            return String.format("%,d", cacheDataService.getBankTotalValue().get());
        }
        // %simppay_card_total_formatted%
        if (identifier.equalsIgnoreCase("card_total_formatted")) {
            return String.format("%,d", cacheDataService.getCardTotalValue().get());
        }

        // %simppay_end_promo%
        if (identifier.equalsIgnoreCase("end_promo")) {
            CoinsConfig coinsConfig = ConfigManager.getInstance().getConfig(CoinsConfig.class);
            MessageConfig messageConfig = ConfigManager.getInstance().getConfig(MessageConfig.class);
            
            // check promo time
            try {
                LocalDateTime promoEndTime = LocalDateTime.parse(coinsConfig.promoEndTimeString, coinsConfig.formatter);
                if (promoEndTime.isBefore(LocalDateTime.now())) {
                    return messageConfig.noPromo;
                } else {
                    return coinsConfig.promoEndTimeString;
                }
            } catch (Exception e) {
                // Parse lỗi thời gian -> coi như không có khuyến mãi
                return messageConfig.noPromo;
            }
        }

        if (player == null) {
            return null;
        }

        UUID uuid = player.getUniqueId();
        if (!cacheDataService.getPlayerTotalValue().containsKey(uuid)) {
            cacheDataService.addPlayerToQueue(uuid);
            return "0";
        }

        // %simppay_total%
        if (identifier.equalsIgnoreCase("total")) {
            return cacheDataService.getPlayerTotalValue().get(uuid).toString();
        }
        // %simppay_total_formatted%
        if (identifier.equalsIgnoreCase("total_formatted")) {
            return String.format("%,d", cacheDataService.getPlayerTotalValue().get(uuid).get());
        }

        return null;
    }

}
