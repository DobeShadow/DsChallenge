package io.bukkitcode.dschallenge.gui;

import io.bukkitcode.dschallenge.DsChallenge;
import io.bukkitcode.dschallenge.challenge.Challenge;
import io.bukkitcode.dschallenge.challenge.ChallengeType;
import io.bukkitcode.dschallenge.data.PlayerData;
import io.bukkitcode.dschallenge.manager.ChallengeManager;
import io.bukkitcode.dschallenge.manager.RewardManager;
import io.bukkitcode.dschallenge.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * 挑战 GUI 管理器。
 * 负责入口界面、跑马灯抽奖动画与结果查看界面。
 */
public class ChallengeGui {

    private final DsChallenge plugin;
    private final ChallengeManager manager;
    private final RewardManager rewardManager;
    private final Random random = new Random();

    /** 正在正常结束动画的玩家，避免关闭事件重复打开界面 */
    private final Set<UUID> finishing = new HashSet<>();

    public ChallengeGui(DsChallenge plugin, ChallengeManager manager, RewardManager rewardManager) {
        this.plugin = plugin;
        this.manager = manager;
        this.rewardManager = rewardManager;
    }

    /**
     * 判断并消费“正常结束”标记
     */
    public boolean consumeFinishing(Player player) {
        return finishing.remove(player.getUniqueId());
    }

    /**
     * 打开入口界面（未抽取时）或结果界面（已抽取时）
     */
    public void open(Player player) {
        PlayerData data = manager.getData(player);
        if (data.hasDrawn()) {
            openResult(player, data);
        } else {
            openEntry(player);
        }
    }

