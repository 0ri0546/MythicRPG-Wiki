package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adopted villagers keep their profession/offers but can no longer breed. */
@Mixin(VillagerEntity.class)
public abstract class VillagerLandMountMixin {
    @Inject(method = "canBreed", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedVillagerBreeding(
            CallbackInfoReturnable<Boolean> cir
    ) {
        VillagerEntity self = (VillagerEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "wantsToStartBreeding", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedVillagerBreedingStart(
            CallbackInfoReturnable<Boolean> cir
    ) {
        VillagerEntity self = (VillagerEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }
}
