package com.mythicrpg.mixin.woodcutting;

import com.mythicrpg.woodcutting.chest.ChestModuleManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(method = "getInventoryAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/inventory/Inventory;", at = @At("RETURN"), cancellable = true)
    private static void mythicrpg$wrapPublicChestInventory(
            World world,
            BlockPos pos,
            CallbackInfoReturnable<Inventory> cir
    ) {
        cir.setReturnValue(ChestModuleManager.wrapIfActive(cir.getReturnValue()));
    }

    @Inject(method = "getBlockInventoryAt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/inventory/Inventory;", at = @At("RETURN"), cancellable = true)
    private static void mythicrpg$wrapInternalChestInventory(
            World world,
            BlockPos pos,
            BlockState state,
            CallbackInfoReturnable<Inventory> cir
    ) {
        cir.setReturnValue(ChestModuleManager.wrapIfActive(cir.getReturnValue()));
    }
}
