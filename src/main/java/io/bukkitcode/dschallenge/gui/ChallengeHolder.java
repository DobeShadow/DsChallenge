package io.bukkitcode.dschallenge.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * 挑战 GUI 的 Holder，用于区分本插件的界面。
 */
public class ChallengeHolder implements InventoryHolder {

    /** 界面类型 */
    public enum Type {
        /** 入口界面：开启挑战按钮 */
        ENTRY,
        /** 抽奖动画界面 */
        ROLLING,
        /** 结果查看界面 */
        RESULT
    }

    private final Type type;
    private Inventory inventory;

    public ChallengeHolder(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
