package io.bukkitcode.dschallenge;

import io.bukkitcode.dschallenge.command.ChallengeCommand;
import io.bukkitcode.dschallenge.data.DataManager;
import io.bukkitcode.dschallenge.gui.ChallengeGui;
import io.bukkitcode.dschallenge.gui.GuiListener;
import io.bukkitcode.dschallenge.listener.ChallengeListener;
import io.bukkitcode.dschallenge.listener.PlayerListener;
import io.bukkitcode.dschallenge.manager.ChallengeManager;
import io.bukkitcode.dschallenge.manager.RewardManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * DsChallenge 主类。
 * 每日随机挑战插件：玩家每天抽取一个高难度主挑战与一个附属支线挑战。
 */
public class DsChallenge extends JavaPlugin {

    /** 当前配置文件结构版本，配置结构变更时需递增 */
    public static final int CONFIG_VERSION = 1;

    private DataManager dataManager;
    private RewardManager rewardManager;
    private ChallengeManager challengeManager;
    private ChallengeGui challengeGui;

    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        checkConfigVersion();

        // 初始化数据层
        dataManager = new DataManager(this);
        dataManager.load();

        // 初始化奖励管理器
        rewardManager = new RewardManager(this);
        rewardManager.init();

        // 初始化挑战管理器
        challengeManager = new ChallengeManager(this, dataManager, rewardManager);
        // 启动生存类支线计时任务
        challengeManager.startSurviveTask();

        // 初始化 GUI
        challengeGui = new ChallengeGui(this, challengeManager, rewardManager);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new ChallengeListener(challengeManager), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this, challengeManager), this);
        getServer().getPluginManager().registerEvents(new GuiListener(this, challengeGui), this);

        // 注册命令
        PluginCommand command = getCommand("challenge");
        if (command != null) {
            ChallengeCommand executor = new ChallengeCommand(this, challengeManager, challengeGui);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        } else {
            getLogger().warning("未能注册 /challenge 命令，请检查 plugin.yml");
        }

        getLogger().info("DsChallenge 已启用");
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAll();
            dataManager.clearCache();
        }
        getLogger().info("DsChallenge 已关闭");
    }

    /**
     * 检查配置文件版本，若过旧则记录警告
     */
    private void checkConfigVersion() {
        int current = getConfig().getInt("config-version", 0);
        if (current != CONFIG_VERSION) {
            getLogger().warning("配置文件版本不匹配（当前: " + current
                    + "，期望: " + CONFIG_VERSION + "），建议删除 config.yml 后重启以生成新配置。");
        }
    }

    /**
     * 配置文件是否为旧版本
     */
    public boolean isConfigOutdated() {
        return getConfig().getInt("config-version", 0) != CONFIG_VERSION;
    }
}
