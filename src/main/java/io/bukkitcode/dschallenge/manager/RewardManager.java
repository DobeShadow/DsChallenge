package io.bukkitcode.dschallenge.manager;

import io.bukkitcode.dschallenge.DsChallenge;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * 奖励发放管理器。
 * 支持物品奖励、Vault 经济奖励、命令奖励三种形式。
 */
public class RewardManager {

    private final DsChallenge plugin;
    /** Vault 经济是否可用 */
    private boolean vaultAvailable;

    public RewardManager(DsChallenge plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化时检测 Vault 是否可用
     */
    public void init() {
        vaultAvailable = Bukkit.getPluginManager().getPlugin("Vault") != null;
        if (vaultAvailable) {
            plugin.getLogger().info("已检测到 Vault，经济奖励功能启用");
        } else {
            plugin.getLogger().info("未检测到 Vault，经济奖励将被跳过");
        }
    }

    public boolean isVaultAvailable() {
        return vaultAvailable;
    }

    /**
     * 发放奖励
     *
     * @param player  目标玩家
     * @param section 奖励配置节点（main 或 side）
     */
    public void giveReward(Player player, ConfigurationSection section) {
        if (section == null) {
            return;
        }
        giveItems(player, section);
        giveMoney(player, section);
        runCommands(player, section);
    }

    /**
     * 发放物品奖励
     */
    private void giveItems(Player player, ConfigurationSection section) {
        List<?> rawList = section.getList("items");
        if (rawList == null) {
            return;
        }
        for (Object raw : rawList) {
            if (!(raw instanceof ConfigurationSection)) {
                continue;
            }
            ConfigurationSection itemSection = (ConfigurationSection) raw;
            String materialName = itemSection.getString("material");
            if (materialName == null) {
                continue;
            }
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("奖励物品材质无效: " + materialName);
                continue;
            }
            int amount = Math.max(1, itemSection.getInt("amount", 1));
            ItemStack stack = new ItemStack(material, amount);
            String name = itemSection.getString("name");
            if (name != null && !name.isEmpty()) {
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(color(name));
                    stack.setItemMeta(meta);
                }
            }
            // 背包满则掉落在地上，避免奖励丢失
            if (!player.getInventory().addItem(stack).isEmpty()) {
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
        }
    }

    /**
     * 发放经济奖励
     */
    private void giveMoney(Player player, ConfigurationSection section) {
        double money = section.getDouble("money", 0.0);
        if (money <= 0) {
            return;
        }
        if (!vaultAvailable) {
            plugin.getLogger().info("跳过经济奖励（未安装 Vault）: " + player.getName() + " +" + money);
            return;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            Object registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null) {
                plugin.getLogger().warning("Vault 已安装但未找到经济服务实现");
                return;
            }
            Object economy = registration.getClass().getMethod("getProvider").invoke(registration);
            economyClass.getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class)
                    .invoke(economy, player, money);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "发放经济奖励失败", e);
        }
    }

    /**
     * 执行命令奖励
     */
    private void runCommands(Player player, ConfigurationSection section) {
        List<String> commands = section.getStringList("commands");
        for (String command : commands) {
            if (command == null || command.isEmpty()) {
                continue;
            }
            String parsed = command.replace("%player%", player.getName());
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "执行奖励命令失败: " + parsed, e);
            }
        }
    }

    /**
     * 读取奖励配置节点
     */
    public ConfigurationSection getRewardSection(FileConfiguration config, boolean main) {
        return config.getConfigurationSection(main ? "rewards.main" : "rewards.side");
    }

    /**
     * 生成奖励描述文本，用于 GUI 展示
     */
    public List<String> describeReward(ConfigurationSection section) {
        List<String> lines = new ArrayList<>();
        if (section == null) {
            return lines;
        }
        List<?> rawList = section.getList("items");
        if (rawList != null) {
            for (Object raw : rawList) {
                if (!(raw instanceof ConfigurationSection)) {
                    continue;
                }
                ConfigurationSection itemSection = (ConfigurationSection) raw;
                String materialName = itemSection.getString("material", "?");
                int amount = itemSection.getInt("amount", 1);
                lines.add("§7物品: §f" + materialName + " x" + amount);
            }
        }
        double money = section.getDouble("money", 0.0);
        if (money > 0) {
            lines.add("§7金币: §6" + money);
        }
        List<String> commands = section.getStringList("commands");
        if (!commands.isEmpty()) {
            lines.add("§7额外奖励: §f" + commands.size() + " 项");
        }
        return lines;
    }

    private String color(String text) {
        return text.replace('&', '§');
    }
}
