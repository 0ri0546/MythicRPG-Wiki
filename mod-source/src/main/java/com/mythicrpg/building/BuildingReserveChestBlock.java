package com.mythicrpg.building;

import com.mojang.serialization.MapCodec;
import com.mythicrpg.core.PlayerCooldownManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.function.BiConsumer;

/** Owner-bound construction storage. It has no ticker and uses the vanilla 9x3 container UI. */
public final class BuildingReserveChestBlock extends BlockWithEntity {
    public static final MapCodec<BuildingReserveChestBlock> CODEC = createCodec(BuildingReserveChestBlock::new);

    public BuildingReserveChestBlock(Settings settings) {
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
        return new BuildingReserveChestBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (!(world instanceof ServerWorld serverWorld)
                || !(placer instanceof ServerPlayerEntity player)
                || !(world.getBlockEntity(pos) instanceof BuildingReserveChestBlockEntity chest)) {
            return;
        }

        chest.setOwner(player.getUuid());
        if (!BuildingReserveChestManager.registerPlacedChest(serverWorld, pos, player.getUuid())) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building.reserve.limit")
                            .formatted(Formatting.RED),
                    true
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
                || !(world instanceof ServerWorld serverWorld)
                || !(world.getBlockEntity(pos) instanceof BuildingReserveChestBlockEntity chest)) {
            return ActionResult.PASS;
        }

        if (!chest.hasOwner()) {
            chest.setOwner(serverPlayer.getUuid());
            if (!BuildingReserveChestManager.registerPlacedChest(serverWorld, pos, serverPlayer.getUuid())) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.building.reserve.limit")
                                .formatted(Formatting.RED),
                        true
                );
                return ActionResult.CONSUME;
            }
        }

        if (!chest.isOwner(serverPlayer)) {
            if (PlayerCooldownManager.tryUse(serverPlayer, "building_reserve_access_denied", 20)) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.building.reserve.not_owner")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return ActionResult.CONSUME;
        }

        BuildingReserveChestManager.ensureIndexed(serverWorld, pos, serverPlayer.getUuid());
        serverPlayer.openHandledScreen(chest);
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
        if (!world.isClient && !state.isOf(newState.getBlock())) {
            if (world instanceof ServerWorld serverWorld) {
                BuildingReserveChestManager.removeChest(serverWorld, pos);
            }
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof BuildingReserveChestBlockEntity chest) {
                ItemScatterer.spawn(world, pos, chest);
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
        return blockEntity instanceof BuildingReserveChestBlockEntity chest
                ? ScreenHandler.calculateComparatorOutput((Inventory) chest)
                : 0;
    }

    @Override
    protected void onExploded(
            BlockState state,
            World world,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> stackMerger
    ) {
        // Owner-bound reserves are protected from explosions to avoid indirect inventory theft.
    }
}
