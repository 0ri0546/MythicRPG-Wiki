package com.mythicrpg.fighting.barons;

import com.mythicrpg.core.EntityCooldownManager;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class HotheadBaronBehavior {

    private static final double TRIGGER_RANGE = 24.0;
    private static final int BURST_COOLDOWN_TICKS = 80;
    private static final int PROJECTILE_COUNT = 8;
    private static final double PROJECTILE_SPEED = 0.65;
    private static final String COOLDOWN_HOTHEAD_BURST = "baron_hothead_burst";

    private HotheadBaronBehavior() {
    }

    public static void tick(ServerWorld world, BlazeEntity blaze) {
        if (!(blaze.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }

        if (!BaronEntityQuery.isValidPlayerTarget(target)) {
            return;
        }

        if (blaze.squaredDistanceTo(target) > TRIGGER_RANGE * TRIGGER_RANGE) {
            return;
        }

        if (!EntityCooldownManager.tryUse(blaze, COOLDOWN_HOTHEAD_BURST, BURST_COOLDOWN_TICKS)) {
            return;
        }

        fireBurst(blaze, world);
    }

    private static void fireBurst(BlazeEntity blaze, ServerWorld world) {
        double originX = blaze.getX();
        double originY = blaze.getBodyY(0.65);
        double originZ = blaze.getZ();

        int projectileCount = BaronScaling.getHotheadProjectileCount(blaze, PROJECTILE_COUNT);

        for (int i = 0; i < projectileCount; i++) {
            double angle = (Math.PI * 2.0 / projectileCount) * i;

            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);

            Vec3d direction = new Vec3d(dirX, 0.0, dirZ).normalize();

            SmallFireballEntity fireball = new SmallFireballEntity(
                    world,
                    blaze,
                    direction.multiply(PROJECTILE_SPEED)
            );

            fireball.refreshPositionAndAngles(
                    originX + direction.x * 1.2,
                    originY,
                    originZ + direction.z * 1.2,
                    blaze.getYaw(),
                    blaze.getPitch()
            );

            world.spawnEntity(fireball);
        }

        world.spawnParticles(
                ParticleTypes.FLAME,
                originX,
                originY,
                originZ,
                35,
                0.6,
                0.35,
                0.6,
                0.08
        );

        world.spawnParticles(
                ParticleTypes.LAVA,
                originX,
                originY,
                originZ,
                12,
                0.35,
                0.25,
                0.35,
                0.04
        );

        world.playSound(
                null,
                blaze.getBlockPos(),
                SoundEvents.ENTITY_BLAZE_SHOOT,
                SoundCategory.HOSTILE,
                1.1f,
                0.75f
        );
    }
}
