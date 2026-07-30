package com.mythicrpg.mixin;

import com.mythicrpg.fighting.barons.BaronDeathMessageRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageTracker.class)
public abstract class DamageTrackerMixin {

    @Shadow
    @Final
    private LivingEntity entity;

    @Inject(method = "getDeathMessage", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$useBaronDeathMessage(CallbackInfoReturnable<Text> cir) {
        Text deathMessage = BaronDeathMessageRegistry.createDeathMessage(this.entity);

        if (deathMessage != null) {
            cir.setReturnValue(deathMessage);
        }
    }
}
