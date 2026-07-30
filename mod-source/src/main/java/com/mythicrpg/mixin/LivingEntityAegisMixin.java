package com.mythicrpg.mixin;

import com.mythicrpg.mining.archaeology.relic.ColossalAegisManager;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public class LivingEntityAegisMixin {
    @ModifyVariable(method = "takeKnockback", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double mythicrpg$reduceAegisKnockback(double strength) {
        return strength * ColossalAegisManager.knockbackMultiplier((LivingEntity)(Object)this);
    }
}
