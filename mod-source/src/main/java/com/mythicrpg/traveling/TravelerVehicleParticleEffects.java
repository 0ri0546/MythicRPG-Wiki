package com.mythicrpg.traveling;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Lightweight particle polish executed directly by each loaded custom vehicle.
 * No player scan or world-wide entity scan is required.
 */
public final class TravelerVehicleParticleEffects {

    private TravelerVehicleParticleEffects() {
    }

    public static void tickMinecart(TravelerMinecartEntity minecart) {
        World world = minecart.getWorld();
        if (!world.isClient()
                || !isParticleTick(
                        minecart.getId(),
                        minecart.age,
                        TravelerVehicleParticleConfig.MINECART_PARTICLE_INTERVAL_TICKS
                )) {
            return;
        }

        Vec3d velocity = minecart.getVelocity();
        double horizontalSpeedSquared = horizontalSpeedSquared(velocity);
        double minimumSpeed = TravelerVehicleParticleConfig.MINECART_MIN_HORIZONTAL_SPEED;

        if (horizontalSpeedSquared < minimumSpeed * minimumSpeed) {
            return;
        }

        double inverseSpeed = 1.0D / Math.sqrt(horizontalSpeedSquared);
        double directionX = velocity.x * inverseSpeed;
        double directionZ = velocity.z * inverseSpeed;
        double sideSign = ((minecart.age / TravelerVehicleParticleConfig.MINECART_PARTICLE_INTERVAL_TICKS) & 1) == 0
                ? -1.0D
                : 1.0D;
        double sideOffset = TravelerVehicleParticleConfig.MINECART_SIDE_OFFSET * sideSign;

        world.addParticle(
                ParticleTypes.ELECTRIC_SPARK,
                minecart.getX()
                        - directionX * TravelerVehicleParticleConfig.MINECART_REAR_OFFSET
                        - directionZ * sideOffset,
                minecart.getY() + TravelerVehicleParticleConfig.MINECART_PARTICLE_HEIGHT,
                minecart.getZ()
                        - directionZ * TravelerVehicleParticleConfig.MINECART_REAR_OFFSET
                        + directionX * sideOffset,
                0.0D,
                0.0D,
                0.0D
        );
    }

    public static void tickBoat(TravelerBoatEntity boat) {
        World world = boat.getWorld();
        if (!world.isClient()
                || !isParticleTick(
                        boat.getId(),
                        boat.age,
                        TravelerVehicleParticleConfig.BOAT_PARTICLE_INTERVAL_TICKS
                )
                || !boat.isTouchingWater()) {
            return;
        }

        Vec3d velocity = boat.getVelocity();
        double horizontalSpeedSquared = horizontalSpeedSquared(velocity);
        double minimumSpeed = TravelerVehicleParticleConfig.BOAT_MIN_HORIZONTAL_SPEED;

        if (horizontalSpeedSquared < minimumSpeed * minimumSpeed) {
            return;
        }

        double inverseSpeed = 1.0D / Math.sqrt(horizontalSpeedSquared);
        double directionX = velocity.x * inverseSpeed;
        double directionZ = velocity.z * inverseSpeed;
        double sideSign = ((boat.age / TravelerVehicleParticleConfig.BOAT_PARTICLE_INTERVAL_TICKS) & 1) == 0
                ? -1.0D
                : 1.0D;
        double sideOffset = TravelerVehicleParticleConfig.BOAT_SIDE_OFFSET * sideSign;

        world.addParticle(
                ParticleTypes.SPLASH,
                boat.getX()
                        - directionX * TravelerVehicleParticleConfig.BOAT_REAR_OFFSET
                        - directionZ * sideOffset,
                boat.getY() + TravelerVehicleParticleConfig.BOAT_PARTICLE_HEIGHT,
                boat.getZ()
                        - directionZ * TravelerVehicleParticleConfig.BOAT_REAR_OFFSET
                        + directionX * sideOffset,
                -directionX * 0.02D,
                0.01D,
                -directionZ * 0.02D
        );
    }

    private static boolean isParticleTick(int entityId, int age, int intervalTicks) {
        return intervalTicks > 0 && Math.floorMod(age + entityId, intervalTicks) == 0;
    }

    private static double horizontalSpeedSquared(Vec3d velocity) {
        return velocity.x * velocity.x + velocity.z * velocity.z;
    }
}
