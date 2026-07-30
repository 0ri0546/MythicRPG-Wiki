package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.passive.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Some vanilla entities override MobEntity.interactMob and can consume the
 * click before an AdoptionSaddleItem receives it. This shared hook routes
 * those exceptions back through MythicRPG's normal adoption/mount pipeline.
 */
@Mixin({
        SnowGolemEntity.class,
        HoglinEntity.class,
        VillagerEntity.class,
        WanderingTraderEntity.class
})
public abstract class SpecialLandMountInteractionMixin {
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$routeSpecialLandMountInteraction(
            PlayerEntity player,
            Hand hand,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        MobEntity self = (MobEntity) (Object) this;
        boolean merchant = self instanceof VillagerEntity
                || self instanceof WanderingTraderEntity;

        ActionResult result = LandMountManager.handleSpecialInteraction(
                self,
                player,
                hand,
                merchant
        );

        if (result != ActionResult.PASS) {
            cir.setReturnValue(result);
        }
    }
}
