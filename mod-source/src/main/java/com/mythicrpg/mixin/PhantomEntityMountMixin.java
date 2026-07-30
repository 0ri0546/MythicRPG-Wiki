package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountDataAccess;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.PhantomEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PhantomEntity.class)
public abstract class PhantomEntityMountMixin {

    @Inject(method = "isDisallowedInPeaceful", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$keepAdoptedPhantomInPeaceful(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (mythicrpg$isAdopted()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canTarget", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedPhantomTargeting(
            EntityType<?> type,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (mythicrpg$isAdopted()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void mythicrpg$protectAdoptedPhantomFromDaylight(CallbackInfo ci) {
        if (mythicrpg$isAdopted()) {
            PhantomEntity self = (PhantomEntity) (Object) this;
            self.extinguish();
            self.fallDistance = 0.0F;
        }
    }

    @Unique
    private boolean mythicrpg$isAdopted() {
        return (Object) this instanceof LandMountDataAccess access
                && access.mythicrpg$isAdoptedLandMount();
    }
}
