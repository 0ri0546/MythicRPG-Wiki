package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.joml.Vector3f;

public final class DruidBaronBehavior {

    private static final DustParticleEffect DRUID_RED_DUST =
            new DustParticleEffect(new Vector3f(0.2f, 0.8f, 1.0f), 1.5f);

    private DruidBaronBehavior() {
    }

    public static void handleHit(LivingEntity target, Entity attacker) {
        if (!(target instanceof ServerPlayerEntity player)) {
            return;
        }

        if (!(attacker instanceof SkeletonEntity skeleton)) {
            return;
        }

        if (BaronMobManager.getBaronType(skeleton) != BaronType.DRUID) {
            return;
        }

        if (!(skeleton.getWorld() instanceof ServerWorld world)) {
            return;
        }

        skeleton.heal((float) (4.0f * BaronScaling.getDruidHealMultiplier(skeleton)));
        spawnHealParticles(world, player, skeleton);

        world.playSound(
                null,
                skeleton.getBlockPos(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL,
                SoundCategory.HOSTILE,
                0.45f,
                1.3f
        );
    }

    private static void spawnHealParticles(
            ServerWorld world,
            LivingEntity from,
            LivingEntity to
    ) {
        double startX = from.getX();
        double startY = from.getBodyY(0.6);
        double startZ = from.getZ();

        double endX = to.getX();
        double endY = to.getBodyY(0.6);
        double endZ = to.getZ();

        int steps = 18;

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;

            double x = startX + (endX - startX) * t;
            double y = startY + (endY - startY) * t;
            double z = startZ + (endZ - startZ) * t;

            world.spawnParticles(
                    DRUID_RED_DUST,
                    x,
                    y,
                    z,
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.0
            );
        }
    }
}
