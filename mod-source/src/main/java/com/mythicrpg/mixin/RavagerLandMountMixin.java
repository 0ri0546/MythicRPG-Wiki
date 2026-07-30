package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.mob.RavagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the damaging area roar from reactivating on an adopted Ravager. */
@Mixin(RavagerEntity.class)
public abstract class RavagerLandMountMixin {
    @Inject(method = "roar", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedRavagerRoar(CallbackInfo ci) {
        RavagerEntity self = (RavagerEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            ci.cancel();
        }
    }
}
