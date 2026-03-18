package org.simpmc.simppay.handler.data;

import org.simpmc.simppay.handler.PaymentHandler;
import org.simpmc.simppay.handler.banking.payos.PayosHandler;
import org.simpmc.simppay.handler.banking.sepay.SepayHandler;
import org.simpmc.simppay.handler.banking.web2m.W2MHandler;

import java.util.function.Supplier;

public enum BankAPI {
    PAYOS(PayosHandler::new),
    WEB2M(W2MHandler::new),
    SEPAY(SepayHandler::new);

    public final Supplier<PaymentHandler> handlerFactory;

    BankAPI(Supplier<PaymentHandler> handlerFactory) {
        this.handlerFactory = handlerFactory;
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
