package com.mythicrpg.mixin;

import com.mythicrpg.building.BuildingScaffoldingManager;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Catches scaffolding removal through players, explosions, pistons and commands. */
@Mixin(AbstractBlock.class)
public abstract class AbstractBlockBuildingScaffoldingMixin {
    @Inject(method = "onStateReplaced", at = @At("HEAD"))
    private void mythicrpg$removeExtendedScaffoldingIndex(
            BlockState state,
            World world,
            BlockPos pos,
            BlockState newState,
            boolean moved,
            CallbackInfo ci
    ) {
        if (world instanceof ServerWorld serverWorld
                && state.isOf(Blocks.SCAFFOLDING)
                && !newState.isOf(Blocks.SCAFFOLDING)) {
            BuildingScaffoldingManager.onScaffoldingRemoved(serverWorld, pos);
        }
    }
}
