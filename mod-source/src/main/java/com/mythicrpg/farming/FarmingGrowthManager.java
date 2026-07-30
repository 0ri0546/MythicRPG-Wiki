package com.mythicrpg.farming;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class FarmingGrowthManager {
    private static final int RADIUS = 5;
    private static final int EXTRA_GROWTH_ATTEMPTS_PER_SECOND = 200;
    private static final int EXTRA_GROWTH_ATTEMPTS_PER_TICK =
            EXTRA_GROWTH_ATTEMPTS_PER_SECOND / 20;

    private FarmingGrowthManager() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            tickLivingField(serverWorld);
        });
    }

    private static void tickLivingField(ServerWorld world) {
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }

            if (!SkillTreeManager.hasBonus(player, SkillType.FARMING, BonusType.LIVING_FIELD)) {
                continue;
            }

            applyLivingFieldAround(player, world);
        }
    }

    private static void applyLivingFieldAround(ServerPlayerEntity player, ServerWorld world) {
        BlockPos center = player.getBlockPos();

        // Keep the same 200 attempts/player/second while spreading the work
        // over all 20 ticks instead of creating a one-tick spike every second.
        for (int i = 0; i < EXTRA_GROWTH_ATTEMPTS_PER_TICK; i++) {
            BlockPos pos = center.add(
                    world.random.nextBetween(-RADIUS, RADIUS),
                    world.random.nextBetween(-2, 2),
                    world.random.nextBetween(-RADIUS, RADIUS)
            );

            BlockState state = world.getBlockState(pos);

            if (!canLivingFieldAffect(state)) {
                continue;
            }

            state.randomTick(world, pos, world.random);

            if (world.random.nextFloat() < 0.12f) {
                world.spawnParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        pos.getX() + 0.5,
                        pos.getY() + 0.8,
                        pos.getZ() + 0.5,
                        1,
                        0.15,
                        0.15,
                        0.15,
                        0.01
                );
            }
        }
    }

    private static boolean canLivingFieldAffect(BlockState state) {
        Block block = state.getBlock();

        // Crops classiques : wheat, carrots, potatoes, beetroot, etc.
        if (block instanceof CropBlock) {
            return true;
        }

        return state.isOf(Blocks.NETHER_WART)
                || state.isOf(Blocks.COCOA)
                || state.isOf(Blocks.SWEET_BERRY_BUSH)
                || state.isOf(Blocks.CAVE_VINES)
                || state.isOf(Blocks.CAVE_VINES_PLANT)
                || state.isOf(Blocks.MELON_STEM)
                || state.isOf(Blocks.PUMPKIN_STEM)
                || state.isOf(Blocks.CACTUS)
                || state.isOf(Blocks.SUGAR_CANE)
                || state.isOf(Blocks.BAMBOO)
                || state.isOf(Blocks.KELP)
                || state.isOf(Blocks.KELP_PLANT)
                || state.isOf(Blocks.TWISTING_VINES)
                || state.isOf(Blocks.TWISTING_VINES_PLANT)
                || state.isOf(Blocks.WEEPING_VINES)
                || state.isOf(Blocks.WEEPING_VINES_PLANT);
    }
}