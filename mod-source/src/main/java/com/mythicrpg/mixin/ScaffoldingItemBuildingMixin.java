package com.mythicrpg.mixin;

import com.mythicrpg.building.BuildingScaffoldingManager;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ScaffoldingItem;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScaffoldingItem.class)
public abstract class ScaffoldingItemBuildingMixin {
    @Inject(method = "getPlacementContext", at = @At("RETURN"))
    private void mythicrpg$armExtendedScaffolding(
            ItemPlacementContext original,
            CallbackInfoReturnable<ItemPlacementContext> cir
    ) {
        ItemPlacementContext resolved = cir.getReturnValue();
        if (resolved != null && resolved.getPlayer() instanceof ServerPlayerEntity player) {
            BuildingScaffoldingManager.armPlacement(player, resolved);
        }
    }
}
