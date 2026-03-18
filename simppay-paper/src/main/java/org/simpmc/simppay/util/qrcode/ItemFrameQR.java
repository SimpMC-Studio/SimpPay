package org.simpmc.simppay.util.qrcode;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTInt;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerMapData;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ItemFrameQR {

    private static final AtomicInteger ENTITY_ID_COUNTER = new AtomicInteger(Integer.MAX_VALUE - 200_000);

    private final int entityId;
    private final Player player;
    private final AtomicBoolean destroyed = new AtomicBoolean(false);

    public ItemFrameQR(Player player, byte[] mapBytes) {
        this.player = player;
        this.entityId = ENTITY_ID_COUNTER.getAndDecrement();

        // Send map data so client knows what map 999 looks like
        WrapperPlayServerMapData mapDataPacket = new WrapperPlayServerMapData(
                999, (byte) 0, false, true, null, 128, 128, 0, 0, mapBytes
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, mapDataPacket);

        // Compute spawn location: eye position + facing direction * 2 + slight down offset
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().multiply(2.0);
        Location spawnLocation = eyeLocation.clone().add(direction).add(0, -0.5, 0);

        int facingData = getItemFrameFacing(player.getLocation().getYaw());

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                entityId,
                Optional.of(UUID.randomUUID()),
                EntityTypes.ITEM_FRAME,
                new Vector3d(spawnLocation.getX(), spawnLocation.getY(), spawnLocation.getZ()),
                0f, 0f, 0f,
                facingData,
                null
        );
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, spawnPacket);

        // Set the filled_map item inside the frame
        NBTCompound compound = new NBTCompound();
        compound.setTag("map", new NBTInt(999));
        ItemStack mapItem = ItemStack.builder()
                .type(ItemTypes.FILLED_MAP)
                .nbt(compound)
                .component(ComponentTypes.MAP_ID, 999)
                .build();

        List<EntityData<?>> metadata = new ArrayList<>();
        metadata.add(new EntityData<>(8, EntityDataTypes.OPTIONAL_ITEMSTACK, Optional.of(mapItem))); // item in frame
        metadata.add(new EntityData<>(9, EntityDataTypes.BYTE, (byte) 0));      // rotation

        WrapperPlayServerEntityMetadata metaPacket = new WrapperPlayServerEntityMetadata(entityId, metadata);
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, metaPacket);
    }

    public void destroy() {
        if (destroyed.compareAndSet(false, true)) {
            WrapperPlayServerDestroyEntities destroyPacket = new WrapperPlayServerDestroyEntities(entityId);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, destroyPacket);
        }
    }

    /**
     * Returns the item frame facing direction (object data) based on the player's yaw.
     * The frame will face the player (opposite of player's looking direction).
     * Bukkit yaw: 0=south, 90=west, 180=north, 270=east
     * Item frame directions: 2=north, 3=south, 4=west, 5=east
     */
    private static int getItemFrameFacing(float yaw) {
        float normalized = ((yaw % 360) + 360) % 360;
        if (normalized >= 315 || normalized < 45) return 2;  // player faces south → frame faces north
        if (normalized < 135) return 5;                       // player faces west  → frame faces east
        if (normalized < 225) return 3;                       // player faces north → frame faces south
        return 4;                                             // player faces east  → frame faces west
    }
}
