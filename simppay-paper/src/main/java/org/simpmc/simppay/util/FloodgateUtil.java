package org.simpmc.simppay.util;

import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.Form;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class FloodgateUtil {
    public static boolean enableFloodgate = false;

    public static void sendForm(UUID uuid, Form form) {
        if (enableFloodgate) {
            FloodgateApi.getInstance().sendForm(uuid, form);
        }
    }

    public static boolean isFloodgateUUID(UUID uuid) {
        return uuid.getMostSignificantBits() == 0;
    }
}
