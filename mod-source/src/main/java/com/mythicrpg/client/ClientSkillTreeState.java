package com.mythicrpg.client;

import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.ClientSkillUnlockSnapshot;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ClientSkillTreeState {
    private static final Map<SkillType, List<Integer>> unlockedIds = new EnumMap<>(SkillType.class);
    private static final Map<SkillType, Integer> skillPoints = new EnumMap<>(SkillType.class);
    private static final Map<SkillType, Integer> levels = new EnumMap<>(SkillType.class);
    private static final Map<SkillType, Integer> currentXp = new EnumMap<>(SkillType.class);
    private static final Map<SkillType, Integer> xpForNext = new EnumMap<>(SkillType.class);

    public static void update(SkillType type, List<Integer> newUnlockedIds, int newSkillPoints,
                              int newLevel, int newCurrentXp, int newXpForNext) {
        unlockedIds.put(type, newUnlockedIds);
        ClientSkillUnlockSnapshot.update(type, newUnlockedIds);
        skillPoints.put(type, newSkillPoints);
        levels.put(type, newLevel);
        currentXp.put(type, newCurrentXp);
        xpForNext.put(type, newXpForNext);
    }

    public static void updateXp(SkillType type, int newLevel, int newCurrentXp, int newXpForNext) {
        levels.put(type, newLevel);
        currentXp.put(type, newCurrentXp);
        xpForNext.put(type, newXpForNext);
    }

    public static List<Integer> getUnlockedIds(SkillType type) {
        return unlockedIds.getOrDefault(type, List.of());
    }

    public static boolean isUnlocked(SkillType type, int nodeId) {
        return getUnlockedIds(type).contains(nodeId);
    }

    public static int getSkillPoints(SkillType type) {
        return skillPoints.getOrDefault(type, 0);
    }

    public static int getLevel(SkillType type) {
        return levels.getOrDefault(type, 1);
    }

    public static int getCurrentXp(SkillType type) {
        return currentXp.getOrDefault(type, 0);
    }

    public static int getXpForNext(SkillType type) {
        return xpForNext.getOrDefault(type, 0);
    }

    public static int getGlobalLevel() {
        int total = 0;

        for (SkillType type : SkillType.values()) {
            total += getLevel(type);
        }

        return total;
    }

    public static int getMaxGlobalLevel() {
        return SkillType.values().length * 100;
    }
}