package com.mythicrpg.eating;

import com.mojang.serialization.MapCodec;
import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class FridgeBlock extends BlockWithEntity {
    public static final MapCodec<FridgeBlock> CODEC = createCodec(FridgeBlock::new);

    public FridgeBlock(Settings settings) {
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
        return new FridgeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return world.isClient
                ? null
                : validateTicker(type, ModBlockEntities.FRIDGE, FridgeBlockEntity::serverTick);
    }


    @Override
    public void onPlaced(
            World world,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!world.isClient()
                && placer instanceof ServerPlayerEntity player
                && world.getBlockEntity(pos) instanceof FridgeBlockEntity fridge) {
            fridge.claimOwner(player);
        }
    }

    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity serverPlayer
                && world.getBlockEntity(pos) instanceof FridgeBlockEntity fridge) {
            serverPlayer.openHandledScreen(fridge);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
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
            if (world.getBlockEntity(pos) instanceof FridgeBlockEntity fridge) {
                fridge.releasePreservation();
                ItemScatterer.spawn(world, pos, fridge);
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity instanceof FridgeBlockEntity fridge
                ? ScreenHandler.calculateComparatorOutput((Inventory) fridge)
                : 0;
    }
}
