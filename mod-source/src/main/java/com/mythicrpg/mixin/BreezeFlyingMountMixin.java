package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.BreezeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents the Breeze brain from rebuilding an attack target. */
@Mixin(BreezeEntity.class)
public abstract class BreezeFlyingMountMixin {
    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedBreezeTargeting(
            EntityType<?> type,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LandMountManager.isAdoptedMount((BreezeEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getTarget", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$hideAdoptedBreezeTarget(
            CallbackInfoReturnable<LivingEntity> cir
    ) {
        if (LandMountManager.isAdoptedMount((BreezeEntity) (Object) this)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "mobTick", at = @At("HEAD"))
    private void mythicrpg$clearAdoptedBreezeBrainBeforeTick(CallbackInfo ci) {
        BreezeEntity self = (BreezeEntity) (Object) this;
        if (LandMountManager.isAdoptedMount(self)) {
            LandMountManager.maintainAdoptedMountState(self);
        }
    }

    @Inject(method = "mobTick", at = @At("TAIL"))
    private void mythicrpg$clearAdoptedBreezeBrainAfterTick(CallbackInfo ci) {
        BreezeEntity self = (BreezeEntity) (Object) this;
        if (LandMountManager.isAdoptedMount(self)) {
            LandMountManager.maintainAdoptedMountState(self);
        }
    }
}
