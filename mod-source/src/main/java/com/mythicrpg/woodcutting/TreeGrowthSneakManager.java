package com.mythicrpg.woodcutting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.WorldScanUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TreeGrowthSneakManager {

    private static final int CHECK_INTERVAL_TICKS = 1;
    private static final int COOLDOWN_TICKS = 100;
    private static final int RADIUS = 6;
    private static final int MAX_SAPLINGS_PER_TRIGGER = 6;

    private static final Map<UUID, Boolean> WAS_SNEAKING = new HashMap<>();
    private static final Map<UUID, Long> LAST_TRIGGER_TICK = new HashMap<>();

    private static int tickCounter = 0;

    private TreeGrowthSneakManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;

            if (tickCounter % CHECK_INTERVAL_TICKS != 0) {
                return;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tickPlayer(player);
            }
        });
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        boolean isSneaking = player.isSneaking();
        boolean wasSneaking = WAS_SNEAKING.getOrDefault(player.getUuid(), false);

        WAS_SNEAKING.put(player.getUuid(), isSneaking);

        if (!wasSneaking || isSneaking) {
            return;
        }

        // Ici : le joueur vient de relâcher Shift.
        tryTriggerTreeGrowth(player);
    }

    private static void tryTriggerTreeGrowth(ServerPlayerEntity player) {
        if (!SkillTreeManager.hasBonus(player, SkillType.WOODCUTTING, BonusType.TREE_GROWTH)) {
            return;
        }

        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }

        long now = world.getTime();
        long lastTrigger = LAST_TRIGGER_TICK.getOrDefault(player.getUuid(), -999999L);

        if (now - lastTrigger < COOLDOWN_TICKS) {
            return;
        }

        int grownCount = growNearbySaplings(player, world);

        if (grownCount <= 0) {
            return;
        }

        LAST_TRIGGER_TICK.put(player.getUuid(), now);

        world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                player.getX(),
                player.getBodyY(0.5),
                player.getZ(),
                20,
                0.8,
                0.5,
                0.8,
                0.04
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ITEM_BONE_MEAL_USE,
                SoundCategory.PLAYERS,
                0.8f,
                1.2f
        );
    }

    private static int growNearbySaplings(ServerPlayerEntity player, ServerWorld world) {
        BlockPos center = player.getBlockPos();

        return WorldScanUtils.forEachBlockInBox(
                world,
                center,
                RADIUS,
                -2,
                2,
                MAX_SAPLINGS_PER_TRIGGER,
                (pos, state) -> {
                    if (!state.isIn(BlockTags.SAPLINGS)) {
                        return false;
                    }

                    if (!(state.getBlock() instanceof Fertilizable fertilizable)) {
                        return false;
                    }

                    if (!fertilizable.isFertilizable(world, pos, state)) {
                        return false;
                    }

                    if (!fertilizable.canGrow(world, world.random, pos, state)) {
                        return false;
                    }

                    fertilizable.grow(world, world.random, pos, state);

                    world.spawnParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5,
                            pos.getY() + 0.7,
                            pos.getZ() + 0.5,
                            10,
                            0.25,
                            0.25,
                            0.25,
                            0.03
                    );

                    return true;
                }
        );
    }

    public static void clearPlayer(UUID playerUuid) {
        WAS_SNEAKING.remove(playerUuid);
        LAST_TRIGGER_TICK.remove(playerUuid);
    }
}