package com.mythicrpg.traveling;

import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Shared server-authoritative controller for every adopted flying mount. */
public final class FlyingMountController {
    private static final float INPUT_EPSILON = 0.01F;

    private FlyingMountController() {
    }

    public static boolean isControlledFlyingMount(MobEntity mount, PlayerEntity player) {
        return LandMountManager.isFlyingMount(mount)
                && LandMountManager.isOwner(mount, player);
    }

    public static void tickFlyingMount(
            MobEntity mount,
            PlayerEntity player,
            boolean ascendPressed
    ) {
        if (!isControlledFlyingMount(mount, player)) {
            return;
        }

        LandMountType type = LandMountType.fromEntity(mount).orElse(null);
        double cruiseSpeed = resolveCruiseSpeed(mount, type);
        float turnInput = player.sidewaysSpeed;
        float yaw = MathHelper.wrapDegrees(
                mount.getYaw() - turnInput * FlyingMountConfig.TURN_DEGREES_PER_TICK
        );

        boolean descendPressed = player.forwardSpeed < -INPUT_EPSILON;
        double verticalSpeed;
        float pitch;

        if (ascendPressed) {
            verticalSpeed = type == LandMountType.PHANTOM
                    ? FlyingMountConfig.PHANTOM_ASCEND_SPEED
                    : verticalSpeed(
                            cruiseSpeed,
                            FlyingMountConfig.ASCEND_SPEED_MULTIPLIER
                    );
            pitch = FlyingMountConfig.ASCENDING_PITCH;
        } else if (descendPressed) {
            verticalSpeed = -(type == LandMountType.PHANTOM
                    ? FlyingMountConfig.PHANTOM_DESCEND_SPEED
                    : verticalSpeed(
                            cruiseSpeed,
                            FlyingMountConfig.DESCEND_SPEED_MULTIPLIER
                    ));
            pitch = FlyingMountConfig.DESCENDING_PITCH;
        } else {
            verticalSpeed = 0.0D;
            pitch = 0.0F;
        }

        double groundSpeed = type == LandMountType.PHANTOM
                ? FlyingMountConfig.PHANTOM_GROUND_SPEED
                : cruiseSpeed * FlyingMountConfig.GROUND_SPEED_MULTIPLIER;
        double horizontalSpeed = mount.isOnGround() && !ascendPressed
                ? groundSpeed
                : cruiseSpeed;

        float yawRadians = yaw * MathHelper.RADIANS_PER_DEGREE;
        double velocityX = -MathHelper.sin(yawRadians) * horizontalSpeed;
        double velocityZ = MathHelper.cos(yawRadians) * horizontalSpeed;

        mount.setNoGravity(true);
        mount.setYaw(yaw);
        mount.setBodyYaw(yaw);
        mount.setHeadYaw(yaw);
        mount.setPitch(pitch);
        mount.setVelocity(velocityX, verticalSpeed, velocityZ);
        mount.fallDistance = 0.0F;
        mount.setTarget(null);
        mount.setAttacking(false);
        mount.getNavigation().stop();
    }

    public static void returnToAnchor(MobEntity mount, int anchorX, int anchorZ) {
        double deltaX = anchorX + 0.5D - mount.getX();
        double deltaZ = anchorZ + 0.5D - mount.getZ();
        double horizontalLength = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalLength < 0.001D) {
            return;
        }

        double speed = FlyingMountConfig.ANCHOR_RETURN_SPEED;
        mount.setVelocity(
                deltaX / horizontalLength * speed,
                MathHelper.clamp(mount.getVelocity().y, -0.08D, 0.08D),
                deltaZ / horizontalLength * speed
        );
    }

    public static Vec3d movementInput() {
        return Vec3d.ZERO;
    }

    private static double resolveCruiseSpeed(
            MobEntity mount,
            LandMountType type
    ) {
        if (type == LandMountType.PHANTOM) {
            return FlyingMountConfig.PHANTOM_CRUISE_SPEED;
        }
        if (type == LandMountType.GHAST) {
            return FlyingMountConfig.GHAST_FALLBACK_CRUISE_SPEED;
        }

        EntityAttributeInstance flyingSpeed = mount.getAttributeInstance(
                EntityAttributes.GENERIC_FLYING_SPEED
        );
        if (flyingSpeed != null && flyingSpeed.getValue() > 0.0D) {
            return MathHelper.clamp(
                    flyingSpeed.getValue(),
                    FlyingMountConfig.MIN_CRUISE_SPEED,
                    FlyingMountConfig.MAX_CRUISE_SPEED
            );
        }

        EntityAttributeInstance movementSpeed = mount.getAttributeInstance(
                EntityAttributes.GENERIC_MOVEMENT_SPEED
        );
        if (movementSpeed != null && movementSpeed.getValue() > 0.0D) {
            return MathHelper.clamp(
                    movementSpeed.getValue(),
                    FlyingMountConfig.MIN_CRUISE_SPEED,
                    FlyingMountConfig.MAX_CRUISE_SPEED
            );
        }

        return FlyingMountConfig.GHAST_FALLBACK_CRUISE_SPEED;
    }

    private static double verticalSpeed(double cruiseSpeed, double multiplier) {
        return MathHelper.clamp(
                cruiseSpeed * multiplier,
                FlyingMountConfig.MIN_VERTICAL_SPEED,
                FlyingMountConfig.MAX_VERTICAL_SPEED
        );
    }
}
