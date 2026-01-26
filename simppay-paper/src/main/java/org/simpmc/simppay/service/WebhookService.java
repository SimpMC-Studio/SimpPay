package org.simpmc.simppay.service;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.banking.SepayConfig;
import org.simpmc.simppay.event.SepayWebhookReceivedEvent;
import org.simpmc.simppay.handler.banking.sepay.data.SepayWebhookPayload;
import org.simpmc.simppay.util.GsonUtil;
import org.simpmc.simppay.util.MessageUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * HTTP server for receiving Sepay webhooks.
 * Listens on configured port and validates incoming webhook requests.
 */
public class WebhookService implements IService {
    private HttpServer server;
    private SepayConfig config;

    @Override
    public void setup() {
        config = ConfigManager.getInstance().getConfig(SepayConfig.class);

        if (config.webhookApiKey == null || config.webhookApiKey.equals("YOUR_WEBHOOK_API_KEY_HERE")) {
            MessageUtil.warn("[WebhookService] Webhook API key is not configured - webhooks will not work!");
            return;
        }

        try {
            // Create HTTP server
            server = HttpServer.create(new InetSocketAddress(config.webhookPort), 0);
            
            // Register webhook endpoint
            server.createContext(config.webhookPath, new SepayWebhookHandler());
            
            // Use virtual thread executor for better performance
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            
            // Start server
            server.start();
            
            MessageUtil.info("[WebhookService] Server started on port " + config.webhookPort);
            MessageUtil.info("[WebhookService] Webhook URL: http://YOUR_SERVER_IP:" + config.webhookPort + config.webhookPath);
        } catch (IOException e) {
            MessageUtil.warn("[WebhookService] Failed to start HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.stop(0);
            MessageUtil.info("[WebhookService] HTTP server stopped");
        }
    }

    /**
     * HTTP handler for Sepay webhook endpoint
     */
    private class SepayWebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Only accept POST requests
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "{\"success\": false, \"error\": \"Method not allowed\"}");
                return;
            }

            try {
                // Validate API key authentication
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                if (authHeader == null || !authHeader.startsWith("Apikey")) {
                    MessageUtil.debug("[Webhook] Missing or invalid Authorization header");
                    sendResponse(exchange, 401, "{\"success\": false, \"error\": \"Unauthorized\"}");
                    return;
                }

                String providedKey = authHeader.substring(7); // Remove "APIkey_" prefix
                if (!providedKey.equals(config.webhookApiKey)) {
                    MessageUtil.debug("[Webhook] Invalid API key");
                    sendResponse(exchange, 403, "{\"success\": false, \"error\": \"Forbidden\"}");
                    return;
                }

                // Read and parse request body
                String requestBody = readRequestBody(exchange.getRequestBody());
                MessageUtil.debug("[Webhook] Received payload: " + requestBody);

                SepayWebhookPayload payload;
                try {
                    payload = GsonUtil.getGson().fromJson(requestBody, SepayWebhookPayload.class);
                } catch (JsonSyntaxException e) {
                    MessageUtil.warn("[Webhook] Invalid JSON payload: " + e.getMessage());
                    sendResponse(exchange, 400, "{\"success\": false, \"error\": \"Invalid JSON\"}");
                    return;
                }

                if (payload == null) {
                    MessageUtil.warn("[Webhook] Null payload after parsing");
                    sendResponse(exchange, 400, "{\"success\": false, \"error\": \"Invalid payload\"}");
                    return;
                }

                // Validate it's an incoming transfer
                if (!payload.isIncomingTransfer()) {
                    MessageUtil.debug("[Webhook] Ignoring non-incoming transfer");
                    sendResponse(exchange, 200, "{\"success\": true, \"message\": \"Ignored non-incoming transfer\"}");
                    return;
                }

                MessageUtil.info("[Webhook] Received transaction: ID=" + payload.getId() + 
                               ", Amount=" + payload.getTransferAmount() + 
                               ", Content=" + payload.getContent());

                // Fire event on main thread
                SPPlugin.getInstance().getFoliaLib().getScheduler().runAsync(task -> {
                    Bukkit.getPluginManager().callEvent(new SepayWebhookReceivedEvent(payload));
                });

                // Send success response
                sendResponse(exchange, 200, "{\"success\": true}");

            } catch (Exception e) {
                MessageUtil.warn("[Webhook] Error processing webhook: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "{\"success\": false, \"error\": \"Internal server error\"}");
            }
        }

        /**
         * Read request body as string
         */
        private String readRequestBody(InputStream inputStream) throws IOException {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        /**
         * Send HTTP response
         */
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
