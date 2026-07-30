package com.mythicrpg.fishing;

import net.minecraft.util.math.random.Random;

import java.util.EnumMap;

public final class FishingBalance {
    private static final int[] BASE_WEIGHTS = {50, 25, 15, 7, 3};
    private static final int[] BAIT_I_WEIGHTS = {42, 23, 20, 10, 5};
    private static final int[] BAIT_II_WEIGHTS = {32, 20, 27, 14, 7};
    private static final int[] BAIT_III_WEIGHTS = {20, 15, 35, 20, 10};
    private static final int[] LEGENDARY_BAIT_WEIGHTS = {0, 0, 50, 40, 10};

    private FishingBalance() {
    }

    public static EnumMap<FishingRarity, Integer> weights(
            FishingUpgradeItem.Kind bait,
            boolean rarityRune
    ) {
        int[] source;
        if (bait == FishingUpgradeItem.Kind.BAIT_I) {
            source = BAIT_I_WEIGHTS;
        } else if (bait == FishingUpgradeItem.Kind.BAIT_II) {
            source = BAIT_II_WEIGHTS;
        } else if (bait == FishingUpgradeItem.Kind.BAIT_III) {
            source = BAIT_III_WEIGHTS;
        } else if (bait == FishingUpgradeItem.Kind.BAIT_LEGENDARY) {
            source = LEGENDARY_BAIT_WEIGHTS;
        } else {
            source = BASE_WEIGHTS;
        }

        EnumMap<FishingRarity, Integer> weights = new EnumMap<>(FishingRarity.class);
        for (FishingRarity rarity : FishingRarity.values()) {
            weights.put(rarity, source[rarity.rank()]);
        }

        if (rarityRune && bait != FishingUpgradeItem.Kind.BAIT_LEGENDARY) {
            shift(weights, FishingRarity.COMMON, -5);
            shift(weights, FishingRarity.RARE, -3);
            shift(weights, FishingRarity.EPIC, 4);
            shift(weights, FishingRarity.LEGENDARY, 3);
            shift(weights, FishingRarity.MYTHIC, 1);
        }
        return weights;
    }

    private static void shift(
            EnumMap<FishingRarity, Integer> weights,
            FishingRarity rarity,
            int delta
    ) {
        weights.put(rarity, Math.max(0, weights.getOrDefault(rarity, 0) + delta));
    }

    public static FishingRarity roll(
            Random random,
            FishingUpgradeItem.Kind bait,
            boolean rarityRune
    ) {
        EnumMap<FishingRarity, Integer> weights = weights(bait, rarityRune);
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int roll = random.nextInt(Math.max(1, total));
        int cursor = 0;
        for (FishingRarity rarity : FishingRarity.values()) {
            cursor += weights.getOrDefault(rarity, 0);
            if (roll < cursor) {
                return rarity;
            }
        }
        return FishingRarity.COMMON;
    }

    public static int passiveCaptureIntervalTicks(boolean boat) {
        return boat ? 20 * 45 : 20 * 90;
    }
}
