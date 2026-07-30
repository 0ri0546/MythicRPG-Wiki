package com.mythicrpg.fighting.barons;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public final class RunnerBaronBehavior {

    private RunnerBaronBehavior() {
    }

    public static void applyPromotion(CreeperEntity creeper, ServerWorld world) {
        creeper.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, -1, 1, true, true));

        world.spawnParticles(
                ParticleTypes.CLOUD,
                creeper.getX(),
                creeper.getBodyY(0.4),
                creeper.getZ(),
                12,
                0.25,
                0.25,
                0.25,
                0.03
        );
    }
}
