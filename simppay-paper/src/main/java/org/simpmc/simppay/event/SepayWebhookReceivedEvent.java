package org.simpmc.simppay.event;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.handler.banking.sepay.data.SepayWebhookPayload;

/**
 * Event fired when a Sepay webhook is received and validated.
 * Listeners can react to incoming transactions.
 */
@Getter
public class SepayWebhookReceivedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final SepayWebhookPayload payload;

    public SepayWebhookReceivedEvent(SepayWebhookPayload payload) {
        super(true); // async = true
        this.payload = payload;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
}
