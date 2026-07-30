package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the snow trail but removes the adopted Snow Golem's ranged attack. */
@Mixin(SnowGolemEntity.class)
public abstract class SnowGolemLandMountMixin {
    @Inject(method = "shootAt", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedSnowballAttack(
            LivingEntity target,
            float pullProgress,
            CallbackInfo ci
    ) {
        SnowGolemEntity self = (SnowGolemEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            ci.cancel();
        }
    }
}
