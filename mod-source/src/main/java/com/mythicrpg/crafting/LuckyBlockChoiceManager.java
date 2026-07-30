package com.mythicrpg.crafting;

import com.mythicrpg.core.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import java.util.Iterator;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks the two Lucky Blocks created by the "Pile ou face" event.
 *
 * <p>When either block is broken, the other one is removed directly, without
 * invoking its Lucky Block event. A sign-based fallback keeps the pair working
 * after a server restart, even though the in-memory association is lost.</p>
 */
public final class LuckyBlockChoiceManager {

    private static final Map<RegistryKey<World>, Map<BlockPos, BlockPos>> PAIRS = new HashMap<>();
    private static final Direction[] HORIZONTAL_DIRECTIONS = {
            Direction.NORTH,
            Direction.SOUTH,
            Direction.WEST,
            Direction.EAST
    };

    private LuckyBlockChoiceManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getOverworld().getTime() % 1200L == 0L) prune(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clearAll());
    }

    public static void clearAll() {
        PAIRS.clear();
    }

    private static void prune(MinecraftServer server) {
        Iterator<Map.Entry<RegistryKey<World>, Map<BlockPos, BlockPos>>> worlds = PAIRS.entrySet().iterator();
        while (worlds.hasNext()) {
            Map.Entry<RegistryKey<World>, Map<BlockPos, BlockPos>> entry = worlds.next();
            ServerWorld world = server.getWorld(entry.getKey());
            if (world == null) {
                worlds.remove();
                continue;
            }
            Map<BlockPos, BlockPos> pairs = entry.getValue();
            pairs.entrySet().removeIf(pair -> {
                BlockPos first = pair.getKey();
                BlockPos second = pair.getValue();
                if (!world.isChunkLoaded(first) || !world.isChunkLoaded(second)) return false;
                return !world.getBlockState(first).isOf(ModBlocks.LUCKY_BLOCK)
                        || !world.getBlockState(second).isOf(ModBlocks.LUCKY_BLOCK);
            });
            if (pairs.isEmpty()) worlds.remove();
        }
    }

    public static void registerPair(ServerWorld world, BlockPos first, BlockPos second) {
        Map<BlockPos, BlockPos> worldPairs = PAIRS.computeIfAbsent(
                world.getRegistryKey(),
                ignored -> new HashMap<>()
        );

        unlink(worldPairs, first);
        unlink(worldPairs, second);

        BlockPos immutableFirst = first.toImmutable();
        BlockPos immutableSecond = second.toImmutable();

        worldPairs.put(immutableFirst, immutableSecond);
        worldPairs.put(immutableSecond, immutableFirst);
    }

    public static void consumeChoice(ServerWorld world, BlockPos chosenPos) {
        Map<BlockPos, BlockPos> worldPairs = PAIRS.get(world.getRegistryKey());
        BlockPos otherPos = null;

        if (worldPairs != null) {
            otherPos = worldPairs.remove(chosenPos);

            if (otherPos != null) {
                worldPairs.remove(otherPos);
            }

            if (worldPairs.isEmpty()) {
                PAIRS.remove(world.getRegistryKey());
            }
        }

        if (otherPos == null) {
            otherPos = findPersistedPair(world, chosenPos);
        }

        if (otherPos == null) {
            return;
        }

        BlockState otherState = world.getBlockState(otherPos);

        if (!otherState.isOf(ModBlocks.LUCKY_BLOCK)) {
            return;
        }

        world.setBlockState(
                otherPos,
                Blocks.AIR.getDefaultState(),
                Block.NOTIFY_ALL
        );

        world.syncWorldEvent(
                null,
                2001,
                otherPos,
                Block.getRawIdFromState(otherState)
        );
    }

    private static BlockPos findPersistedPair(ServerWorld world, BlockPos chosenPos) {
        BlockState chosenState = world.getBlockState(chosenPos);

        if (!chosenState.isOf(ModBlocks.LUCKY_BLOCK)) {
            return null;
        }

        int chosenLuck = LuckyBlock.decodeLuck(chosenState.get(LuckyBlock.LUCK));

        if (Math.abs(chosenLuck) != LuckyBlockLuckManager.MAX_LUCK) {
            return null;
        }

        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            BlockPos signPos = chosenPos.offset(direction);
            BlockEntity blockEntity = world.getBlockEntity(signPos);

            if (!(blockEntity instanceof SignBlockEntity sign) || !isChoiceSign(sign)) {
                continue;
            }

            BlockPos candidatePos = signPos.offset(direction);
            BlockState candidateState = world.getBlockState(candidatePos);

            if (!candidateState.isOf(ModBlocks.LUCKY_BLOCK)) {
                continue;
            }

            int candidateLuck = LuckyBlock.decodeLuck(candidateState.get(LuckyBlock.LUCK));

            if (candidateLuck == -chosenLuck) {
                return candidatePos.toImmutable();
            }
        }

        return null;
    }

    private static boolean isChoiceSign(SignBlockEntity sign) {
        String front = sign.getFrontText().getMessage(1, false).getString();
        String back = sign.getBackText().getMessage(1, false).getString();

        return "Pile ou face".equalsIgnoreCase(front)
                || "Pile ou face".equalsIgnoreCase(back);
    }

    private static void unlink(Map<BlockPos, BlockPos> worldPairs, BlockPos pos) {
        BlockPos oldOther = worldPairs.remove(pos);

        if (oldOther != null) {
            worldPairs.remove(oldOther);
        }
    }
}
