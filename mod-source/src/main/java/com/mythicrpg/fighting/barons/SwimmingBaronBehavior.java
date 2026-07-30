package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class SwimmingBaronBehavior {

    private static final double MAX_TARGET_DISTANCE = 32.0;

    private SwimmingBaronBehavior() {
    }

    public static boolean allowDamage(LivingEntity target, DamageSource source) {
        if (!(target instanceof EndermanEntity)) {
            return true;
        }

        if (BaronMobManager.getBaronType(target) != BaronType.SWIMMING) {
            return true;
        }

        if (!source.isOf(DamageTypes.DROWN)) {
            return true;
        }

        if (target.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(
                    ParticleTypes.SPLASH,
                    target.getX(),
                    target.getBodyY(0.5),
                    target.getZ(),
                    8,
                    0.25,
                    0.35,
                    0.25,
                    0.04
            );
        }

        return false;
    }

    public static void tick(ServerWorld world, EndermanEntity enderman) {
        if (!(enderman.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }

        if (!BaronEntityQuery.isValidPlayerTarget(target)) {
            return;
        }

        if (!enderman.isTouchingWater() && !enderman.isWet()) {
            return;
        }

        if (enderman.squaredDistanceTo(target) > MAX_TARGET_DISTANCE * MAX_TARGET_DISTANCE) {
            return;
        }

        Vec3d toTarget = target.getPos().subtract(enderman.getPos());

        if (toTarget.lengthSquared() <= 0.001) {
            return;
        }

        Vec3d horizontalDirection = new Vec3d(toTarget.x, 0.0, toTarget.z);

        if (horizontalDirection.lengthSquared() > 0.001) {
            horizontalDirection = horizontalDirection.normalize();
        } else {
            horizontalDirection = Vec3d.ZERO;
        }

        double waterSpeedMultiplier = BaronScaling.getSwimmingWaterSpeedMultiplier(enderman);
        double upwardBoost = (target.getY() > enderman.getY() ? 0.18 : 0.10) * waterSpeedMultiplier;
        double horizontalBoost = 0.18 * waterSpeedMultiplier;
        Vec3d currentVelocity = enderman.getVelocity();

        enderman.setVelocity(new Vec3d(
                currentVelocity.x * 0.6 + horizontalDirection.x * horizontalBoost,
                Math.max(currentVelocity.y, upwardBoost),
                currentVelocity.z * 0.6 + horizontalDirection.z * horizontalBoost
        ));
        enderman.velocityModified = true;

        world.spawnParticles(
                ParticleTypes.SPLASH,
                enderman.getX(),
                enderman.getBodyY(0.4),
                enderman.getZ(),
                6,
                0.25,
                0.25,
                0.25,
                0.03
        );
    }
}
