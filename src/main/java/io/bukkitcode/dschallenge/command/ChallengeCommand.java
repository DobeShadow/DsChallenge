package io.bukkitcode.dschallenge.command;

import io.bukkitcode.dschallenge.DsChallenge;
import io.bukkitcode.dschallenge.data.PlayerData;
import io.bukkitcode.dschallenge.gui.ChallengeGui;
import io.bukkitcode.dschallenge.manager.ChallengeManager;
import io.bukkitcode.dschallenge.util.ItemBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /challenge 命令。
 * 无参数打开挑战界面；refresh 为管理员刷新命令。
 */
public class ChallengeCommand implements CommandExecutor, TabCompleter {

    private final DsChallenge plugin;
    private final ChallengeManager manager;
    private final ChallengeGui gui;

    public ChallengeCommand(DsChallenge plugin, ChallengeManager manager, ChallengeGui gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ItemBuilder.color("&c该命令只能由玩家执行。"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 0) {
            gui.open(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("refresh")) {
            if (!player.hasPermission("dschallenge.admin")) {
                player.sendMessage(ItemBuilder.color("&c你没有权限执行该操作。"));
                return true;
            }
            // 重置该玩家今日挑战，使其可以重新抽取
            PlayerData data = manager.getData(player);
            data.reset(manager.todayKey());
            manager.savePlayer(player);
            player.sendMessage(ItemBuilder.color("&a已重置你今日的挑战，可重新抽取。"));
            gui.open(player);
            return true;
        }

        player.sendMessage(ItemBuilder.color("&c未知子命令。用法: /challenge [refresh]"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("dschallenge.admin")) {
                options.add("refresh");
            }
            String prefix = args[0].toLowerCase();
            options.removeIf(option -> !option.startsWith(prefix));
            return options;
        }
        return Arrays.asList();
    }
}
