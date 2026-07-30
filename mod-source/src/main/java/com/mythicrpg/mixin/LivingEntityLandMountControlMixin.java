package com.mythicrpg.mixin;

import com.mythicrpg.traveling.FlyingMountController;
import com.mythicrpg.traveling.LandMountDataAccess;
import com.mythicrpg.traveling.LandMountManager;
import com.mythicrpg.traveling.LandMountType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLandMountControlMixin {

    @Inject(method = "getControlledMovementInput", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getLandMountMovementInput(
            PlayerEntity player,
            Vec3d movementInput,
            CallbackInfoReturnable<Vec3d> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof MobEntity mob)
                || !LandMountManager.isAdoptedMount(self)
                || !LandMountManager.isOwner(mob, player)) {
            return;
        }

        if (LandMountType.fromEntity(mob).map(LandMountType::isFlying).orElse(false)) {
            cir.setReturnValue(FlyingMountController.movementInput());
            return;
        }

        float sideways = player.sidewaysSpeed * 0.5F;
        float forward = player.forwardSpeed;

        if (forward <= 0.0F) {
            forward *= 0.25F;
        }

        cir.setReturnValue(new Vec3d(sideways, 0.0D, forward));
    }

    @Inject(method = "getSaddledSpeed", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getLandMountSpeed(
            PlayerEntity player,
            CallbackInfoReturnable<Float> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof MobEntity mob)
                || !LandMountManager.isAdoptedMount(self)
                || !LandMountManager.isOwner(mob, player)) {
            return;
        }

        if (LandMountType.fromEntity(mob).map(LandMountType::isFlying).orElse(false)) {
            cir.setReturnValue(0.0F);
            return;
        }

        float movementSpeed = (float) self.getAttributeValue(
                EntityAttributes.GENERIC_MOVEMENT_SPEED
        );
        cir.setReturnValue(Math.max(0.20F, movementSpeed));
    }

    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedMountAttacks(
            net.minecraft.entity.Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tickControlled", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$tickLandMountControl(
            PlayerEntity player,
            Vec3d movementInput,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (!(self instanceof MobEntity mob)
                || !(mob instanceof LandMountDataAccess access)
                || !access.mythicrpg$isAdoptedLandMount()
                || !LandMountManager.isOwner(mob, player)) {
            return;
        }

        boolean jumpPressed = ((LivingEntityJumpingAccessor) player)
                .mythicrpg$isJumping();

        if (LandMountType.fromEntity(mob).map(LandMountType::isFlying).orElse(false)) {
            FlyingMountController.tickFlyingMount(mob, player, jumpPressed);
            access.mythicrpg$setRiderJumpPressed(jumpPressed);
            ci.cancel();
            return;
        }

        self.setYaw(player.getYaw());
        self.setPitch(player.getPitch() * 0.5F);
        self.setBodyYaw(self.getYaw());
        self.setHeadYaw(self.getYaw());

        if (jumpPressed
                && !access.mythicrpg$wasRiderJumpPressed()
                && self.isOnGround()) {
            self.jump();
        }

        access.mythicrpg$setRiderJumpPressed(jumpPressed);
        ci.cancel();
    }
}
