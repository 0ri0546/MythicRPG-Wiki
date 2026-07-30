package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountControl;
import com.mythicrpg.traveling.LandMountDataAccess;
import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PigEntity overrides all vanilla ridden-movement methods and otherwise forces
 * a constant forward input. Adopted pigs must therefore use MythicRPG's shared
 * ground-mount controls explicitly.
 */
@Mixin(PigEntity.class)
public abstract class PigEntityLandMountMixin {
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getAdoptedPigController(
            CallbackInfoReturnable<LivingEntity> cir
    ) {
        PigEntity self = (PigEntity) (Object) this;

        if (self.getFirstPassenger() instanceof PlayerEntity player
                && LandMountControl.canControl(self, player)) {
            cir.setReturnValue(player);
        }
    }

    @Inject(method = "getControlledMovementInput", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getAdoptedPigMovementInput(
            PlayerEntity player,
            Vec3d movementInput,
            CallbackInfoReturnable<Vec3d> cir
    ) {
        PigEntity self = (PigEntity) (Object) this;

        if (LandMountControl.canControl(self, player)) {
            cir.setReturnValue(LandMountControl.movementInput(player));
        }
    }

    @Inject(method = "getSaddledSpeed", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getAdoptedPigSpeed(
            PlayerEntity player,
            CallbackInfoReturnable<Float> cir
    ) {
        PigEntity self = (PigEntity) (Object) this;

        if (LandMountControl.canControl(self, player)) {
            cir.setReturnValue(LandMountControl.movementSpeed(self));
        }
    }

    @Inject(method = "tickControlled", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$tickAdoptedPigControl(
            PlayerEntity player,
            Vec3d movementInput,
            CallbackInfo ci
    ) {
        PigEntity self = (PigEntity) (Object) this;

        if (!(self instanceof LandMountDataAccess access)
                || !LandMountControl.canControl(self, player)) {
            return;
        }

        boolean jumpPressed = ((LivingEntityJumpingAccessor) player)
                .mythicrpg$isJumping();

        LandMountControl.tickGroundControl(self, player, access, jumpPressed);
        ci.cancel();
    }
}
