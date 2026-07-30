package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class UndyingWolfBaronBehavior {

    private static final float MIN_HEALTH_WHEN_TAMED = 1.0F;
    private static final float WOUNDED_RATIO = 0.25F;
    private static final float RECOVERED_RATIO = 0.60F;

    private static final Set<UUID> WOUNDED_WOLVES = new HashSet<>();

    private UndyingWolfBaronBehavior() {
    }

    public static boolean allowDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(target instanceof WolfEntity wolf)) {
            return true;
        }

        if (BaronMobManager.getBaronType(wolf) != BaronType.UNDYING_WOLF) {
            return true;
        }

        if (!wolf.isTamed()) {
            return true;
        }

        if (wolf.getHealth() - amount > MIN_HEALTH_WHEN_TAMED) {
            return true;
        }

        wolf.setHealth(MIN_HEALTH_WHEN_TAMED);
        wolf.setTarget(null);
        WOUNDED_WOLVES.add(wolf.getUuid());

        if (wolf.getWorld() instanceof ServerWorld world) {
            spawnWoundedParticles(world, wolf, 8);
        }

        return false;
    }

    public static boolean allowOutgoingDamage(LivingEntity attacker) {
        if (!(attacker instanceof WolfEntity wolf)) {
            return true;
        }

        if (BaronMobManager.getBaronType(wolf) != BaronType.UNDYING_WOLF) {
            return true;
        }

        if (!wolf.isTamed()) {
            return true;
        }

        return !isInWoundedState(wolf);
    }

    public static void cleanup(LivingEntity entity) {
        if (entity instanceof WolfEntity wolf) {
            WOUNDED_WOLVES.remove(wolf.getUuid());
        }
    }

    public static void clearAll() {
        WOUNDED_WOLVES.clear();
    }

    public static void tick(ServerWorld world, WolfEntity wolf) {
        if (!wolf.isTamed()) {
            if (world.getTime() % 40L == 0L) {
                spawnWildParticles(world, wolf);
            }
            return;
        }

        boolean wasWounded = isInWoundedState(wolf);

        if (isWounded(wolf)) {
            WOUNDED_WOLVES.add(wolf.getUuid());
        }

        if (wasWounded && getHealthRatio(wolf) >= RECOVERED_RATIO) {
            WOUNDED_WOLVES.remove(wolf.getUuid());
            spawnRecoveredParticles(world, wolf);
            return;
        }

        if (isInWoundedState(wolf)) {
            wolf.setTarget(null);

            if (world.getTime() % 20L == 0L) {
                spawnWoundedParticles(world, wolf, 2);
            }
        }
    }

    private static void spawnWildParticles(ServerWorld world, WolfEntity wolf) {
        world.spawnParticles(
                ParticleTypes.SOUL,
                wolf.getX(),
                wolf.getBodyY(0.7),
                wolf.getZ(),
                2,
                0.18,
                0.22,
                0.18,
                0.005
        );
    }

    private static void spawnWoundedParticles(ServerWorld world, WolfEntity wolf, int count) {
        world.spawnParticles(
                ParticleTypes.SMOKE,
                wolf.getX(),
                wolf.getBodyY(0.65),
                wolf.getZ(),
                count,
                0.2,
                0.18,
                0.2,
                0.01
        );
    }

    private static void spawnRecoveredParticles(ServerWorld world, WolfEntity wolf) {
        world.spawnParticles(
                ParticleTypes.HEART,
                wolf.getX(),
                wolf.getBodyY(0.85),
                wolf.getZ(),
                5,
                0.25,
                0.25,
                0.25,
                0.02
        );

        world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                wolf.getX(),
                wolf.getBodyY(0.65),
                wolf.getZ(),
                6,
                0.25,
                0.2,
                0.25,
                0.01
        );
    }

    private static boolean isWounded(WolfEntity wolf) {
        return getHealthRatio(wolf) <= WOUNDED_RATIO;
    }

    private static boolean isInWoundedState(WolfEntity wolf) {
        return WOUNDED_WOLVES.contains(wolf.getUuid()) || isWounded(wolf);
    }

    private static float getHealthRatio(WolfEntity wolf) {
        float maxHealth = wolf.getMaxHealth();
        if (maxHealth <= 0.0F) {
            return 0.0F;
        }

        return wolf.getHealth() / maxHealth;
    }
}
