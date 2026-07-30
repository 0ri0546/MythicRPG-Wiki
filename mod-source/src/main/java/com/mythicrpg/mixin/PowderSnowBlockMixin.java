package com.mythicrpg.mixin;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.traveling.TravelingBonusCache;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
    @Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void mythicrpg$allowTravelingPowderWalking(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof ServerPlayerEntity player
                && TravelingBonusCache.hasBonus(player, BonusType.TRAVEL_POWDER_WALKER)) {
            cir.setReturnValue(true);
        }
    }
}
