package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.mob.GhastEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Refuses every attempt to charge a fireball after the Ghast is adopted. */
@Mixin(GhastEntity.class)
public abstract class GhastFlyingMountMixin {
    @Inject(method = "setShooting", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedGhastShooting(
            boolean shooting,
            CallbackInfo ci
    ) {
        if (shooting
                && LandMountManager.isAdoptedMount((GhastEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
