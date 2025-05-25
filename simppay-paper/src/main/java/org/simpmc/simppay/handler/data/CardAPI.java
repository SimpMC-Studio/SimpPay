package org.simpmc.simppay.handler.data;

import lombok.Getter;
import org.simpmc.simppay.handler.PaymentHandler;
import org.simpmc.simppay.handler.card.TSTHandler;
import org.simpmc.simppay.handler.card.TSVHandler;

@Getter
public enum CardAPI {
    THESIEUTOC(TSTHandler.class),
    THESIEUVIET(TSVHandler.class);

    public final Class<? extends PaymentHandler> handlerClass;

    CardAPI(Class<? extends PaymentHandler> handlerClass) {
        this.handlerClass = handlerClass;
    }

}