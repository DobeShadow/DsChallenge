package io.bukkitcode.dschallenge.data;

import io.bukkitcode.dschallenge.DsChallenge;
import io.bukkitcode.dschallenge.challenge.Challenge;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 数据持久化管理器。
 * 负责玩家数据（含各自抽取的挑战与进度）的读写，存储于 playerdata.yml。
 */
public class DataManager {

    private final DsChallenge plugin;
    private final File playerFile;
    private FileConfiguration playerConfig;

    /** 内存缓存：玩家 UUID -> 数据 */
    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public DataManager(DsChallenge plugin) {
        this.plugin = plugin;
        this.playerFile = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    /**
     * 加载数据文件
     */
    public void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("无法创建插件数据目录");
        }
        playerConfig = loadFile(playerFile);
    }

    private FileConfiguration loadFile(File file) {
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    plugin.getLogger().info("已创建数据文件: " + file.getName());
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "创建数据文件失败: " + file.getName(), e);
            }
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    /**
     * 保存全部玩家数据
     */
    public void saveAll() {
        if (playerConfig == null) {
            return;
        }
        for (PlayerData data : cache.values()) {
            writePlayer(playerConfig, data);
        }
        try {
            playerConfig.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存玩家数据失败", e);
        }
    }

    /**
     * 获取玩家数据，不存在则创建；跨天则重置
     */
    public PlayerData getOrCreate(UUID uuid, String today) {
        PlayerData data = cache.get(uuid);
        if (data == null) {
            data = readPlayer(uuid);
            if (data == null) {
                data = new PlayerData(uuid, today);
            }
            cache.put(uuid, data);
        }
        if (!today.equals(data.getDate())) {
            data.reset(today);
        }
        return data;
    }

    /**
     * 从配置文件读取玩家数据
     */
    private PlayerData readPlayer(UUID uuid) {
        if (playerConfig == null) {
            return null;
        }
        String path = "players." + uuid;
        if (!playerConfig.isConfigurationSection(path)) {
            return null;
        }
        String date = playerConfig.getString(path + ".date", "");
        PlayerData data = new PlayerData(uuid, date);
        data.setMainProgress(playerConfig.getInt(path + ".main-progress", 0));
        data.setSideProgress(playerConfig.getInt(path + ".side-progress", 0));
        data.setMainStatus(parseStatus(playerConfig.getString(path + ".main-status")));
        data.setSideStatus(parseStatus(playerConfig.getString(path + ".side-status")));

        ConfigurationSection mainSection = playerConfig.getConfigurationSection(path + ".main-challenge");
        if (mainSection != null) {
            data.setMainChallenge(Challenge.deserialize(mainSection.getValues(false)));
        }
        ConfigurationSection sideSection = playerConfig.getConfigurationSection(path + ".side-challenge");
        if (sideSection != null) {
            data.setSideChallenge(Challenge.deserialize(sideSection.getValues(false)));
        }
        return data;
    }

    private PlayerData.Status parseStatus(String name) {
        if (name == null) {
            return PlayerData.Status.IN_PROGRESS;
        }
        try {
            return PlayerData.Status.valueOf(name);
        } catch (IllegalArgumentException e) {
            return PlayerData.Status.IN_PROGRESS;
        }
    }

    /**
     * 将玩家数据写入配置对象
     */
    private void writePlayer(FileConfiguration config, PlayerData data) {
        String path = "players." + data.getUuid();
        config.set(path + ".date", data.getDate());
        config.set(path + ".main-progress", data.getMainProgress());
        config.set(path + ".main-status", data.getMainStatus().name());
        config.set(path + ".side-progress", data.getSideProgress());
        config.set(path + ".side-status", data.getSideStatus().name());
        config.set(path + ".main-challenge", data.getMainChallenge() == null
                ? null : data.getMainChallenge().serialize());
        config.set(path + ".side-challenge", data.getSideChallenge() == null
                ? null : data.getSideChallenge().serialize());
    }

    /**
     * 清理内存缓存（插件关闭时调用）
     */
    public void clearCache() {
        cache.clear();
    }
}
