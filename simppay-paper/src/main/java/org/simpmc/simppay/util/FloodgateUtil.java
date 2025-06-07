package org.simpmc.simppay.util;

import org.geysermc.cumulus.form.Form;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class FloodgateUtil {

    public static void sendForm(UUID uuid, Object form) {
        FloodgateApi.getInstance().sendForm(uuid, (Form) form);

    }
}
