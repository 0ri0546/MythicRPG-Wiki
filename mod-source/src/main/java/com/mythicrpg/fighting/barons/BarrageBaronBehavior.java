package com.mythicrpg.fighting.barons;

import com.mythicrpg.core.EntityCooldownManager;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class BarrageBaronBehavior {

    private static final int SHOT_COOLDOWN_TICKS = 35;
    private static final double MAX_TARGET_DISTANCE = 32.0;
    private static final String COOLDOWN_BARRAGE_SHOT = "baron_barrage_shot";

    private BarrageBaronBehavior() {
    }

    public static void tick(ServerWorld world, SkeletonEntity skeleton) {
        if (!(skeleton.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }

        if (!BaronEntityQuery.isValidPlayerTarget(target)) {
            return;
        }

        if (skeleton.squaredDistanceTo(target) > MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE) {
            return;
        }

        int cooldownTicks = BaronScaling.getBarrageCooldownTicks(skeleton, SHOT_COOLDOWN_TICKS);

        if (!EntityCooldownManager.tryUse(skeleton, COOLDOWN_BARRAGE_SHOT, cooldownTicks)) {
            return;
        }

        if (skeleton instanceof RangedAttackMob rangedAttackMob) {
            rangedAttackMob.shootAt(target, 1.0f);
        }

        world.spawnParticles(
                ParticleTypes.CRIT,
                skeleton.getX(),
                skeleton.getBodyY(0.7),
                skeleton.getZ(),
                12,
                0.25,
                0.35,
                0.25,
                0.08
        );

        world.playSound(
                null,
                skeleton.getBlockPos(),
                SoundEvents.ENTITY_SKELETON_SHOOT,
                SoundCategory.HOSTILE,
                0.65f,
                1.35f
        );
    }
}
