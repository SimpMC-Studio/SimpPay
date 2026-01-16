package org.simpmc.simppay.handler;

import java.util.UUID;

public interface ICoins {
    void take(UUID uuid, int amount);

    int look(UUID uuid);

    void give(UUID uuid, int amount);

    void set(UUID uuid, int amount);
}
