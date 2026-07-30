package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class SurvivorBaronBehavior {

    private SurvivorBaronBehavior() {
    }

    public static boolean allowDamage(LivingEntity target, DamageSource source) {
        if (!(target instanceof ZombieEntity)) {
            return true;
        }

        if (BaronMobManager.getBaronType(target) != BaronType.SURVIVOR) {
            return true;
        }

        if (isDirectPlayerMeleeDamage(source)) {
            return true;
        }

        if (target.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    target.getX(),
                    target.getBodyY(0.6),
                    target.getZ(),
                    6,
                    0.25,
                    0.3,
                    0.25,
                    0.01
            );
        }

        return false;
    }

    private static boolean isDirectPlayerMeleeDamage(DamageSource source) {
        Entity attacker = source.getAttacker();
        Entity sourceEntity = source.getSource();

        return attacker instanceof ServerPlayerEntity
                && sourceEntity instanceof ServerPlayerEntity;
    }
}
