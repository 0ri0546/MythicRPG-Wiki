
package com.mythicrpg.fishing;

import com.mojang.serialization.MapCodec;
import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Waterloggable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.util.shape.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class FishNetBlock extends BlockWithEntity implements Waterloggable {
    public static final MapCodec<FishNetBlock> CODEC = createCodec(FishNetBlock::new);
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 2.0, 15.0);

    public FishNetBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(WATERLOGGED, false));
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
    protected VoxelShape getOutlineShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return SHAPE;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FishNetBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return world.isClient
                ? null
                : validateTicker(type, ModBlockEntities.FISH_NET, FishNetBlockEntity::tick);
    }

    @Override
    @Nullable
    public BlockState getPlacementState(ItemPlacementContext context) {
        if (!context.getWorld().getRegistryKey().equals(World.OVERWORLD)) {
            return null;
        }
        FluidState fluid = context.getWorld().getFluidState(context.getBlockPos());
        if (fluid.getFluid() != Fluids.WATER) {
            return null;
        }
        return getDefaultState().with(WATERLOGGED, true);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED)
                ? Fluids.WATER.getStill(false)
                : super.getFluidState(state);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(
                state,
                direction,
                neighborState,
                world,
                pos,
                neighborPos
        );
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
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
        if (!world.isClient
                && placer instanceof ServerPlayerEntity player
                && world.getBlockEntity(pos) instanceof FishNetBlockEntity net
                && !net.claim(player)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.fishing.net_limit")
                            .formatted(Formatting.RED),
                    false
            );
            world.breakBlock(pos, true, player);
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
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !(world.getBlockEntity(pos) instanceof FishNetBlockEntity net)) {
            return ActionResult.PASS;
        }
        if (!net.isOwner(serverPlayer)) {
            serverPlayer.sendMessage(
                    Text.translatable("message.mythicrpg.fishing.net_not_owner")
                            .formatted(Formatting.RED),
                    true
            );
            return ActionResult.CONSUME;
        }
        serverPlayer.openHandledScreen(net);
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
                && world.getBlockEntity(pos) instanceof FishNetBlockEntity net) {
            if (world instanceof ServerWorld serverWorld) {
                FishingNetManager.release(net.owner(), serverWorld, pos);
            }
            ItemScatterer.spawn(world, pos, net);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
