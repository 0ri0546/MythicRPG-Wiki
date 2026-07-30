package com.mythicrpg.mixin;

import com.mythicrpg.building.BuildingScaffoldingManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScaffoldingBlock.class)
public abstract class ScaffoldingBlockBuildingMixin {
    @Inject(method = "calculateDistance", at = @At("HEAD"), cancellable = true)
    private static void mythicrpg$extendSupportedDistance(
            BlockView world,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        int override = BuildingScaffoldingManager.getDistanceOverride(world, pos);
        if (override >= 0) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "getStateForNeighborUpdate", at = @At("HEAD"))
    private void mythicrpg$queueExtendedDistanceUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos,
            CallbackInfoReturnable<BlockState> cir
    ) {
        if (world instanceof ServerWorld serverWorld) {
            BuildingScaffoldingManager.onNeighborChanged(serverWorld, pos);
        }
    }
}
