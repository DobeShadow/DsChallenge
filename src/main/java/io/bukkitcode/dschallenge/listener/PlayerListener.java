package io.bukkitcode.dschallenge.listener;

import io.bukkitcode.dschallenge.DsChallenge;
import io.bukkitcode.dschallenge.challenge.Challenge;
import io.bukkitcode.dschallenge.challenge.ChallengeType;
import io.bukkitcode.dschallenge.data.PlayerData;
import io.bukkitcode.dschallenge.manager.ChallengeManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 玩家相关监听器。
 * 处理进服提示、死亡失败判定、移动距离累计与退出保存。
 */
public class PlayerListener implements Listener {

    private final DsChallenge plugin;
    private final ChallengeManager manager;

    /** 移动距离累计缓存：UUID -> 已累计的未满一格距离 */
    private final Map<UUID, Double> moveBuffer = new HashMap<>();

    public PlayerListener(DsChallenge plugin, ChallengeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * 玩家进服：旧配置检测 + 今日挑战提示
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 旧配置文件提示（仅 OP 可见）
        if (player.isOp() && plugin.isConfigOutdated()) {
            player.sendMessage(color("&c[DsChallenge] &e检测到配置文件版本过旧，"
                    + "当前配置版本: &f" + plugin.getConfig().getInt("config-version", 0)
                    + " &e，插件期望版本: &f" + DsChallenge.CONFIG_VERSION
                    + " &e。建议删除 config.yml 后重启以生成新配置。"));
        }

        if (!plugin.getConfig().getBoolean("notify-on-join", true)) {
            return;
        }
        PlayerData data = manager.getData(player);
        if (!data.hasDrawn()) {
            player.sendMessage(color("&6&l===== 今日挑战 ====="));
            player.sendMessage(color("&e你今天还没有开启挑战！"));
            player.sendMessage(color("&7输入 &f/challenge &7打开界面开始抽取"));
            return;
        }
        Challenge main = data.getMainChallenge();
        Challenge side = data.getSideChallenge();
        player.sendMessage(color("&6&l===== 今日挑战 ====="));
        player.sendMessage(color("&e主挑战: &f" + main.describe()));
        player.sendMessage(color("&b支线: &f" + side.describe()));
        player.sendMessage(color("&7输入 &f/challenge &7查看详情"));
    }

    /**
     * 玩家死亡：主挑战失败 + 生存类支线进度清零
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        manager.failMain(player);
        manager.resetSurviveProgress(player);
        moveBuffer.remove(player.getUniqueId());
    }

    /**
     * 玩家移动：累计移动距离（仅当该玩家支线为移动类时统计）
     * 注意：PlayerMoveEvent 为高频事件，此处仅在必要时计算，并使用距离阈值累计。
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerData data = manager.getData(player);
        Challenge side = data.getSideChallenge();
        if (side == null || side.getType() != ChallengeType.MOVE) {
            return;
        }
        if (data.getSideStatus() != PlayerData.Status.IN_PROGRESS) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        // 仅统计水平移动，避免跳跃/掉落刷进度
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= 0.0) {
            return;
        }
        UUID uuid = player.getUniqueId();
        double buffer = moveBuffer.getOrDefault(uuid, 0.0) + distance;
        if (buffer >= 1.0) {
            int whole = (int) buffer;
            moveBuffer.put(uuid, buffer - whole);
            manager.addSideProgress(player, whole);
        } else {
            moveBuffer.put(uuid, buffer);
        }
    }

    /**
     * 玩家退出：保存数据
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        moveBuffer.remove(event.getPlayer().getUniqueId());
        manager.savePlayer(event.getPlayer());
    }

    private String color(String text) {
        return text.replace('&', '§');
    }
}
