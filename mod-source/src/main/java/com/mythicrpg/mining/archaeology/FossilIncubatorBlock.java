package com.mythicrpg.mining.archaeology;

import com.mojang.serialization.MapCodec;
import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModBlockEntities;
import com.mythicrpg.core.PlayerCooldownManager;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import java.util.function.BiConsumer;

public final class FossilIncubatorBlock extends BlockWithEntity {

    public static final MapCodec<FossilIncubatorBlock> CODEC = createCodec(FossilIncubatorBlock::new);

    public FossilIncubatorBlock(Settings settings) {
        super(settings);
    }

    /**
     * Cancels the actual server-side break as a final protection layer.
     * calcBlockBreakingDelta handles normal survival mining; this event also covers instant creative breaks.
     */
    public static void registerBreakProtection() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(blockEntity instanceof FossilIncubatorBlockEntity incubator)
                    || !incubator.isLockedForBreaking()) {
                return true;
            }

            if (player instanceof ServerPlayerEntity serverPlayer
                    && PlayerCooldownManager.tryUse(serverPlayer, "fossil_incubator_break_locked", 20)) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.fossil_incubator.break_locked")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return false;
        });
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
        return new FossilIncubatorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return world.isClient
                ? null
                : validateTicker(type, ModBlockEntities.FOSSIL_INCUBATOR, FossilIncubatorBlockEntity::serverTick);
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

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        if (!SkillTreeManager.hasBonus(serverPlayer, SkillType.MINING, BonusType.FOSSIL_INCUBATION)) {
            if (PlayerCooldownManager.tryUse(serverPlayer, "fossil_incubator_locked", 20)) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.fossil_incubator.locked")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return ActionResult.CONSUME;
        }

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FossilIncubatorBlockEntity incubator) {
            serverPlayer.openHandledScreen(incubator);
            return ActionResult.CONSUME;
        }

        return ActionResult.PASS;
    }

    @Override
    protected void onBlockBreakStart(
            BlockState state,
            World world,
            BlockPos pos,
            PlayerEntity player
    ) {
        if (!world.isClient
                && world.getBlockEntity(pos) instanceof FossilIncubatorBlockEntity incubator
                && incubator.isLockedForBreaking()
                && player instanceof ServerPlayerEntity serverPlayer
                && PlayerCooldownManager.tryUse(serverPlayer, "fossil_incubator_break_locked", 20)) {
            serverPlayer.sendMessage(
                    Text.translatable("message.mythicrpg.fossil_incubator.break_locked")
                            .formatted(Formatting.RED),
                    true
            );
        }
        super.onBlockBreakStart(state, world, pos, player);
    }

    @Override
    protected void onExploded(
            BlockState state,
            World world,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> stackMerger
    ) {
        if (world.getBlockEntity(pos) instanceof FossilIncubatorBlockEntity incubator
                && incubator.isLockedForBreaking()) {
            return;
        }
        super.onExploded(state, world, pos, explosion, stackMerger);
    }

    @Override
    protected float calcBlockBreakingDelta(
            BlockState state,
            PlayerEntity player,
            net.minecraft.world.BlockView world,
            BlockPos pos
    ) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof FossilIncubatorBlockEntity incubator && incubator.isLockedForBreaking()) {
            return 0.0F;
        }
        return super.calcBlockBreakingDelta(state, player, world, pos);
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
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FossilIncubatorBlockEntity incubator) {
                ItemScattererCompat.spawn(world, pos, incubator);
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
        return blockEntity instanceof FossilIncubatorBlockEntity incubator
                ? ScreenHandler.calculateComparatorOutput((net.minecraft.inventory.Inventory) incubator)
                : 0;
    }

    /** Isolated to keep the block class readable and make the inventory-drop call easy to audit. */
    private static final class ItemScattererCompat {
        private ItemScattererCompat() {
        }

        private static void spawn(World world, BlockPos pos, FossilIncubatorBlockEntity inventory) {
            net.minecraft.util.ItemScatterer.spawn(world, pos, inventory);
        }
    }
}
