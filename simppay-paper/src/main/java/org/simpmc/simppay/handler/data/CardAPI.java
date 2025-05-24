package org.simpmc.simppay.handler.data;

import org.simpmc.simppay.handler.PaymentHandler;
import org.simpmc.simppay.handler.card.TSTHandler;
import org.simpmc.simppay.handler.card.TSVHandler;

public enum CardAPI {
    THESIEUTOC(TSTHandler.class),
    THESIEUVIET(TSVHandler.class); //Tích hợp thêm thẻ siêu việt

    public final Class<? extends PaymentHandler> handlerClass;

    CardAPI(Class<? extends PaymentHandler> handlerClass) {
        this.handlerClass = handlerClass;
    }
}