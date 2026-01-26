package org.simpmc.simppay.service;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.handler.banking.sepay.data.BankData;
import org.simpmc.simppay.handler.banking.sepay.data.VietQRResponse;
import org.simpmc.simppay.util.GsonUtil;
import org.simpmc.simppay.util.MessageUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for caching bank data from VietQR API.
 * Bank data is fetched asynchronously on plugin startup and cached for the plugin lifetime.
 */
public class BankCacheService implements IService {
    private static final String VIETQR_API_URL = "https://api.vietqr.io/v2/banks";
    
    private final Map<String, BankData> bankCache = new HashMap<>();
    private boolean loaded = false;

    @Override
    public void setup() {
        MessageUtil.info("[BankCache] Starting async fetch from VietQR API...");
        
        // Fetch bank data asynchronously
        fetchBankDataAsync().thenAccept(success -> {
            if (success) {
                MessageUtil.info("[BankCache] Successfully loaded " + bankCache.size() + " banks");
                loaded = true;
            } else {
                MessageUtil.warn("[BankCache] Failed to fetch bank data from VietQR API");
            }
        }).exceptionally(throwable -> {
            MessageUtil.warn("[BankCache] Error fetching bank data: " + throwable.getMessage());
            throwable.printStackTrace();
            return null;
        });
    }

    @Override
    public void shutdown() {
        bankCache.clear();
        loaded = false;
        MessageUtil.debug("[BankCache] Cache cleared");
    }

    /**
     * Fetch bank data from VietQR API asynchronously
     * 
     * @return CompletableFuture that completes with true if successful
     */
    private CompletableFuture<Boolean> fetchBankDataAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OkHttpClient client = new OkHttpClient.Builder().build();
                
                Request request = new Request.Builder()
                        .url(VIETQR_API_URL)
                        .get()
                        .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        MessageUtil.warn("[BankCache] HTTP error: " + response.code());
                        return false;
                    }
                    
                    String responseBody = response.body().string();
                    VietQRResponse vietQRResponse = GsonUtil.getGson().fromJson(responseBody, VietQRResponse.class);
                    
                    if (vietQRResponse == null || !vietQRResponse.isSuccess()) {
                        MessageUtil.warn("[BankCache] Invalid response from VietQR API");
                        return false;
                    }
                    
                    if (!vietQRResponse.hasData()) {
                        MessageUtil.warn("[BankCache] No bank data in response");
                        return false;
                    }
                    
                    // Cache banks with lowercase keys for case-insensitive lookup
                    synchronized (bankCache) {
                        bankCache.clear();
                        for (BankData bank : vietQRResponse.getData()) {
                            if (bank.getShortName() != null) {
                                String key = bank.getShortName().toLowerCase();
                                bankCache.put(key, bank);
                                MessageUtil.debug("[BankCache] Cached: " + bank.getShortName() + " -> BIN: " + bank.getBin());
                            }
                        }
                    }
                    
                    return true;
                }
            } catch (IOException e) {
                MessageUtil.warn("[BankCache] IOException: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Get bank data by bank name (case-insensitive)
     * 
     * @param bankName Bank name to search for (matches shortName field)
     * @return BankData if found, null otherwise
     */
    public BankData getBankByName(String bankName) {
        if (bankName == null || bankName.isEmpty()) {
            return null;
        }
        
        String key = bankName.toLowerCase();
        synchronized (bankCache) {
            return bankCache.get(key);
        }
    }

    /**
     * Check if bank cache is loaded
     * 
     * @return true if cache is loaded
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Get the number of cached banks
     * 
     * @return number of banks in cache
     */
    public int getCacheSize() {
        synchronized (bankCache) {
            return bankCache.size();
        }
    }
}
