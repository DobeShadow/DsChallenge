package io.bukkitcode.dschallenge.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 物品构建工具。
 */
public final class ItemBuilder {

    private ItemBuilder() {
    }

    /**
     * 快速构建一个带名称与 lore 的物品
     */
    public static ItemStack build(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(color(name));
            }
            if (lore != null && lore.length > 0) {
                List<String> lines = new ArrayList<>();
                for (String line : lore) {
                    lines.add(color(line));
                }
                meta.setLore(lines);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * 构建带 lore 列表的物品
     */
    public static ItemStack build(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.setDisplayName(color(name));
            }
            if (lore != null && !lore.isEmpty()) {
                List<String> lines = new ArrayList<>();
                for (String line : lore) {
                    lines.add(color(line));
                }
                meta.setLore(lines);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /**
     * 构建一个纯装饰用的玻璃板
     */
    public static ItemStack pane(Material material) {
        return build(material, " ", Arrays.asList());
    }

    /**
     * 颜色代码转换
     */
    public static String color(String text) {
        return text == null ? "" : text.replace('&', '§');
    }
}
