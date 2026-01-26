package org.simpmc.simppay.listener.internal.payment;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.banking.SepayConfig;
import org.simpmc.simppay.event.PaymentSuccessEvent;
import org.simpmc.simppay.event.SepayWebhookReceivedEvent;
import org.simpmc.simppay.handler.banking.sepay.data.SepayWebhookPayload;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.service.PaymentService;
import org.simpmc.simppay.util.MessageUtil;

/**
 * Listener for Sepay webhook events.
 * Matches incoming webhook transactions to pending payments using reference codes.
 */
public class SepayWebhookListener implements Listener {
    private final SPPlugin plugin;
    private final PaymentService paymentService;

    public SepayWebhookListener(SPPlugin plugin) {
        this.plugin = plugin;
        this.paymentService = SPPlugin.getService(PaymentService.class);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        MessageUtil.debug("[SepayWebhookListener] Registered");
    }

    @EventHandler
    public void onWebhookReceived(SepayWebhookReceivedEvent event) {
        SepayWebhookPayload payload = event.getPayload();
        
        MessageUtil.debug("[SepayWebhook] Processing webhook - Amount: " + payload.getTransferAmount());
        MessageUtil.debug("[SepayWebhook] Content: " + payload.getContent());

        // Extract reference code from the content/description
        String content = payload.getContent();
        if (content == null || content.isEmpty()) {
            MessageUtil.debug("[SepayWebhook] No content in webhook payload, cannot match payment");
            return;
        }

        // Find pending payment matching by reference code in content
        Payment matchedPayment = findMatchingPayment(content);

        if (matchedPayment == null) {
            MessageUtil.debug("[SepayWebhook] No pending payment found matching content: " + content);
            return;
        }

        MessageUtil.info("[SepayWebhook] Matched transaction to payment: " + matchedPayment.getDetail().getRefID());

        // Verify amount matches
        double expectedAmount = matchedPayment.getDetail().getAmount();
        if (Math.abs(expectedAmount - payload.getTransferAmount()) > 0.01) {
            MessageUtil.warn("[SepayWebhook] Amount mismatch! Expected: " + expectedAmount + 
                    ", Received: " + payload.getTransferAmount() + 
                    ", RefID: " + matchedPayment.getDetail().getRefID());
            // Still process but log warning - the reference code matched
        }

        // Update the payment with webhook transaction ID
        matchedPayment.getDetail().setRefID(String.valueOf(payload.getId()));
        
        // Remove from polling payments since webhook confirmed it
        paymentService.getPollingPayments().remove(matchedPayment.getPaymentID());
        
        // Fire success event on main thread
        plugin.getFoliaLib().getScheduler().runNextTick(task -> {
            Bukkit.getPluginManager().callEvent(new PaymentSuccessEvent(matchedPayment));
        });
    }

    /**
     * Find a pending payment that matches the reference code in webhook content.
     * The content field contains the bank transfer description which should include our reference code.
     * 
     * @param content Webhook content/description field
     * @return Matching payment or null if not found
     */
    private Payment findMatchingPayment(String content) {
        SepayConfig config = ConfigManager.getInstance().getConfig(SepayConfig.class);
        String prefix = config.descriptionPrefix;
        
        // First check polling payments (payments being actively checked)
        for (Payment payment : paymentService.getPollingPayments().values()) {
            if (isReferenceMatch(payment, content, prefix)) {
                return payment;
            }
        }
        
        // Then check regular payments map
        for (Payment payment : paymentService.getPayments().values()) {
            if (isReferenceMatch(payment, content, prefix)) {
                return payment;
            }
        }
        
        return null;
    }

    /**
     * Check if payment reference code is found in webhook content.
     * 
     * @param payment Payment to check
     * @param content Webhook content
     * @param prefix Expected reference code prefix
     * @return true if reference code matches
     */
    private boolean isReferenceMatch(Payment payment, String content, String prefix) {
        String refID = payment.getDetail().getRefID();
        if (refID == null || !refID.startsWith(prefix)) {
            return false;
        }
        
        // Check if the reference code is contained in the webhook content
        // Bank transfer descriptions may have additional text, so we use contains
        return content.toUpperCase().contains(refID.toUpperCase());
    }
}
