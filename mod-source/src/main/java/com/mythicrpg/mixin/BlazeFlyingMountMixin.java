package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.mob.BlazeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Clears the Blaze's charged-fireball state after adoption. */
@Mixin(BlazeEntity.class)
public abstract class BlazeFlyingMountMixin {
    @Invoker("setFireActive")
    protected abstract void mythicrpg$setFireActive(boolean fireActive);

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void mythicrpg$neutralizeAdoptedBlaze(CallbackInfo ci) {
        BlazeEntity self = (BlazeEntity) (Object) this;
        if (LandMountManager.isAdoptedMount(self)) {
            mythicrpg$setFireActive(false);
            self.setTarget(null);
            self.setAttacking(false);
        }
    }
}
