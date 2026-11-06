package io.bukkitcode.dschallenge.gui;

import io.bukkitcode.dschallenge.DsChallenge;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * GUI 交互监听器。
 * 处理点击、拖拽与关闭事件。
 */
public class GuiListener implements Listener {

    private final DsChallenge plugin;
    private final ChallengeGui gui;

    public GuiListener(DsChallenge plugin, ChallengeGui gui) {
        this.plugin = plugin;
        this.gui = gui;
    }

    /**
     * 点击事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ChallengeHolder)) {
            return;
        }
        // 本插件界面禁止任何物品移动
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        ChallengeHolder challengeHolder = (ChallengeHolder) holder;

        if (challengeHolder.getType() == ChallengeHolder.Type.ENTRY) {
            // 点击中间按钮开启抽奖
            if (event.getRawSlot() == 4) {
                player.closeInventory();
                // 下一 tick 打开动画界面，避免与关闭事件冲突
                plugin.getServer().getScheduler().runTask(plugin, () -> gui.startRolling(player));
            }
        }
    }

    /**
     * 拖拽事件：本插件界面禁止拖拽
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ChallengeHolder) {
            event.setCancelled(true);
        }
    }

    /**
     * 关闭事件：抽奖动画期间禁止关闭
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ChallengeHolder)) {
            return;
        }
        ChallengeHolder challengeHolder = (ChallengeHolder) holder;
        if (challengeHolder.getType() != ChallengeHolder.Type.ROLLING) {
            return;
        }
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getPlayer();
        // 正常结束动画导致的关闭，不再重新打开
        if (gui.consumeFinishing(player)) {
            return;
        }
        // 玩家手动关闭：强制重新打开界面
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                player.openInventory(event.getInventory());
            }
        });
    }
}
