package org.simpmc.simppay.handler.data;

import org.simpmc.simppay.handler.CoinsHandler;
import org.simpmc.simppay.handler.coins.CoinsEngineHandler;
import org.simpmc.simppay.handler.coins.DefaultCoinsHandler;
import org.simpmc.simppay.handler.coins.PlayerPointsHandler;

import java.util.function.Supplier;

public enum CoinsAPI {
    PLAYERPOINTS(PlayerPointsHandler::new),
    NONE(DefaultCoinsHandler::new),
    COINSENGINE(CoinsEngineHandler::new);

    public final Supplier<CoinsHandler> handlerFactory;

    CoinsAPI(Supplier<CoinsHandler> handlerFactory) {
        this.handlerFactory = handlerFactory;
    }
}
