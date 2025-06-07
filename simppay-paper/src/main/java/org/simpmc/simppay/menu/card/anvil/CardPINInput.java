package org.simpmc.simppay.menu.card.anvil;

import lombok.Getter;
import net.kyori.adventure.text.serializer.json.JSONComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.entity.Player;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.MessageConfig;
import org.simpmc.simppay.config.types.menu.card.anvil.CardPinMenuConfig;
import org.simpmc.simppay.config.types.menu.card.anvil.CardSerialMenuConfig;
import org.simpmc.simppay.data.PaymentStatus;
import org.simpmc.simppay.model.Payment;
import org.simpmc.simppay.model.detail.CardDetail;
import org.simpmc.simppay.util.MessageUtil;
import org.simpmc.simppay.util.SoundUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

@Getter
public class CardPINInput {
    private final Object initialData;
    private final Player player;
    public CardPINInput(Player p, Object initialData) {
        this.initialData = initialData;
        this.player = p;
        openAnvil();
    }

    public void openAnvil() {
        CardPinMenuConfig menuConfig = SPPlugin.getInstance().getConfigManager().getConfig(CardPinMenuConfig.class);
        new AnvilGUI.Builder()
                .jsonTitle(JSONComponentSerializer.json().serialize(MessageUtil.getComponentParsed(menuConfig.title, player)))
                .itemLeft(menuConfig.item.getItemStack(player))
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) {
                        return Collections.emptyList();
                    }
                    String pin = stateSnapshot.getText();
                    if (pin == null || pin.isEmpty() || !pin.matches("\\d+")) {
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
                                    detail.setPin(pin);
                                    UUID uuid = UUID.nameUUIDFromBytes(detail.serial.getBytes()); // payment uuid is based on serial number of the card
                                    Payment payment = new Payment(uuid, player.getUniqueId(), detail);

                                    MessageConfig messageConfig = ConfigManager.getInstance().getConfig(MessageConfig.class);

                                    if (SPPlugin.getInstance().getPaymentService().getPayments().containsKey(payment.getPaymentID())) {
                                        MessageUtil.sendMessage(player, messageConfig.pendingCard);
                                        SoundUtil.sendSound(player, messageConfig.soundEffect.get(PaymentStatus.PENDING).toSound());
                                        return;
                                    }

                                    PaymentStatus status = SPPlugin.getInstance().getPaymentService().sendCard(payment);

                                    if (status == PaymentStatus.FAILED) {
                                        MessageUtil.sendMessage(player, messageConfig.failedCard);
                                        SoundUtil.sendSound(player, messageConfig.soundEffect.get(PaymentStatus.FAILED).toSound());
                                    }
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
