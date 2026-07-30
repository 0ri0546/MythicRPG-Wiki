package com.mythicrpg.eating;

import com.mojang.serialization.MapCodec;
import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModBlockEntities;
import com.mythicrpg.core.PlayerCooldownManager;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import java.util.function.BiConsumer;

public final class CookingPotBlock extends BlockWithEntity {
    public static final MapCodec<CookingPotBlock> CODEC = createCodec(CookingPotBlock::new);

    public CookingPotBlock(Settings settings) {
        super(settings);
    }

    public static void registerBreakProtection() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(blockEntity instanceof CookingPotBlockEntity cookingPot)
                    || !cookingPot.isLockedForBreaking()) {
                return true;
            }
            if (player instanceof ServerPlayerEntity serverPlayer
                    && PlayerCooldownManager.tryUse(serverPlayer, "eating_cooking_pot_break_locked", 20)) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.eating.cooking_pot_break_locked")
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
        return new CookingPotBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return world.isClient
                ? null
                : validateTicker(type, ModBlockEntities.COOKING_POT, CookingPotBlockEntity::serverTick);
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
        if (!SkillTreeManager.hasBonus(serverPlayer, SkillType.EATING, BonusType.EATING_COOKING)) {
            if (PlayerCooldownManager.tryUse(serverPlayer, "eating_cooking_pot_locked", 20)) {
                serverPlayer.sendMessage(
                        Text.translatable("message.mythicrpg.eating.cooking_locked")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return ActionResult.CONSUME;
        }
        if (world.getBlockEntity(pos) instanceof CookingPotBlockEntity cookingPot) {
            serverPlayer.openHandledScreen(cookingPot);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;
    }


    @Override
    protected void onBlockBreakStart(BlockState state, World world, BlockPos pos, PlayerEntity player) {
        if (!world.isClient
                && world.getBlockEntity(pos) instanceof CookingPotBlockEntity cookingPot
                && cookingPot.isLockedForBreaking()
                && player instanceof ServerPlayerEntity serverPlayer
                && PlayerCooldownManager.tryUse(serverPlayer, "eating_cooking_pot_break_locked", 20)) {
            serverPlayer.sendMessage(
                    Text.translatable("message.mythicrpg.eating.cooking_pot_break_locked")
                            .formatted(Formatting.RED),
                    true
            );
        }
        super.onBlockBreakStart(state, world, pos, player);
    }

    @Override
    protected float calcBlockBreakingDelta(
            BlockState state,
            PlayerEntity player,
            net.minecraft.world.BlockView world,
            BlockPos pos
    ) {
        if (world.getBlockEntity(pos) instanceof CookingPotBlockEntity cookingPot
                && cookingPot.isLockedForBreaking()) {
            return 0.0F;
        }
        return super.calcBlockBreakingDelta(state, player, world, pos);
    }

    @Override
    protected void onExploded(
            BlockState state,
            World world,
            BlockPos pos,
            Explosion explosion,
            BiConsumer<ItemStack, BlockPos> stackMerger
    ) {
        if (world.getBlockEntity(pos) instanceof CookingPotBlockEntity cookingPot
                && cookingPot.isLockedForBreaking()) {
            return;
        }
        super.onExploded(state, world, pos, explosion, stackMerger);
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
                && world.getBlockEntity(pos) instanceof CookingPotBlockEntity cookingPot) {
            ItemScatterer.spawn(world, pos, cookingPot);
            world.updateComparators(pos, this);
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}
