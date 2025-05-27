package org.simpmc.simppay.config.types.data.menu;

import de.exlll.configlib.Configuration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.simpmc.simppay.util.MessageUtil;

import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Configuration
@Builder
public class DisplayItem implements Cloneable {
    private Material material;
    private String name;
    private List<String> lores;
    private int customModelData;
    private int amount;
    private RoleType role;

    public DisplayItem replaceStringInName(String str, String replacement) {
        if (name != null) {
            name = name.replace(str, replacement);
        }
        return this;
    }

    public DisplayItem replaceStringInName(String placeholder, Function<String, String> replacement) {
        if (name != null) {
            name = name.replace(placeholder, replacement.apply(placeholder));
        }
        return this;
    }


    public ItemStack getItemStack(Player player) {
        ItemStack itemStack = new org.bukkit.inventory.ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.displayName(MessageUtil.getComponentParsed(name, player));
            }
            if (lores != null && !lores.isEmpty()) {
                meta.lore(lores.stream()
                        .map(lore -> MessageUtil.getComponentParsed(lore, player))
                        .toList());
            }
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    public ItemStack getItemStack() {
        ItemStack itemStack = new org.bukkit.inventory.ItemStack(material, amount);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (name != null && !name.isEmpty()) {
                meta.displayName(MessageUtil.getComponentParsed(name, null));
            }
            if (lores != null && !lores.isEmpty()) {
                meta.lore(lores.stream()
                        .map(lore -> MessageUtil.getComponentParsed(lore, null))
                        .toList());
            }
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }


    @Override
    public DisplayItem clone() {
        try {
            DisplayItem clone = (DisplayItem) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}