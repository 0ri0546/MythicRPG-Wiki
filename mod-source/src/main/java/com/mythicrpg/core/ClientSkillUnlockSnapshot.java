package com.mythicrpg.core;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Common-side, API-free snapshot used only for safe client prediction. */
public final class ClientSkillUnlockSnapshot {
    private static final Map<SkillType, List<Integer>> UNLOCKED = new EnumMap<>(SkillType.class);

    private ClientSkillUnlockSnapshot() {
    }

    public static void update(SkillType type, List<Integer> unlockedIds) {
        UNLOCKED.put(type, List.copyOf(unlockedIds == null ? List.of() : unlockedIds));
    }

    public static boolean isUnlocked(SkillType type, int nodeId) {
        return UNLOCKED.getOrDefault(type, List.of()).contains(nodeId);
    }

    public static void clear() {
        UNLOCKED.clear();
    }
}
