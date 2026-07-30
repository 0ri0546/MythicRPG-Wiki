package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class PanicBaronBehavior {

    private PanicBaronBehavior() {
    }

    public static void handleHit(LivingEntity target) {
        if (BaronMobManager.getBaronType(target) != BaronType.PANIC) {
            return;
        }

        int panicDurationTicks = Math.max(1, (int) Math.round(80 * BaronScaling.getPanicDurationMultiplier(target)));

        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                panicDurationTicks,
                2,
                true,
                true
        ));

        target.addStatusEffect(new StatusEffectInstance(
                StatusEffects.JUMP_BOOST,
                panicDurationTicks,
                2,
                true,
                true
        ));

        if (!(target.getWorld() instanceof ServerWorld world)) {
            return;
        }

        world.spawnParticles(
                ParticleTypes.CLOUD,
                target.getX(),
                target.getBodyY(0.5),
                target.getZ(),
                12,
                0.35,
                0.25,
                0.35,
                0.05
        );

        world.playSound(
                null,
                target.getBlockPos(),
                SoundEvents.ENTITY_RABBIT_JUMP,
                SoundCategory.NEUTRAL,
                0.55f,
                1.5f
        );
    }
}
