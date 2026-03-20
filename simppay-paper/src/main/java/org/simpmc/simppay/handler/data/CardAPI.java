package org.simpmc.simppay.handler.data;

import org.simpmc.simppay.config.types.card.*;
import org.simpmc.simppay.data.card.CardType;
import org.simpmc.simppay.handler.PaymentHandler;
import org.simpmc.simppay.handler.card.nencer.NencerCardHandler;

import java.util.Map;
import java.util.function.Supplier;

public enum CardAPI {
    CARD2K(() -> new NencerCardHandler(
            "https://card2k.com/chargingws/v2",
            Card2KConfig.class, "Card2K", Map.of())),
    THESIEURECOM(() -> new NencerCardHandler(
            "https://thesieure.com/chargingws/v2",
            ThesieureConfig.class, "TSR", Map.of())),
    GT1SCOM(() -> new NencerCardHandler(
            "https://gachthe1s.com/chargingws/v2",
            Gachthe1sConfig.class, "GT1S",
            Map.of(CardType.VIETNAMOBILE, "VNMB", CardType.GARENA, "GARENA2"))),
    DOITHE1SVN(() -> new NencerCardHandler(
            "https://doithe1s.vn/chargingws/v2",
            Doithe1sConfig.class, "DT1S", Map.of()));

    public final Supplier<PaymentHandler> handlerFactory;

    CardAPI(Supplier<PaymentHandler> handlerFactory) {
        this.handlerFactory = handlerFactory;
    }
}
