package com.mythicrpg.fighting.barons;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class GiantBaronBehavior {

    private GiantBaronBehavior() {
    }

    public static void applyPromotion(SlimeEntity slime, ServerWorld world) {
        int currentSize = slime.getSize();
        int newSize = Math.min(currentSize * 2, 8);

        slime.setSize(newSize, true);
        slime.addStatusEffect(new StatusEffectInstance(
                StatusEffects.JUMP_BOOST,
                -1,
                BaronScaling.getGiantJumpBoostAmplifier(slime),
                true,
                true
        ));

        world.spawnParticles(
                ParticleTypes.POOF,
                slime.getX(),
                slime.getBodyY(0.5),
                slime.getZ(),
                25,
                0.6,
                0.4,
                0.6,
                0.05
        );

        world.playSound(
                null,
                slime.getBlockPos(),
                SoundEvents.ENTITY_SLIME_SQUISH,
                SoundCategory.HOSTILE,
                0.9f,
                0.7f
        );
    }
}
