package io.bukkitcode.dschallenge.listener;

import io.bukkitcode.dschallenge.challenge.Challenge;
import io.bukkitcode.dschallenge.challenge.ChallengeType;
import io.bukkitcode.dschallenge.data.PlayerData;
import io.bukkitcode.dschallenge.manager.ChallengeManager;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;

/**
 * 挑战进度监听器。
 * 负责把游戏内行为转换为玩家自己的挑战进度。
 */
public class ChallengeListener implements Listener {

    private final ChallengeManager manager;

    public ChallengeListener(ChallengeManager manager) {
        this.manager = manager;
    }

    /**
     * 击杀实体
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }
        PlayerData data = manager.getData(killer);
        if (!data.hasDrawn()) {
            return;
        }
        String entityName = entity.getType().name();

        Challenge main = data.getMainChallenge();
        if (main != null && (main.getType() == ChallengeType.KILL || main.getType() == ChallengeType.KILL_SMALL)
                && manager.targetMatches(main, entityName)) {
            manager.addMainProgress(killer, 1);
        }

        Challenge side = data.getSideChallenge();
        if (side != null && side.getType() == ChallengeType.KILL_SMALL
                && manager.targetMatches(side, entityName)) {
            manager.addSideProgress(killer, 1);
        }
    }

    /**
     * 挖掘方块
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = manager.getData(player);
        if (!data.hasDrawn()) {
            return;
        }
        String blockName = event.getBlock().getType().name();

        Challenge main = data.getMainChallenge();
        if (main != null && main.getType() == ChallengeType.MINE && manager.targetMatches(main, blockName)) {
            manager.addMainProgress(player, 1);
        }

        Challenge side = data.getSideChallenge();
        if (side != null && side.getType() == ChallengeType.CLEAN && manager.targetMatches(side, blockName)) {
            manager.addSideProgress(player, 1);
        }
    }

    /**
     * 合成物品
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        PlayerData data = manager.getData(player);
        Challenge main = data.getMainChallenge();
        if (main == null || main.getType() != ChallengeType.CRAFT) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result == null) {
            return;
        }
        if (!manager.targetMatches(main, result.getType().name())) {
            return;
        }
        int amount = result.getAmount();
        if (event.isShiftClick()) {
            amount = Math.max(1, calcShiftAmount(event));
        }
        manager.addMainProgress(player, amount);
    }

    /**
     * 估算 shift 批量合成的产出数量
     */
    private int calcShiftAmount(CraftItemEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        int min = Integer.MAX_VALUE;
        for (ItemStack stack : matrix) {
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            min = Math.min(min, stack.getAmount());
        }
        if (min == Integer.MAX_VALUE) {
            return 1;
        }
        return min * event.getRecipe().getResult().getAmount();
    }

    /**
     * 钓鱼
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        Player player = event.getPlayer();
        PlayerData data = manager.getData(player);
        if (!data.hasDrawn()) {
            return;
        }
        Challenge main = data.getMainChallenge();
        if (main != null && main.getType() == ChallengeType.FISH) {
            manager.addMainProgress(player, 1);
        }
        Challenge side = data.getSideChallenge();
        if (side != null && side.getType() == ChallengeType.SIDE_FISH) {
            manager.addSideProgress(player, 1);
        }
    }

    /**
     * 与村民交易
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrade(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (event.getInventory().getType() != InventoryType.MERCHANT) {
            return;
        }
        if (!(event.getInventory() instanceof MerchantInventory)) {
            return;
        }
        // 只处理点击结果槽（槽位 2）取出成品的情况
        if (event.getRawSlot() != 2) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        PlayerData data = manager.getData(player);
        Challenge main = data.getMainChallenge();
        if (main != null && main.getType() == ChallengeType.TRADE) {
            manager.addMainProgress(player, 1);
        }
    }

    /**
     * 造成伤害
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getDamager();
        PlayerData data = manager.getData(player);
        Challenge main = data.getMainChallenge();
        if (main == null || main.getType() != ChallengeType.DAMAGE) {
            return;
        }
        int damage = (int) Math.ceil(event.getFinalDamage());
        if (damage > 0) {
            manager.addMainProgress(player, damage);
        }
    }

    /**
     * 给动物喂食
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFeed(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        PlayerData data = manager.getData(player);
        Challenge side = data.getSideChallenge();
        if (side == null || side.getType() != ChallengeType.FEED) {
            return;
        }
        Entity entity = event.getRightClicked();
        if (!(entity instanceof org.bukkit.entity.Animals)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!isFood(hand.getType())) {
            return;
        }
        manager.addSideProgress(player, 1);
    }

    /**
     * 判断材质是否为可喂食动物的食物
     */
    private boolean isFood(Material material) {
        switch (material) {
            case WHEAT:
            case WHEAT_SEEDS:
            case CARROT:
            case POTATO:
            case BEETROOT:
            case BEETROOT_SEEDS:
            case MELON_SLICE:
            case PUMPKIN_SEEDS:
            case SUGAR_CANE:
            case HAY_BLOCK:
            case APPLE:
            case GOLDEN_APPLE:
            case GOLDEN_CARROT:
            case SEAGRASS:
            case SWEET_BERRIES:
            case GLOW_BERRIES:
            case TORCHFLOWER_SEEDS:
            case PITCHER_POD:
            case COCOA_BEANS:
            case RED_MUSHROOM:
            case BROWN_MUSHROOM:
            case SLIME_BALL:
            case BLAZE_ROD:
            case TROPICAL_FISH:
            case COD:
            case SALMON:
            case PUFFERFISH:
            case DANDELION:
            case POPPY:
            case BLUE_ORCHID:
            case ALLIUM:
            case AZURE_BLUET:
            case RED_TULIP:
            case ORANGE_TULIP:
            case WHITE_TULIP:
            case PINK_TULIP:
            case OXEYE_DAISY:
            case CORNFLOWER:
            case LILY_OF_THE_VALLEY:
            case WITHER_ROSE:
            case SUNFLOWER:
            case LILAC:
            case ROSE_BUSH:
            case PEONY:
                return true;
            default:
                return false;
        }
    }

    /**
     * 附魔
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        PlayerData data = manager.getData(player);
        Challenge side = data.getSideChallenge();
        if (side == null || side.getType() != ChallengeType.ENCHANT) {
            return;
        }
        manager.addSideProgress(player, 1);
    }
}
