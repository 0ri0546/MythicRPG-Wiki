package com.mythicrpg.mixin;

import com.mythicrpg.mining.archaeology.FossilCleaningManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BrushItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrushItem.class)
public abstract class BrushItemFossilMixin {

    @Inject(method = "getMaxUseTime", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$allowLongFossilCleaning(
            ItemStack stack,
            LivingEntity user,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (FossilCleaningManager.isLookingAtFossil(user)) {
            cir.setReturnValue(FossilCleaningManager.MAX_BRUSH_USE_TICKS);
        }
    }

    @Inject(method = "usageTick", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$cleanFossil(
            World world,
            LivingEntity user,
            ItemStack stack,
            int remainingUseTicks,
            CallbackInfo ci
    ) {
        if (FossilCleaningManager.handleUsageTick(world, user, stack, remainingUseTicks)) {
            ci.cancel();
        }
    }
}
