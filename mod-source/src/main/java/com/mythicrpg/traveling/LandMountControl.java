package com.mythicrpg.traveling;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Shared controls for adopted ground mounts whose vanilla entity class
 * overrides Minecraft's normal ridden-movement methods (pig, llama, etc.).
 */
public final class LandMountControl {
    private LandMountControl() {
    }

    public static boolean canControl(MobEntity mob, PlayerEntity player) {
        return mob instanceof LandMountDataAccess access
                && access.mythicrpg$isAdoptedLandMount()
                && LandMountManager.isOwner(mob, player);
    }

    public static Vec3d movementInput(PlayerEntity player) {
        float sideways = player.sidewaysSpeed * 0.5F;
        float forward = player.forwardSpeed;

        if (forward <= 0.0F) {
            forward *= 0.25F;
        }

        return new Vec3d(sideways, 0.0D, forward);
    }

    public static float movementSpeed(LivingEntity mount) {
        float movementSpeed = (float) mount.getAttributeValue(
                EntityAttributes.GENERIC_MOVEMENT_SPEED
        );
        return Math.max(0.20F, movementSpeed);
    }

    public static void tickGroundControl(
            MobEntity mob,
            PlayerEntity player,
            LandMountDataAccess access,
            boolean jumpPressed
    ) {
        mob.setYaw(player.getYaw());
        mob.setPitch(player.getPitch() * 0.5F);
        mob.setBodyYaw(mob.getYaw());
        mob.setHeadYaw(mob.getYaw());

        if (jumpPressed
                && !access.mythicrpg$wasRiderJumpPressed()
                && mob.isOnGround()) {
            mob.jump();
        }

        access.mythicrpg$setRiderJumpPressed(jumpPressed);
    }
}
