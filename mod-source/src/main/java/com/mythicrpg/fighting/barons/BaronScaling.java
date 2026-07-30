package com.mythicrpg.fighting.barons;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

public final class BaronScaling {

    private static final String SPAWN_FIGHTING_LEVEL_TAG_PREFIX = "mythicrpg_baron_spawn_fighting_level_";

    private static final int MIN_LEVEL = 0;
    private static final int MAX_LEVEL = 100;

    private static final double HEALTH_PER_LEVEL = 0.03;
    private static final double DAMAGE_PER_LEVEL = 0.01;
    private static final double XP_REWARD_PER_LEVEL = 0.01;

    private BaronScaling() {
    }

    public static void setSpawnFightingLevel(LivingEntity entity, int fightingLevel) {
        entity.getCommandTags().stream()
                .filter(tag -> tag.startsWith(SPAWN_FIGHTING_LEVEL_TAG_PREFIX))
                .toList()
                .forEach(entity::removeCommandTag);

        entity.addCommandTag(SPAWN_FIGHTING_LEVEL_TAG_PREFIX + clampLevel(fightingLevel));
    }

    public static int getSpawnFightingLevel(Entity entity) {
        for (String tag : entity.getCommandTags()) {
            if (!tag.startsWith(SPAWN_FIGHTING_LEVEL_TAG_PREFIX)) {
                continue;
            }

            String rawLevel = tag.substring(SPAWN_FIGHTING_LEVEL_TAG_PREFIX.length());

            try {
                return clampLevel(Integer.parseInt(rawLevel));
            } catch (NumberFormatException ignored) {
                return MIN_LEVEL;
            }
        }

        return MIN_LEVEL;
    }

    public static double getHealthMultiplier(Entity entity) {
        return getHealthMultiplier(getSpawnFightingLevel(entity));
    }

    public static double getHealthMultiplier(int fightingLevel) {
        return 1.0 + clampLevel(fightingLevel) * HEALTH_PER_LEVEL;
    }

    public static double getDamageMultiplier(Entity entity) {
        return getDamageMultiplier(getSpawnFightingLevel(entity));
    }

    public static double getDamageMultiplier(int fightingLevel) {
        return 1.0 + clampLevel(fightingLevel) * DAMAGE_PER_LEVEL;
    }

    public static double getXpRewardMultiplier(Entity entity) {
        return getXpRewardMultiplier(getSpawnFightingLevel(entity));
    }

    public static double getXpRewardMultiplier(int fightingLevel) {
        return 1.0 + clampLevel(fightingLevel) * XP_REWARD_PER_LEVEL;
    }

    public static double getUnscaledMaxHealthForXp(LivingEntity entity) {
        return entity.getMaxHealth() / getHealthMultiplier(entity);
    }

    public static double getDruidHealMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static int getBarrageCooldownTicks(Entity entity, int baseCooldownTicks) {
        return scaledCooldown(entity, baseCooldownTicks, 0.60);
    }

    public static double getNukeCloudDurationMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static double getNukeCloudRadiusMultiplier(Entity entity) {
        return linearMultiplier(entity, 0.5);
    }

    public static float getSurvivorDirectDamageTakenMultiplier(Entity entity) {
        return (float) decreasingMultiplier(entity, 0.50);
    }

    public static double getFugitiveSpeedMultiplier(Entity entity) {
        return linearMultiplier(entity, 0.40);
    }

    public static double getGoldenSecondRewardChance(Entity entity) {
        return clampLevel(getSpawnFightingLevel(entity)) / 100.0;
    }

    public static double getPanicDurationMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static int getHotheadProjectileCount(Entity entity, int baseProjectileCount) {
        int level = clampLevel(getSpawnFightingLevel(entity));
        return Math.min(baseProjectileCount * 2, baseProjectileCount + (level / 25) * 2);
    }

    public static int getAlchemistCooldownTicks(Entity entity, int baseCooldownTicks) {
        return scaledCooldown(entity, baseCooldownTicks, 0.70);
    }

    public static int getGiantJumpBoostAmplifier(Entity entity) {
        int level = clampLevel(getSpawnFightingLevel(entity));
        return level >= 100 ? 1 : 0;
    }

    public static double getSwimmingWaterSpeedMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static double getDrownedKingChargeMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static double getChargingDistanceMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static double getChargingDamageMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static double getBalloonCloudMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static double getStalkerScaleModifier(Entity entity, double genericBaronScaleBonus) {
        int level = clampLevel(getSpawnFightingLevel(entity));
        double finalScale = Math.max(0.40, 0.80 - level * 0.004);
        return finalScale - 1.0 - genericBaronScaleBonus;
    }

    public static double getRunnerSpeedMultiplier(Entity entity) {
        return linearMultiplier(entity, 0.40);
    }

    public static double getInkBlindnessDurationMultiplier(Entity entity) {
        return linearMultiplier(entity, 1.0);
    }

    public static double getInfernoRampSpeedMultiplier(Entity entity) {
        return linearMultiplier(entity, 0.40);
    }

    public static int getThrowerCooldownTicks(Entity entity, int baseCooldownTicks) {
        return scaledCooldown(entity, baseCooldownTicks, 0.50);
    }

    public static int getThrowerSearchIntervalTicks(Entity entity, int baseIntervalTicks) {
        int level = clampLevel(getSpawnFightingLevel(entity));
        double frequencyMultiplier = 1.0 + level * 0.02;
        return Math.max(1, (int) Math.round(baseIntervalTicks / frequencyMultiplier));
    }

    private static double linearMultiplier(Entity entity, double bonusAtLevel100) {
        return 1.0 + clampLevel(getSpawnFightingLevel(entity)) * (bonusAtLevel100 / 100.0);
    }

    private static double decreasingMultiplier(Entity entity, double minimumAtLevel100) {
        int level = clampLevel(getSpawnFightingLevel(entity));
        return 1.0 - level * ((1.0 - minimumAtLevel100) / 100.0);
    }

    private static int scaledCooldown(Entity entity, int baseCooldownTicks, double multiplierAtLevel100) {
        int level = clampLevel(getSpawnFightingLevel(entity));
        double multiplier = 1.0 - level * ((1.0 - multiplierAtLevel100) / 100.0);
        return Math.max(1, (int) Math.round(baseCooldownTicks * multiplier));
    }

    private static int clampLevel(int fightingLevel) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, fightingLevel));
    }
}
