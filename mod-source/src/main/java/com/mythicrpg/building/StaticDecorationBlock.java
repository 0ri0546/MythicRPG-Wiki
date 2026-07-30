package com.mythicrpg.building;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Invisible, non-colliding anchor rendered by a BlockEntityRenderer. */
public final class StaticDecorationBlock extends BlockWithEntity {
    public static final MapCodec<StaticDecorationBlock> CODEC = createCodec(StaticDecorationBlock::new);
    private static final VoxelShape OUTLINE = createCuboidShape(5, 5, 5, 11, 11, 11);

    public StaticDecorationBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return net.minecraft.util.shape.VoxelShapes.empty();
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StaticDecorationBlockEntity(pos, state);
    }


    @Override
    protected ActionResult onUse(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player,
            BlockHitResult hit
    ) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)
                || !(world.getBlockEntity(pos) instanceof StaticDecorationBlockEntity decoration)) {
            return ActionResult.PASS;
        }
        Hand hand = player.getMainHandStack().isOf(this.asItem()) ? Hand.MAIN_HAND
                : player.getOffHandStack().isOf(this.asItem()) ? Hand.OFF_HAND
                : null;
        if (hand == null) {
            return ActionResult.PASS;
        }
        StaticDecorationUiManager.openBlock(serverPlayer, hand, pos, decoration);
        return ActionResult.CONSUME;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!(world instanceof ServerWorld serverWorld)
                || !(placer instanceof ServerPlayerEntity player)
                || !(world.getBlockEntity(pos) instanceof StaticDecorationBlockEntity decoration)) return;

        if (!StaticDecorationState.get(serverWorld.getServer()).add(serverWorld, pos, player.getUuid())) {
            world.removeBlock(pos, false);
            if (!player.isCreative()) {
                ItemStack refund = stack.copyWithCount(1);
                player.getInventory().insertStack(refund);
                if (!refund.isEmpty()) player.dropItem(refund, false);
            }
            player.sendMessage(Text.translatable("message.mythicrpg.static_decoration.limit")
                    .formatted(Formatting.RED), true);
            BuildingSoundFeedback.error(player);
            return;
        }
        decoration.configure(player.getUuid(), StaticDecorationItemData.read(stack));
    }

    @Override
    protected void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!world.isClient && !state.isOf(newState.getBlock()) && world instanceof ServerWorld serverWorld) {
            StaticDecorationState.get(serverWorld.getServer()).remove(serverWorld, pos);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    protected List<ItemStack> getDroppedStacks(
            BlockState state,
            LootContextParameterSet.Builder builder
    ) {
        net.minecraft.entity.Entity cause = builder.getOptional(LootContextParameters.THIS_ENTITY);
        BlockEntity blockEntity = builder.getOptional(LootContextParameters.BLOCK_ENTITY);
        if (!(cause instanceof PlayerEntity)
                && blockEntity instanceof StaticDecorationBlockEntity decoration) {
            ItemStack configured = new ItemStack(this);
            StaticDecorationItemData.write(configured, decoration.effect());
            return List.of(configured);
        }
        return super.getDroppedStacks(state, builder);
    }

    @Override
    public void afterBreak(World world, PlayerEntity player, BlockPos pos, BlockState state,
                           @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!world.isClient && !player.isCreative() && blockEntity instanceof StaticDecorationBlockEntity decoration) {
            ItemStack refund = new ItemStack(this);
            StaticDecorationItemData.write(refund, decoration.effect());
            player.getInventory().insertStack(refund);
            if (!refund.isEmpty()) player.dropItem(refund, false);
        }
        super.afterBreak(world, player, pos, state, blockEntity, tool);
    }

    @Override
    public ItemStack getPickStack(WorldView world, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (world.getBlockEntity(pos) instanceof StaticDecorationBlockEntity decoration) {
            StaticDecorationItemData.write(stack, decoration.effect());
        }
        return stack;
    }
}
