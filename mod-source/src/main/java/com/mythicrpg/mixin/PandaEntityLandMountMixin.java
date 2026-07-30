package com.mythicrpg.mixin;

import com.mythicrpg.traveling.AdoptionSaddleItem;
import com.mythicrpg.traveling.LandMountDataAccess;
import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PandaEntity overrides interactMob and therefore bypasses the generic
 * MobEntity interaction hook used by the existing adopted mounts.
 */
@Mixin(PandaEntity.class)
public abstract class PandaEntityLandMountMixin {
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$handlePandaMountInteraction(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        PandaEntity self = (PandaEntity) (Object) this;
        ItemStack heldStack = player.getStackInHand(hand);

        if (heldStack.getItem() instanceof AdoptionSaddleItem saddleItem) {
            ActionResult adoptionResult = saddleItem.useOnEntity(
                    heldStack,
                    player,
                    self,
                    hand
            );

            if (adoptionResult != ActionResult.PASS) {
                cir.setReturnValue(adoptionResult);
            }
            return;
        }

        if (self instanceof LandMountDataAccess access
                && access.mythicrpg$isAdoptedLandMount()) {
            mythicrpg$resetRideBlockingPose(self);
        }

        ActionResult mountResult = LandMountManager.handleAdoptedInteraction(
                self,
                player,
                heldStack
        );

        if (mountResult != ActionResult.PASS) {
            cir.setReturnValue(mountResult);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void mythicrpg$keepAdoptedPandaRideable(CallbackInfo ci) {
        PandaEntity self = (PandaEntity) (Object) this;

        if (self instanceof LandMountDataAccess access
                && access.mythicrpg$isAdoptedLandMount()
                && self.hasPassengers()) {
            mythicrpg$resetRideBlockingPose(self);
        }
    }

    private static void mythicrpg$resetRideBlockingPose(PandaEntity panda) {
        panda.setSitting(false);
        panda.setLyingOnBack(false);
        panda.setEating(false);
        panda.setPlaying(false);
    }
}
