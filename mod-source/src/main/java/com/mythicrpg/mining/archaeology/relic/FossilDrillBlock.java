package com.mythicrpg.mining.archaeology.relic;

import com.mojang.serialization.MapCodec;
import com.mythicrpg.core.ModBlockEntities;
import com.mythicrpg.mining.archaeology.polish.ArchaeologyPolishEffects;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class FossilDrillBlock extends BlockWithEntity {

    public static final MapCodec<FossilDrillBlock> CODEC = createCodec(FossilDrillBlock::new);

    public FossilDrillBlock(Settings settings) {
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

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FossilDrillBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return world.isClient
                ? null
                : validateTicker(type, ModBlockEntities.FOSSIL_DRILL, FossilDrillBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)
                || !(world.getBlockEntity(pos) instanceof FossilDrillBlockEntity blockEntity)
                || !blockEntity.isProcessing()) {
            return ActionResult.PASS;
        }

        serverPlayer.sendMessage(
                Text.translatable(
                        "message.mythicrpg.fossil_drill.status",
                        blockEntity.progressPercent(serverWorld.getTime()),
                        ArchaeologyPolishEffects.formatTicks(blockEntity.remainingTicks(serverWorld.getTime())),
                        blockEntity.oreCount(),
                        blockEntity.multiplier()
                ).formatted(Formatting.AQUA),
                true
        );
        return ActionResult.CONSUME;
    }

    @Override
    protected void onStateReplaced(
            BlockState state,
            World world,
            BlockPos pos,
            BlockState newState,
            boolean moved
    ) {
        if (!world.isClient
                && !state.isOf(newState.getBlock())
                && world instanceof ServerWorld serverWorld
                && world.getBlockEntity(pos) instanceof FossilDrillBlockEntity blockEntity) {
            FossilDrillManager.remove(serverWorld, blockEntity.owner(), pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
