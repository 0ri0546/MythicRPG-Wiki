package com.mythicrpg.mixin.client;

import com.mythicrpg.client.ClientSkillTreeState;
import com.mythicrpg.core.SkillType;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockClientMixin {
    @Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void mythicrpg$allowClientTravelingPowderWalking(
            Entity entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (entity instanceof ClientPlayerEntity
                && ClientSkillTreeState.isUnlocked(SkillType.TRAVELING, 4)) {
            cir.setReturnValue(true);
        }
    }
}