    /**
     * 打开入口界面：1 行，中间一个“开启挑战”按钮
     */
    public void openEntry(Player player) {
        ChallengeHolder holder = new ChallengeHolder(ChallengeHolder.Type.ENTRY);
        Inventory inv = Bukkit.createInventory(holder, 9,
                ItemBuilder.color("&8今日挑战"));
        holder.setInventory(inv);

        ItemStack filler = ItemBuilder.pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, filler);
        }
        inv.setItem(4, ItemBuilder.build(Material.NETHER_STAR,
                "&e&l开启今日挑战",
                "&7点击抽取今天的主挑战",
                "&7每天仅可抽取一次"));

        player.openInventory(inv);
    }

    /**
     * 打开结果界面：展示今日主挑战与支线详情
     */
    public void openResult(Player player, PlayerData data) {
        ChallengeHolder holder = new ChallengeHolder(ChallengeHolder.Type.RESULT);
        Inventory inv = Bukkit.createInventory(holder, 9,
                ItemBuilder.color("&8今日挑战"));
        holder.setInventory(inv);

        ItemStack filler = ItemBuilder.pane(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, filler);
        }

        Challenge main = data.getMainChallenge();
        Challenge side = data.getSideChallenge();

        inv.setItem(2, buildMainItem(data, main));
        inv.setItem(4, buildSideItem(data, side));
        inv.setItem(6, buildRewardItem());

        player.openInventory(inv);
    }

    /**
     * 构建主挑战展示物品
     */
    private ItemStack buildMainItem(PlayerData data, Challenge main) {
        List<String> lore = new ArrayList<>();
        lore.add("&7要求: &f" + main.describe());
        lore.add("&7进度: &f" + Math.min(data.getMainProgress(), main.getAmount())
                + " &7/ &f" + main.getAmount());
        lore.add("&7状态: " + statusText(data.getMainStatus()));
        lore.add("");
        lore.add("&c注意: 主挑战一命制，死亡即失败锁定");
        return ItemBuilder.build(iconOf(main.getType()), "&6&l主挑战", lore);
    }

    /**
     * 构建支线展示物品
     */
    private ItemStack buildSideItem(PlayerData data, Challenge side) {
        List<String> lore = new ArrayList<>();
        lore.add("&7要求: &f" + side.describe());
        lore.add("&7进度: &f" + Math.min(data.getSideProgress(), side.getAmount())
                + " &7/ &f" + side.getAmount());
        lore.add("&7状态: " + statusText(data.getSideStatus()));
        lore.add("");
        lore.add("&7完成主挑战时支线奖励会一并发放");
        return ItemBuilder.build(iconOf(side.getType()), "&b&l支线挑战", lore);
    }

    /**
     * 构建奖励展示物品
     */
    private ItemStack buildRewardItem() {
        List<String> lore = new ArrayList<>();
        ConfigurationSection mainReward = rewardManager.getRewardSection(plugin.getConfig(), true);
        lore.add("&6主挑战奖励:");
        lore.addAll(rewardManager.describeReward(mainReward));
        lore.add("");
        ConfigurationSection sideReward = rewardManager.getRewardSection(plugin.getConfig(), false);
        lore.add("&b支线奖励:");
        lore.addAll(rewardManager.describeReward(sideReward));
        return ItemBuilder.build(Material.CHEST, "&e&l奖励一览", lore);
    }

    /**
     * 状态文本
     */
    private String statusText(PlayerData.Status status) {
        switch (status) {
            case COMPLETED:
                return "&a已完成";
            case FAILED:
                return "&c已失败";
            default:
                return "&e进行中";
        }
    }

    /**
     * 根据挑战类型返回展示图标
     */
    private Material iconOf(ChallengeType type) {
        switch (type) {
            case KILL:
                return Material.DIAMOND_SWORD;
            case MINE:
                return Material.DIAMOND_PICKAXE;
            case CRAFT:
                return Material.CRAFTING_TABLE;
            case FISH:
            case SIDE_FISH:
                return Material.FISHING_ROD;
            case TRADE:
                return Material.EMERALD;
            case DAMAGE:
                return Material.IRON_SWORD;
            case SURVIVE:
                return Material.SHIELD;
            case FEED:
                return Material.WHEAT;
            case ENCHANT:
                return Material.ENCHANTING_TABLE;
            case MOVE:
                return Material.LEATHER_BOOTS;
            case KILL_SMALL:
                return Material.CHICKEN_SPAWN_EGG;
            case CLEAN:
                return Material.IRON_SHOVEL;
            default:
                return Material.PAPER;
        }
    }

    /**
     * 开始跑马灯抽奖动画
     */
    public void startRolling(Player player) {
        PlayerData data = manager.getData(player);
        if (data.hasDrawn()) {
            player.sendMessage(ItemBuilder.color("&c你今天已经抽取过挑战了。"));
            return;
        }

        ChallengeHolder holder = new ChallengeHolder(ChallengeHolder.Type.ROLLING);
        Inventory inv = Bukkit.createInventory(holder, 9,
                ItemBuilder.color("&8正在抽取挑战..."));
        holder.setInventory(inv);

        // 左右各 2 格玻璃板，中间 5 格（slot 2-6）用于滚动
        ItemStack filler = ItemBuilder.pane(Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(0, filler);
        inv.setItem(1, filler);
        inv.setItem(7, filler);
        inv.setItem(8, filler);

        player.openInventory(inv);

        // 先抽取结果，动画只是表现
        manager.draw(player);
        PlayerData drawn = manager.getData(player);
        Challenge main = drawn.getMainChallenge();
        if (main == null) {
            // 抽取失败（理论上不会发生），直接关闭界面
            player.closeInventory();
            player.sendMessage(ItemBuilder.color("&c抽取挑战失败，请稍后重试。"));
            return;
        }

        new RollTask(player, inv, main).runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * 跑马灯动画任务。
     * 每 tick 刷新中间 5 格，间隔逐渐变长实现减速，最终锁定中间格。
     */
    private class RollTask extends BukkitRunnable {

        private final Player player;
        private final Inventory inventory;
        private final Challenge result;
        private final ChallengeType[] types = ChallengeType.values();

        /** 已执行步数 */
        private int step = 0;
        /** 总步数（约 3 秒 = 60 tick，但用步数控制减速） */
        private final int totalSteps = 40;
        /** 距离下一次刷新的 tick 计数 */
        private int tickCounter = 0;
        /** 当前需要的间隔 */
        private int currentInterval = 1;

        RollTask(Player player, Inventory inventory, Challenge result) {
            this.player = player;
            this.inventory = inventory;
            this.result = result;
        }

        @Override
        public void run() {
            if (!player.isOnline()) {
                cancel();
                return;
            }
            // 玩家关闭了界面则中止
            if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof ChallengeHolder)) {
                cancel();
                return;
            }

            tickCounter++;
            if (tickCounter < currentInterval) {
                return;
            }
            tickCounter = 0;
            step++;

            // 计算减速：进度越靠后间隔越大
            double progress = (double) step / totalSteps;
            if (progress > 0.5) {
                currentInterval = 1 + (int) ((progress - 0.5) * 8);
            }

            if (step >= totalSteps) {
                // 锁定结果
                lockResult();
                cancel();
                return;
            }

            // 滚动显示随机图标
            for (int slot = 2; slot <= 6; slot++) {
                ChallengeType randomType = types[random.nextInt(types.length)];
                inventory.setItem(slot, ItemBuilder.build(iconOf(randomType),
                        "&7???", "&8抽取中..."));
            }
        }

        /**
         * 锁定最终结果到中间格
         */
        private void lockResult() {
            for (int slot = 2; slot <= 6; slot++) {
                inventory.setItem(slot, ItemBuilder.pane(Material.BLACK_STAINED_GLASS_PANE));
            }
            inventory.setItem(4, ItemBuilder.build(iconOf(result.getType()),
                    "&6&l" + result.getType().getDisplayName(),
                    "&7" + result.describe()));

            // 延迟片刻后关闭并输出聊天信息
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        return;
                    }
                    // 打上正常结束标记，避免关闭事件重新打开界面
                    finishing.add(player.getUniqueId());
                    player.closeInventory();
                    sendResultMessage();
                }
            }.runTaskLater(plugin, 20L);
        }

        /**
         * 聊天框输出挑战要求与奖励
         */
        private void sendResultMessage() {
            PlayerData data = manager.getData(player);
            Challenge main = data.getMainChallenge();
            Challenge side = data.getSideChallenge();

            player.sendMessage(ItemBuilder.color("&6&l===== 今日挑战已开启 ====="));
            player.sendMessage(ItemBuilder.color("&e主挑战: &f" + main.describe()));
            player.sendMessage(ItemBuilder.color("&c  └ 一命制，死亡即失败锁定"));
            player.sendMessage(ItemBuilder.color("&b支线: &f" + side.describe()));
            player.sendMessage(ItemBuilder.color("&7  └ 完成主挑战时支线奖励一并发放"));

            ConfigurationSection mainReward = rewardManager.getRewardSection(plugin.getConfig(), true);
            List<String> mainLines = rewardManager.describeReward(mainReward);
            if (!mainLines.isEmpty()) {
                player.sendMessage(ItemBuilder.color("&6主挑战奖励:"));
                for (String line : mainLines) {
                    player.sendMessage(ItemBuilder.color("&7  " + line));
                }
            }
            ConfigurationSection sideReward = rewardManager.getRewardSection(plugin.getConfig(), false);
            List<String> sideLines = rewardManager.describeReward(sideReward);
            if (!sideLines.isEmpty()) {
                player.sendMessage(ItemBuilder.color("&b支线奖励:"));
                for (String line : sideLines) {
                    player.sendMessage(ItemBuilder.color("&7  " + line));
                }
            }
            player.sendMessage(ItemBuilder.color("&7输入 &f/challenge &7随时查看进度"));
        }
    }
}
