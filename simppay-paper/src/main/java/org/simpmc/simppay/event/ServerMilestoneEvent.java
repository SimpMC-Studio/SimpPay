package org.simpmc.simppay.event;

import lombok.AllArgsConstructor;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.config.types.data.MilestoneConfig;

@AllArgsConstructor
public class ServerMilestoneEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    public final MilestoneConfig milestoneConfig;

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}