package com.mythicrpg.mining.archaeology.relic;

import com.mojang.serialization.MapCodec;
import com.mythicrpg.core.ModBlockEntities;
import com.mythicrpg.core.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public final class GrowthTotemBlock extends BlockWithEntity {

    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final MapCodec<GrowthTotemBlock> CODEC = createCodec(GrowthTotemBlock::new);

    public GrowthTotemBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(HALF, DoubleBlockHalf.LOWER));
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return context.getWorld().getBlockState(context.getBlockPos().up()).canReplace(context)
                ? getDefaultState()
                : null;
    }

    @Override
    public void onPlaced(
            World world,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack
    ) {
        if (!world.isClient) {
            world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
            if (world.getBlockEntity(pos) instanceof GrowthTotemBlockEntity blockEntity) {
                blockEntity.setLevel(RelicItemData.getLevel(stack).value());
            }
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            WorldAccess world,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            return neighborState.isOf(this) && neighborState.get(HALF) != half
                    ? state
                    : Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
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

        BlockPos lowerPos = state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
        if (!(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)
                || !(world.getBlockEntity(lowerPos) instanceof GrowthTotemBlockEntity blockEntity)) {
            return ActionResult.PASS;
        }

        int crops = GrowthTotemBlockEntity.countCompatibleCrops(
                serverWorld,
                lowerPos,
                blockEntity.radius()
        );
        serverPlayer.sendMessage(
                Text.translatable(
                        "message.mythicrpg.growth_totem.status",
                        blockEntity.level(),
                        blockEntity.radius(),
                        crops
                ).formatted(crops > 0 ? Formatting.GREEN : Formatting.YELLOW),
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
        if (!state.isOf(newState.getBlock())) {
            BlockPos other = state.get(HALF) == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
            if (world.getBlockState(other).isOf(this)) {
                world.setBlockState(
                        other,
                        Blocks.AIR.getDefaultState(),
                        Block.NOTIFY_ALL | Block.SKIP_DROPS
                );
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockPos lowerPos = state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
        if (!world.isClient && !player.isCreative()) {
            ItemStack drop = new ItemStack(ModItems.GROWTH_TOTEM);
            if (world.getBlockEntity(lowerPos) instanceof GrowthTotemBlockEntity blockEntity) {
                RelicItemData.setLevel(drop, RelicLevel.fromValue(blockEntity.level()));
            }
            net.minecraft.util.ItemScatterer.spawn(
                    world,
                    lowerPos.getX() + 0.5,
                    lowerPos.getY() + 0.5,
                    lowerPos.getZ() + 0.5,
                    drop
            );
        }
        return super.onBreak(world, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER
                ? new GrowthTotemBlockEntity(pos, state)
                : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return world.isClient || state.get(HALF) != DoubleBlockHalf.LOWER
                ? null
                : validateTicker(type, ModBlockEntities.GROWTH_TOTEM, GrowthTotemBlockEntity::tick);
    }
}
