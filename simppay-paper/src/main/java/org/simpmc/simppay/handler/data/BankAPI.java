package org.simpmc.simppay.handler.data;

import org.simpmc.simppay.handler.BankHandler;
import org.simpmc.simppay.handler.banking.payos.PayosHandler;
import org.simpmc.simppay.handler.banking.sepay.SepayHandler;
import org.simpmc.simppay.handler.banking.web2m.W2MHandler;

public enum BankAPI {
    PAYOS(PayosHandler.class),
    WEB2M(W2MHandler.class),
    SEPAY(SepayHandler.class); // Phase 4: Sepay integration

    public final Class<? extends BankHandler> handlerClass;

    BankAPI(Class<? extends BankHandler> handlerClass) {
        this.handlerClass = handlerClass;
    }

    public static String getValues() {
        StringBuilder values = new StringBuilder();
        for (BankAPI api : BankAPI.values()) {
            if (!values.isEmpty()) {
                values.append(", ");
            }
            values.append(api.name());
        }
        return values.toString();
    }
}
