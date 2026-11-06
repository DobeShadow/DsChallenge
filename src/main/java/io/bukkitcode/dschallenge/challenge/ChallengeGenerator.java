package io.bukkitcode.dschallenge.challenge;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 每日挑战生成器。
 * 从配置中读取主挑战池与支线池，随机抽取目标与数量。
 */
public class ChallengeGenerator {

    private final Random random = new Random();

    /**
     * 生成今日主挑战
     */
    public Challenge generateMain(FileConfiguration config) {
        List<ChallengeType> pool = readPool(config, "main-pool", true);
        if (pool.isEmpty()) {
            // 兜底：配置为空时使用默认击杀挑战
            return new Challenge(ChallengeType.KILL, "ZOMBIE", 200, true);
        }
        ChallengeType type = pool.get(random.nextInt(pool.size()));
        return buildChallenge(config, type, true);
    }

    /**
     * 生成今日支线挑战
     */
    public Challenge generateSide(FileConfiguration config) {
        List<ChallengeType> pool = readPool(config, "side-pool", false);
        if (pool.isEmpty()) {
            return new Challenge(ChallengeType.SURVIVE, null, 1800, false);
        }
        ChallengeType type = pool.get(random.nextInt(pool.size()));
        return buildChallenge(config, type, false);
    }

    /**
     * 读取配置中的类型池
     */
    private List<ChallengeType> readPool(FileConfiguration config, String path, boolean main) {
        List<ChallengeType> result = new ArrayList<>();
        List<String> names = config.getStringList(path);
        for (String name : names) {
            ChallengeType type = ChallengeType.fromName(name);
            if (type != null && type.isMain() == main) {
                result.add(type);
            }
        }
        return result;
    }

    /**
     * 根据类型构建具体挑战
     */
    private Challenge buildChallenge(FileConfiguration config, ChallengeType type, boolean main) {
        String base = "challenges." + type.name() + ".";
        int min = config.getInt(base + "min", 1);
        int max = config.getInt(base + "max", min);
        int amount = randomBetween(min, max);

        String target = null;
        switch (type) {
            case KILL:
            case KILL_SMALL:
            case MINE:
            case CRAFT:
            case CLEAN:
                target = pickTarget(config, base + "targets");
                break;
            default:
                break;
        }
        return new Challenge(type, target, amount, main);
    }

    /**
     * 从目标列表中随机抽取一个
     */
    private String pickTarget(FileConfiguration config, String path) {
        List<String> targets = config.getStringList(path);
        if (targets.isEmpty()) {
            return null;
        }
        return targets.get(random.nextInt(targets.size()));
    }

    private int randomBetween(int min, int max) {
        if (max <= min) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }
}
