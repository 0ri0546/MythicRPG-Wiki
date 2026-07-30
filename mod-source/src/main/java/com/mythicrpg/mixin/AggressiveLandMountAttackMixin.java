package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.ZoglinEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hoglins, Zoglins and Ravagers override tryAttack, so the generic
 * LivingEntity hook cannot guarantee that their vanilla attack is blocked.
 */
@Mixin({HoglinEntity.class, ZoglinEntity.class, RavagerEntity.class})
public abstract class AggressiveLandMountAttackMixin {
    @Inject(method = "tryAttack", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedSpecialAttack(
            Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MobEntity self = (MobEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }
}
