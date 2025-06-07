package org.simpmc.simppay.service.cache;

import lombok.Getter;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.database.entities.SPPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

// TODO: Rewrite this class to use a more efficient data structure, use generics stuff thingy, or libraries
// TODO: Fix this colossal class, it is a mess !!!!!!
@Getter
public class CacheDataService {

    private static CacheDataService instance;
    private final ConcurrentLinkedQueue<UUID> playerQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerTotalValue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerDailyTotalValue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerWeeklyTotalValue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerMonthlyTotalValue = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, AtomicLong> playerYearlyTotalValue = new ConcurrentHashMap<>();
    private final AtomicLong serverTotalValue = new AtomicLong(0);
    private final AtomicLong serverDailyTotalValue = new AtomicLong(0);
    private final AtomicLong serverWeeklyTotalValue = new AtomicLong(0);
    private final AtomicLong serverMonthlyTotalValue = new AtomicLong(0);
    private final AtomicLong serverYearlyTotalValue = new AtomicLong(0);
    private final AtomicLong cardTotalValue = new AtomicLong(0);
    private final AtomicLong bankTotalValue = new AtomicLong(0);

    private CacheDataService() {
        SPPlugin.getInstance().getFoliaLib().getScheduler().runTimerAsync(task -> processQueue(), 1, 20L);
        // Player cache are updated once when player first join
        // and then on PaymentSuccessEvent given there is a player

        // Server cache are updated once when server start
        // and then on PaymentSuccessEvent
    }

    public static synchronized CacheDataService getInstance() {
        if (instance == null) {
            instance = new CacheDataService();
        }
        return instance;
    }

    public void clearAllCache() {
        playerTotalValue.clear();
        playerDailyTotalValue.clear();
        playerWeeklyTotalValue.clear();
        playerMonthlyTotalValue.clear();
        playerYearlyTotalValue.clear();
        serverTotalValue.set(0);
        serverDailyTotalValue.set(0);
        serverWeeklyTotalValue.set(0);
        serverMonthlyTotalValue.set(0);
        serverYearlyTotalValue.set(0);
        cardTotalValue.set(0);
        bankTotalValue.set(0);
    }

    public void clearPlayerCache(UUID playerUUID) {
        playerTotalValue.remove(playerUUID);
        playerDailyTotalValue.remove(playerUUID);
        playerWeeklyTotalValue.remove(playerUUID);
        playerMonthlyTotalValue.remove(playerUUID);
        playerYearlyTotalValue.remove(playerUUID);
    }

    // Process queue conurrently
    public void addPlayerToQueue(UUID playerUUID) {
        playerQueue.add(playerUUID);
    }

    public void processQueue() {
        while (!playerQueue.isEmpty()) {
            SPPlugin plugin = SPPlugin.getInstance();
            UUID playerUUID = playerQueue.poll();

            SPPlayer player = plugin.getDatabaseService().getPlayerService().findByUuid(playerUUID);
            if (player == null) {
                // Player not found, re-add to queue
                playerQueue.add(playerUUID);
                continue;
            }

            double totalValue = plugin.getDatabaseService().getPaymentLogService().getPlayerTotalAmount(player);
            if (totalValue != 0) {
                playerTotalValue.putIfAbsent(playerUUID, new AtomicLong((long) totalValue));
                updatePlayerTimedValues(playerUUID);
            }
        }
    }

    public void updateServerDataCache() {
        SPPlugin plugin = SPPlugin.getInstance();
        serverTotalValue.set(plugin.getDatabaseService().getPaymentLogService().getEntireServerAmount());
        serverDailyTotalValue.set(plugin.getDatabaseService().getPaymentLogService().getEntireServerDailyAmount());
        serverWeeklyTotalValue.set(plugin.getDatabaseService().getPaymentLogService().getEntireServerWeeklyAmount());
        serverMonthlyTotalValue.set(plugin.getDatabaseService().getPaymentLogService().getEntireServerMonthlyAmount());
        serverYearlyTotalValue.set(plugin.getDatabaseService().getPaymentLogService().getEntireServerYearlyAmount());
        cardTotalValue.set(plugin.getDatabaseService().getPaymentLogService().getEntireServerCardAmount());
        bankTotalValue.set(plugin.getDatabaseService().getPaymentLogService().getEntireServerBankAmount());
    }

    private void updatePlayerTimedValues(UUID playerUUID) {
        SPPlugin plugin = SPPlugin.getInstance();
        SPPlayer player = plugin.getDatabaseService().getPlayerService().findByUuid(playerUUID);

        playerDailyTotalValue.compute(playerUUID, (k, v) -> {
            if (v == null) {
                return new AtomicLong(plugin.getDatabaseService().getPaymentLogService().getPlayerDailyAmount(player));
            }
            v.set(plugin.getDatabaseService().getPaymentLogService().getPlayerDailyAmount(player));
            return v;
        });

        playerWeeklyTotalValue.compute(playerUUID, (k, v) -> {
            if (v == null) {
                return new AtomicLong(plugin.getDatabaseService().getPaymentLogService().getPlayerWeeklyAmount(player));
            }
            v.set(plugin.getDatabaseService().getPaymentLogService().getPlayerWeeklyAmount(player));
            return v;
        });
        playerMonthlyTotalValue.compute(playerUUID, (k, v) -> {
            if (v == null) {
                return new AtomicLong(plugin.getDatabaseService().getPaymentLogService().getPlayerMonthlyAmount(player));
            }
            v.set(plugin.getDatabaseService().getPaymentLogService().getPlayerMonthlyAmount(player));
            return v;
        });
        playerYearlyTotalValue.compute(playerUUID, (k, v) -> {
            if (v == null) {
                return new AtomicLong(plugin.getDatabaseService().getPaymentLogService().getPlayerYearlyAmount(player));
            }
            v.set(plugin.getDatabaseService().getPaymentLogService().getPlayerYearlyAmount(player));
            return v;
        });

    }
}
