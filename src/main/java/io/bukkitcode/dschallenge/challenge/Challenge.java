package io.bukkitcode.dschallenge.challenge;

import java.util.HashMap;
import java.util.Map;

/**
 * 单个挑战的数据模型。
 * 一个 Challenge 描述“今天要做什么”，不保存玩家进度（进度在 PlayerData 里）。
 */
public class Challenge {

    private final ChallengeType type;
    /** 目标标识：怪物名 / 方块名 / 物品名 / 生物群系名 / 实体名，部分类型可为空 */
    private final String target;
    /** 需要达到的数量 / 时长（秒） / 距离（格） */
    private final int amount;
    /** 是否为主挑战 */
    private final boolean main;

    public Challenge(ChallengeType type, String target, int amount, boolean main) {
        this.type = type;
        this.target = target;
        this.amount = amount;
        this.main = main;
    }

    public ChallengeType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getAmount() {
        return amount;
    }

    public boolean isMain() {
        return main;
    }

    /**
     * 生成面向玩家的一句话描述
     */
    public String describe() {
        String t = target == null ? "" : target;
        switch (type) {
            case KILL:
                return "击杀 " + amount + " 只 " + t;
            case MINE:
                return "挖掘 " + amount + " 个 " + t;
            case CRAFT:
                return "合成 " + amount + " 个 " + t;
            case FISH:
            case SIDE_FISH:
                return "钓到 " + amount + " 条鱼";
            case TRADE:
                return "与村民完成 " + amount + " 次交易";
            case DAMAGE:
                return "累计造成 " + amount + " 点伤害";
            case SURVIVE:
                return "连续存活 " + formatTime(amount) + " 且不死亡";
            case FEED:
                return "给动物喂食 " + amount + " 次";
            case ENCHANT:
                return "完成 " + amount + " 次附魔";
            case MOVE:
                return "累计移动 " + amount + " 格";
            case KILL_SMALL:
                return "击杀 " + amount + " 只 " + t;
            case CLEAN:
                return "清理 " + amount + " 个 " + t;
            default:
                return "未知挑战";
        }
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        if (m > 0 && s > 0) {
            return m + " 分 " + s + " 秒";
        }
        if (m > 0) {
            return m + " 分钟";
        }
        return s + " 秒";
    }

    /**
     * 序列化为 Map，便于写入 yml
     */
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type.name());
        map.put("target", target);
        map.put("amount", amount);
        map.put("main", main);
        return map;
    }

    /**
     * 从 Map 反序列化
     */
    public static Challenge deserialize(Map<?, ?> map) {
        if (map == null) {
            return null;
        }
        ChallengeType type = ChallengeType.fromName(String.valueOf(map.get("type")));
        if (type == null) {
            return null;
        }
        Object targetObj = map.get("target");
        String target = targetObj == null ? null : String.valueOf(targetObj);
        int amount = map.get("amount") instanceof Number ? ((Number) map.get("amount")).intValue() : 1;
        boolean main = map.get("main") instanceof Boolean && (Boolean) map.get("main");
        return new Challenge(type, target, amount, main);
    }
}
