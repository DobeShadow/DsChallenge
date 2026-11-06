package io.bukkitcode.dschallenge.manager;

import io.bukkitcode.dschallenge.DsChallenge;
import io.bukkitcode.dschallenge.challenge.Challenge;
import io.bukkitcode.dschallenge.challenge.ChallengeGenerator;
import io.bukkitcode.dschallenge.challenge.ChallengeType;
import io.bukkitcode.dschallenge.data.DataManager;
import io.bukkitcode.dschallenge.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 挑战核心管理器。
 * 每个玩家拥有独立的主挑战与支线挑战，由玩家主动抽取开启。
 */
public class ChallengeManager {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final DsChallenge plugin;
    private final DataManager dataManager;
    private final RewardManager rewardManager;
    private final ChallengeGenerator generator = new ChallengeGenerator();

    public ChallengeManager(DsChallenge plugin, DataManager dataManager, RewardManager rewardManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.rewardManager = rewardManager;
    }

    /**
     * 启动生存类支线的计时任务。
     * 每秒为持有生存类支线且状态为进行中的在线玩家推进 1 秒进度。
     */
    public void startSurviveTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerData data = getData(player);
                Challenge side = data.getSideChallenge();
                if (side == null || side.getType() != ChallengeType.SURVIVE) {
                    continue;
                }
                if (data.getSideStatus() != PlayerData.Status.IN_PROGRESS) {
                    continue;
                }
                addSideProgress(player, 1);
            }
        }, 20L, 20L);
    }

    /**
     * 重置玩家的生存类支线进度（死亡时调用）
     */
    public void resetSurviveProgress(Player player) {
        PlayerData data = getData(player);
        Challenge side = data.getSideChallenge();
        if (side != null && side.getType() == ChallengeType.SURVIVE
                && data.getSideStatus() == PlayerData.Status.IN_PROGRESS) {
            data.setSideProgress(0);
        }
    }

    /**
     * 为玩家抽取今日挑战（若已抽取则返回 false）
     */
    public boolean draw(Player player) {
        PlayerData data = getData(player);
        if (data.hasDrawn()) {
            return false;
        }
        FileConfiguration config = plugin.getConfig();
        Challenge main = generator.generateMain(config);
        Challenge side = generator.generateSide(config);
        data.setMainChallenge(main);
        data.setSideChallenge(side);
        data.setMainProgress(0);
        data.setMainStatus(PlayerData.Status.IN_PROGRESS);
        data.setSideProgress(0);
        data.setSideStatus(PlayerData.Status.IN_PROGRESS);
        dataManager.saveAll();
        return true;
    }

    /**
     * 计算“挑战日期”。
     * 若当前时间早于配置的重置时间，则视为前一天。
     */
    public String todayKey() {
        LocalDateTime now = LocalDateTime.now();
        LocalTime resetTime = parseResetTime();
        if (now.toLocalTime().isBefore(resetTime)) {
            return now.toLocalDate().minusDays(1).format(DATE_FORMAT);
        }
        return now.toLocalDate().format(DATE_FORMAT);
    }

    private LocalTime parseResetTime() {
        String raw = plugin.getConfig().getString("reset-time", "04:00");
        try {
            return LocalTime.parse(raw, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            plugin.getLogger().warning("reset-time 配置格式错误，已回退为 04:00");
            return LocalTime.of(4, 0);
        }
    }

    /**
     * 获取玩家数据（自动处理跨天重置）
     */
    public PlayerData getData(Player player) {
        return dataManager.getOrCreate(player.getUniqueId(), todayKey());
    }

    /**
     * 推进主挑战进度
     */
    public void addMainProgress(Player player, int delta) {
        if (delta <= 0) {
            return;
        }
        PlayerData data = getData(player);
        Challenge main = data.getMainChallenge();
        if (main == null || data.getMainStatus() != PlayerData.Status.IN_PROGRESS) {
            return;
        }
        data.addMainProgress(delta);
        if (data.getMainProgress() >= main.getAmount()) {
            completeMain(player, data, main);
        }
    }

    /**
     * 推进支线进度
     */
    public void addSideProgress(Player player, int delta) {
        if (delta <= 0) {
            return;
        }
        PlayerData data = getData(player);
        Challenge side = data.getSideChallenge();
        if (side == null || data.getSideStatus() != PlayerData.Status.IN_PROGRESS) {
            return;
        }
        data.addSideProgress(delta);
        if (data.getSideProgress() >= side.getAmount()) {
            completeSide(player, data, side);
        }
    }

    /**
     * 主挑战完成：发放主奖励 + 支线奖励（支线作为附属一并发放）
     */
    private void completeMain(Player player, PlayerData data, Challenge main) {
        data.setMainStatus(PlayerData.Status.COMPLETED);
        data.setMainProgress(main.getAmount());

        ConfigurationSection mainReward = rewardManager.getRewardSection(plugin.getConfig(), true);
        rewardManager.giveReward(player, mainReward);

        // 支线作为附属奖励一并发放（若尚未完成）
        Challenge side = data.getSideChallenge();
        if (side != null && data.getSideStatus() != PlayerData.Status.COMPLETED) {
            data.setSideStatus(PlayerData.Status.COMPLETED);
            data.setSideProgress(side.getAmount());
            ConfigurationSection sideReward = rewardManager.getRewardSection(plugin.getConfig(), false);
            rewardManager.giveReward(player, sideReward);
        }

        String msg = plugin.getConfig().getString("messages.main-complete",
                "&6[每日挑战] &e%player% &a完成了今日主挑战：&f%challenge%")
                .replace("%player%", player.getName())
                .replace("%challenge%", main.describe());
        if (plugin.getConfig().getBoolean("broadcast-on-complete", true)) {
            Bukkit.broadcastMessage(color(msg));
        } else {
            player.sendMessage(color(msg));
        }
        dataManager.saveAll();
    }

    /**
     * 支线单独完成（主挑战未完成时）
     */
    private void completeSide(Player player, PlayerData data, Challenge side) {
        data.setSideStatus(PlayerData.Status.COMPLETED);
        data.setSideProgress(side.getAmount());

        ConfigurationSection reward = rewardManager.getRewardSection(plugin.getConfig(), false);
        rewardManager.giveReward(player, reward);

        String msg = plugin.getConfig().getString("messages.side-complete",
                "&b[每日挑战] &a你完成了今日支线：&f%challenge%")
                .replace("%player%", player.getName())
                .replace("%challenge%", side.describe());
        player.sendMessage(color(msg));
        dataManager.saveAll();
    }

    /**
     * 主挑战失败（玩家死亡）
     */
    public void failMain(Player player) {
        PlayerData data = getData(player);
        Challenge main = data.getMainChallenge();
        if (main == null || data.getMainStatus() != PlayerData.Status.IN_PROGRESS) {
            return;
        }
        data.setMainStatus(PlayerData.Status.FAILED);
        String msg = plugin.getConfig().getString("messages.main-failed",
                "&c[每日挑战] &7你死亡了，今日主挑战失败并已锁定。")
                .replace("%player%", player.getName());
        player.sendMessage(color(msg));
        dataManager.saveAll();
    }

    /**
     * 玩家退出时保存
     */
    public void savePlayer(Player player) {
        dataManager.saveAll();
    }

    /**
     * 判断目标是否匹配（target 为 null 时视为匹配任意）
     */
    public boolean targetMatches(Challenge challenge, String candidate) {
        if (challenge == null) {
            return false;
        }
        String target = challenge.getTarget();
        if (target == null) {
            return true;
        }
        return target.equalsIgnoreCase(candidate);
    }

    /**
     * 判断玩家当前主挑战是否为指定类型
     */
    public boolean isMainType(PlayerData data, ChallengeType type) {
        return data.getMainChallenge() != null && data.getMainChallenge().getType() == type;
    }

    /**
     * 判断玩家当前支线是否为指定类型
     */
    public boolean isSideType(PlayerData data, ChallengeType type) {
        return data.getSideChallenge() != null && data.getSideChallenge().getType() == type;
    }

    private String color(String text) {
        return text.replace('&', '§');
    }
}
