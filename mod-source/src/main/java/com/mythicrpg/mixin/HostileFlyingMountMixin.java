package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps adopted hostile flying mounts alive when the world is Peaceful. */
@Mixin(HostileEntity.class)
public abstract class HostileFlyingMountMixin {
    @Inject(method = "isDisallowedInPeaceful", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$keepAdoptedFlyingMountInPeaceful(
            CallbackInfoReturnable<Boolean> cir
    ) {
        MobEntity self = (MobEntity) (Object) this;
        if (LandMountManager.isAdoptedMount(self)
                && LandMountManager.isFlyingMount(self)) {
            cir.setReturnValue(false);
        }
    }
}
