package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.BeeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps an adopted Bee neutral, alive and outside hives. */
@Mixin(BeeEntity.class)
public abstract class BeeFlyingMountMixin {
    @Invoker("setHasStung")
    protected abstract void mythicrpg$setHasStung(boolean hasStung);

    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedBeeSting(
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LandMountManager.isAdoptedMount((BeeEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canEnterHive", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$keepAdoptedBeeOutsideHive(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (LandMountManager.isAdoptedMount((BeeEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void mythicrpg$neutralizeAdoptedBee(CallbackInfo ci) {
        BeeEntity self = (BeeEntity) (Object) this;
        if (LandMountManager.isAdoptedMount(self)) {
            self.setAngerTime(0);
            self.setAngryAt(null);
            mythicrpg$setHasStung(false);
        }
    }
}
