package com.mythicrpg.mixin;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin {

    @Inject(method = "teleportRandomly", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventSpecialEndermanTeleport(
            CallbackInfoReturnable<Boolean> cir
    ) {
        EndermanEntity enderman = (EndermanEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(enderman)) {
            cir.setReturnValue(false);
            return;
        }

        if (!BaronMobManager.isBaron(enderman)) {
            return;
        }

        if (BaronMobManager.getBaronType(enderman) != BaronType.SWIMMING) {
            return;
        }

        if (!enderman.isWet()) {
            return;
        }

        cir.setReturnValue(false);
    }

    /** Final internal teleport path used by damage, water and target reactions. */
    @Inject(method = "teleportTo(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedEndermanTeleportToCoordinates(
            double x,
            double y,
            double z,
            CallbackInfoReturnable<Boolean> cir
    ) {
        EndermanEntity self = (EndermanEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "teleportTo(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedEndermanTeleportToEntity(
            net.minecraft.entity.Entity target,
            CallbackInfoReturnable<Boolean> cir
    ) {
        EndermanEntity self = (EndermanEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedEndermanTarget(
            @Nullable LivingEntity target,
            CallbackInfo ci
    ) {
        EndermanEntity self = (EndermanEntity) (Object) this;

        if (target != null && LandMountManager.isAdoptedMount(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "isPlayerStaring", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$ignoreStaringAtAdoptedEnderman(
            PlayerEntity player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        EndermanEntity self = (EndermanEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setProvoked", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedEndermanProvocation(CallbackInfo ci) {
        EndermanEntity self = (EndermanEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "isAngry", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$hideAdoptedEndermanAnger(
            CallbackInfoReturnable<Boolean> cir
    ) {
        EndermanEntity self = (EndermanEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isProvoked", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$hideAdoptedEndermanProvokedState(
            CallbackInfoReturnable<Boolean> cir
    ) {
        EndermanEntity self = (EndermanEntity) (Object) this;

        if (LandMountManager.isAdoptedMount(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setCarriedBlock", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$preventAdoptedEndermanBlockPickup(
            @Nullable BlockState blockState,
            CallbackInfo ci
    ) {
        EndermanEntity self = (EndermanEntity) (Object) this;

        // Null is allowed so adoption can clear a block already being carried.
        if (blockState != null && LandMountManager.isAdoptedMount(self)) {
            ci.cancel();
        }
    }
}
