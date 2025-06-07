package org.simpmc.simppay.menu.card.anvil;

import lombok.Getter;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.config.types.menu.card.anvil.CardSerialMenuConfig;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.util.MessageUtil;

import java.util.Arrays;
import java.util.Collections;
@Getter
public class CardSerialInput {
    private final Object initialData;
    private final Player player;
    public CardSerialInput(Player p, Object initialData) {
        this.initialData = initialData;
        this.player = p;
        openAnvil();
    }

    public void openAnvil() {
        CardSerialMenuConfig menuConfig = SPPlugin.getInstance().getConfigManager().getConfig(CardSerialMenuConfig.class);
        new AnvilGUI.Builder()
                .jsonTitle(JSONComponentSerializer.json().serialize(MessageUtil.getComponentParsed(menuConfig.title, player)))
                .itemLeft(menuConfig.item.getItemStack(player))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    String serial = stateSnapshot.getText();
                    if (serial == null || serial.isEmpty() || !serial.matches("\\d+")) {
                        MessageConfig config = SPPlugin.getInstance().getConfigManager().getConfig(MessageConfig.class);
                        MessageUtil.sendMessage(player, config.invalidParam);
                        return Collections.emptyList();
                    } else {
                        // correct input, process it
                        return Arrays.asList(
                                AnvilGUI.ResponseAction.close(),
                                AnvilGUI.ResponseAction.run(() -> {
                                    // Handle the input here, e.g., save it to the card or database
                                    CardDetail detail = (CardDetail) this.getInitialData();
                                    detail.setSerial(serial);
                                    new CardPINInput(player, detail);
                                })
                        );
                    }
                })
                .mainThreadExecutor(run -> SPPlugin.getInstance().getFoliaLib().getScheduler().runAtEntity(player, task -> {
                    run.run();
                }))
                .plugin(SPPlugin.getInstance())
                .open(player);
    }

}
