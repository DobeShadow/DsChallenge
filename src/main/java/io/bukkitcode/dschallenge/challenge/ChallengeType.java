package io.bukkitcode.dschallenge.challenge;

/**
 * 挑战类型枚举。
 * 每个类型标记了它属于主挑战池还是支线池，以及默认的展示名称。
 */
public enum ChallengeType {

    // ===== 主挑战池 =====
    KILL("击杀挑战", true),
    MINE("采集挑战", true),
    CRAFT("合成挑战", true),
    FISH("钓鱼挑战", true),
    TRADE("交易挑战", true),
    DAMAGE("伤害挑战", true),

    // ===== 支线池 =====
    SURVIVE("生存挑战", false),
    SIDE_FISH("钓鱼支线", false),
    FEED("喂养挑战", false),
    ENCHANT("附魔挑战", false),
    MOVE("移动挑战", false),
    KILL_SMALL("小怪击杀", false),
    CLEAN("清理挑战", false);

    private final String displayName;
    private final boolean main;

    ChallengeType(String displayName, boolean main) {
        this.displayName = displayName;
        this.main = main;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 是否属于主挑战池
     */
    public boolean isMain() {
        return main;
    }

    /**
     * 按名称安全解析，找不到返回 null
     */
    public static ChallengeType fromName(String name) {
        if (name == null) {
            return null;
        }
        for (ChallengeType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
