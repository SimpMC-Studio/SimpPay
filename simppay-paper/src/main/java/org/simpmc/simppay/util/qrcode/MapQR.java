package org.simpmc.simppay.util.qrcode;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import org.bukkit.entity.Player;
import org.bukkit.map.MapPalette;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.BankingConfig;

import java.util.Arrays;

public class MapQR {
    public static void sendPacketQRMap(byte[] mapBytes, Player player) {
        // Create a new map data packet
        WrapperPlayServerMapData mapDataPacket = new WrapperPlayServerMapData(
                999,
                (byte) 0,
                false,
                true,
                null,
                128,
                128,
                0,
                0,
                mapBytes
        );

        PacketEvents.getAPI().getPlayerManager().sendPacket(player, mapDataPacket);

        // Send the map item with fake data to player
        // TODO: Check server verison => create respective packet, nbt or component
        NBTCompound compound = new NBTCompound();
        compound.setTag("map", new NBTInt(999));
        int slotIndex = ConfigManager.getInstance().getConfig(BankingConfig.class).showQrOnLeftHand ? 45 : 36; // 45 for left hand, 36 for main hand
        WrapperPlayServerSetSlot setSlotPacket = new WrapperPlayServerSetSlot(
                0,
                0,
                slotIndex,
                ItemStack.builder().type(ItemTypes.FILLED_MAP).nbt(compound).component(ComponentTypes.MAP_ID, 999).build()
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, setSlotPacket);
    }

}
