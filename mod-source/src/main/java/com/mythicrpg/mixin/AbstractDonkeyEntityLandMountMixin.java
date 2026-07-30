package com.mythicrpg.mixin;

import com.mythicrpg.traveling.AdoptionSaddleItem;
import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Llamas inherit their right-click interaction from AbstractDonkeyEntity,
 * which overrides AbstractHorseEntity.interactMob. The adoption hook must
 * therefore be placed here to run before the vanilla donkey/llama logic.
 */
@Mixin(AbstractDonkeyEntity.class)
public abstract class AbstractDonkeyEntityLandMountMixin {
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$handleLlamaMountInteraction(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        AbstractDonkeyEntity self = (AbstractDonkeyEntity) (Object) this;

        if (!(self instanceof LlamaEntity llama)) {
            return;
        }

        ItemStack heldStack = player.getStackInHand(hand);

        if (heldStack.getItem() instanceof AdoptionSaddleItem saddleItem) {
            ActionResult adoptionResult = saddleItem.useOnEntity(
                    heldStack,
                    player,
                    llama,
                    hand
            );

            if (adoptionResult != ActionResult.PASS) {
                cir.setReturnValue(adoptionResult);
            }
            return;
        }

        ActionResult mountResult = LandMountManager.handleAdoptedInteraction(
                llama,
                player,
                heldStack
        );

        if (mountResult != ActionResult.PASS) {
            cir.setReturnValue(mountResult);
        }
    }
}
