package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountControl;
import com.mythicrpg.traveling.LandMountDataAccess;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Llamas use the horse interaction and riding pipeline instead of the generic
 * MobEntity/LivingEntity methods used by the existing MythicRPG mounts.
 */
@Mixin(AbstractHorseEntity.class)
public abstract class AbstractHorseEntityLandMountMixin {
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getAdoptedLlamaController(
            CallbackInfoReturnable<LivingEntity> cir
    ) {
        AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;

        if (self instanceof LlamaEntity llama
                && llama.getFirstPassenger() instanceof PlayerEntity player
                && LandMountControl.canControl(llama, player)) {
            cir.setReturnValue(player);
        }
    }

    @Inject(method = "getControlledMovementInput", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getAdoptedLlamaMovementInput(
            PlayerEntity player,
            Vec3d movementInput,
            CallbackInfoReturnable<Vec3d> cir
    ) {
        AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;

        if (self instanceof LlamaEntity llama
                && LandMountControl.canControl(llama, player)) {
            cir.setReturnValue(LandMountControl.movementInput(player));
        }
    }

    @Inject(method = "getSaddledSpeed", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$getAdoptedLlamaSpeed(
            PlayerEntity player,
            CallbackInfoReturnable<Float> cir
    ) {
        AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;

        if (self instanceof LlamaEntity llama
                && LandMountControl.canControl(llama, player)) {
            cir.setReturnValue(LandMountControl.movementSpeed(llama));
        }
    }

    @Inject(method = "tickControlled", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$tickAdoptedLlamaControl(
            PlayerEntity player,
            Vec3d movementInput,
            CallbackInfo ci
    ) {
        AbstractHorseEntity self = (AbstractHorseEntity) (Object) this;

        if (!(self instanceof LlamaEntity llama)
                || !(llama instanceof LandMountDataAccess access)
                || !LandMountControl.canControl(llama, player)) {
            return;
        }

        boolean jumpPressed = ((LivingEntityJumpingAccessor) player)
                .mythicrpg$isJumping();

        LandMountControl.tickGroundControl(llama, player, access, jumpPressed);
        ci.cancel();
    }
}
