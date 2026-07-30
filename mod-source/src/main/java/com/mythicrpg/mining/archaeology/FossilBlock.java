package com.mythicrpg.mining.archaeology;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public final class FossilBlock extends BlockWithEntity {

    public static final MapCodec<FossilBlock> CODEC = createCodec(FossilBlock::new);

    public FossilBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FossilBlockEntity(pos, state);
    }

    @Override
    protected float calcBlockBreakingDelta(
            BlockState state,
            PlayerEntity player,
            BlockView world,
            BlockPos pos
    ) {
        return 0.0F;
    }

    @Override
    protected void onStateReplaced(
            BlockState state,
            World world,
            BlockPos pos,
            BlockState newState,
            boolean moved
    ) {
        if (!world.isClient() && !state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);

            if (blockEntity instanceof FossilBlockEntity fossil) {
                fossil.recordRemovalFromSite();
            }
        }

        super.onStateReplaced(state, world, pos, newState, moved);
    }

}
