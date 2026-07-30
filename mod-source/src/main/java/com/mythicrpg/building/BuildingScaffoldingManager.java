package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Extends horizontal scaffolding support to 32 blocks while keeping Minecraft's
 * vanilla DISTANCE property unchanged.
 *
 * <p>The real 7..32 distance is persisted externally. calculateDistance only
 * performs an O(1) lookup for established extended scaffolding. Changes are
 * propagated locally and incrementally with a strict per-tick budget.</p>
 */
public final class BuildingScaffoldingManager {
    public static final int MAX_HORIZONTAL_DISTANCE = 32;

    private static final int VANILLA_STABLE_DISTANCE = 6;
    private static final int VANILLA_UNSUPPORTED_DISTANCE = 7;
    private static final int INTERNAL_UNSUPPORTED_DISTANCE = MAX_HORIZONTAL_DISTANCE + 1;
    private static final int UNKNOWN_DISTANCE = -1;

    private static final int ARMED_PLACEMENT_LIFETIME_TICKS = 3;
    private static final int MAX_DISTANCE_UPDATES_PER_TICK = 4_096;
    // A world cannot contain more indexed entries than the per-dimension cap,
    // so this queue can always hold a complete repair pass without dropping work.
    private static final int MAX_QUEUED_POSITIONS_PER_WORLD =
            BuildingScaffoldingState.MAX_ENTRIES_PER_DIMENSION;

    private static final Map<PlacementKey, ArmedPlacement> ARMED_PLACEMENTS = new HashMap<>();
    private static final Map<MinecraftServer, Map<RegistryKey<World>, DirtyQueue>> DIRTY_QUEUES =
            new IdentityHashMap<>();
    private static final Set<MinecraftServer> INDEX_LIMIT_WARNING_EMITTED =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<MinecraftServer> QUEUE_LIMIT_WARNING_EMITTED =
            java.util.Collections.newSetFromMap(new IdentityHashMap<>());

    private BuildingScaffoldingManager() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(BuildingScaffoldingManager::onChunkLoaded);
        ServerLifecycleEvents.SERVER_STARTED.register(BuildingScaffoldingManager::reconcileDimensions);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            expireArmedPlacements(server);
            processDirtyQueues(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ARMED_PLACEMENTS.keySet().removeIf(key -> key.server() == server);
            DIRTY_QUEUES.remove(server);
            INDEX_LIMIT_WARNING_EMITTED.remove(server);
            QUEUE_LIMIT_WARNING_EMITTED.remove(server);
        });
    }

    /** Called from ScaffoldingItem after vanilla has resolved its exact placement position. */
    public static void armPlacement(ServerPlayerEntity player, ItemPlacementContext context) {
        if (!(context.getWorld() instanceof ServerWorld world)
                || !SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_SCAFFOLDING_RANGE
        )) {
            return;
        }

        BlockPos pos = context.getBlockPos().toImmutable();
        BuildingScaffoldingState state = BuildingScaffoldingState.get(world.getServer());
        boolean alreadyEligible = state.isEligible(world, pos);
        if (!state.addEligible(world, pos)) {
            warnIndexLimit(world.getServer());
            return;
        }

        PlacementKey key = key(world, pos);
        ARMED_PLACEMENTS.put(
                key,
                new ArmedPlacement(
                        world.getTime() + ARMED_PLACEMENT_LIFETIME_TICKS,
                        !alreadyEligible
                )
        );
    }

    /**
     * Returns a safe vanilla-representable distance, or -1 to keep vanilla behavior.
     * Established extended blocks are an O(1) indexed lookup.
     */
    public static int getDistanceOverride(BlockView blockView, BlockPos pos) {
        if (!(blockView instanceof ServerWorld world)) {
            return -1;
        }

        PlacementKey placementKey = key(world, pos);
        ArmedPlacement armedPlacement = ARMED_PLACEMENTS.get(placementKey);
        boolean armed = armedPlacement != null;
        BuildingScaffoldingState state = BuildingScaffoldingState.get(world.getServer());
        boolean eligible = state.isEligible(world, pos);
        int storedDistance = state.getDistance(world, pos);

        if (!armed && !eligible) {
            return -1;
        }

        if (!armed && !world.getBlockState(pos).isOf(Blocks.SCAFFOLDING)) {
            boolean removed = state.removeAll(world, pos);
            if (removed) {
                queueDependents(world, pos, state);
            }
            return -1;
        }

        // Active extended positions use a constant-time indexed answer. Their
        // exact distance is repaired by the bounded dirty queue after changes.
        if (!armed && storedDistance >= BuildingScaffoldingState.MIN_EXTENDED_DISTANCE) {
            return VANILLA_STABLE_DISTANCE;
        }

        // Armed placements and eligible vanilla positions are calculated from
        // five direct predecessors only. This also lets a previously stable
        // perk structure become extended again after its supports change.
        int calculated = calculateLocalDistance(world, pos, state);
        if (calculated == UNKNOWN_DISTANCE) {
            return -1;
        }
        if (calculated <= VANILLA_STABLE_DISTANCE) {
            state.removeExtended(world, pos);
            return -1;
        }
        if (calculated <= MAX_HORIZONTAL_DISTANCE) {
            if (state.putDistance(world, pos, calculated)) {
                queueDependents(world, pos, state);
                return VANILLA_STABLE_DISTANCE;
            }
            warnIndexLimit(world.getServer());
            return -1;
        }

        state.removeExtended(world, pos);
        return -1;
    }

    /** Called by the scaffolding neighbor-update mixin. */
    public static void onNeighborChanged(ServerWorld world, BlockPos pos) {
        BuildingScaffoldingState state = BuildingScaffoldingState.get(world.getServer());
        if (state.isEligible(world, pos)) {
            queueDirty(world, pos);
        }
    }

    /** Called for every removal path, including explosions and piston replacement. */
    public static void onScaffoldingRemoved(ServerWorld world, BlockPos pos) {
        ARMED_PLACEMENTS.remove(key(world, pos));
        BuildingScaffoldingState state = BuildingScaffoldingState.get(world.getServer());
        boolean wasEligible = state.removeAll(world, pos);
        if (wasEligible) {
            queueDependents(world, pos, state);
        }
    }


    private static void reconcileDimensions(MinecraftServer server) {
        Set<String> validDimensions = new HashSet<>();
        for (ServerWorld world : server.getWorlds()) {
            validDimensions.add(world.getRegistryKey().getValue().toString());
        }

        int removed = BuildingScaffoldingState.get(server).retainDimensions(validDimensions);
        if (removed > 0) {
            MythicRPG.LOGGER.info(
                    "Removed {} stale extended scaffolding entries from unavailable dimensions.",
                    removed
            );
        }
    }

    private static void onChunkLoaded(ServerWorld world, net.minecraft.world.chunk.WorldChunk chunk) {
        BuildingScaffoldingState state = BuildingScaffoldingState.get(world.getServer());
        int chunkX = chunk.getPos().x;
        int chunkZ = chunk.getPos().z;

        for (long packedPos : state.positionsInChunk(world, chunkX, chunkZ)) {
            BlockPos pos = BlockPos.fromLong(packedPos);
            if (!chunk.getBlockState(pos).isOf(Blocks.SCAFFOLDING)) {
                state.removeAll(world, pos);
                queueDependents(world, pos, state);
            } else {
                queueDirty(world, pos);
            }
        }

        // A newly loaded border can change the support of already loaded entries
        // in an adjacent chunk. Only the single touching border is queued.
        queueLoadedBorder(world, state, chunkX - 1, chunkZ, Direction.EAST);
        queueLoadedBorder(world, state, chunkX + 1, chunkZ, Direction.WEST);
        queueLoadedBorder(world, state, chunkX, chunkZ - 1, Direction.SOUTH);
        queueLoadedBorder(world, state, chunkX, chunkZ + 1, Direction.NORTH);
    }

    private static void queueLoadedBorder(
            ServerWorld world,
            BuildingScaffoldingState state,
            int chunkX,
            int chunkZ,
            Direction touchingSide
    ) {
        if (!world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
            return;
        }
        for (long packedPos : state.positionsInChunk(world, chunkX, chunkZ)) {
            BlockPos pos = BlockPos.fromLong(packedPos);
            int localX = pos.getX() & 15;
            int localZ = pos.getZ() & 15;
            boolean touches = switch (touchingSide) {
                case EAST -> localX == 15;
                case WEST -> localX == 0;
                case SOUTH -> localZ == 15;
                case NORTH -> localZ == 0;
                default -> false;
            };
            if (touches) {
                queueDirty(world, pos);
            }
        }
    }

    private static void expireArmedPlacements(MinecraftServer server) {
        Iterator<Map.Entry<PlacementKey, ArmedPlacement>> iterator =
                ARMED_PLACEMENTS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<PlacementKey, ArmedPlacement> entry = iterator.next();
            PlacementKey placement = entry.getKey();
            ArmedPlacement armed = entry.getValue();
            if (placement.server() != server) {
                continue;
            }

            ServerWorld world = server.getWorld(placement.worldKey());
            if (world == null) {
                iterator.remove();
                continue;
            }
            if (world.getTime() < armed.expiresAt()) {
                continue;
            }

            BlockPos pos = BlockPos.fromLong(placement.packedPos());
            BuildingScaffoldingState state = BuildingScaffoldingState.get(server);
            if (!isChunkLoaded(world, pos)) {
                // The chunk-load reconciliation will validate the persisted entry.
            } else if (!world.getBlockState(pos).isOf(Blocks.SCAFFOLDING)) {
                if (armed.eligibilityAdded() && state.removeAll(world, pos)) {
                    queueDependents(world, pos, state);
                }
            } else if (state.isEligible(world, pos)) {
                queueDirty(world, pos);
            }
            iterator.remove();
        }
    }

    private static void processDirtyQueues(MinecraftServer server) {
        Map<RegistryKey<World>, DirtyQueue> byWorld = DIRTY_QUEUES.get(server);
        if (byWorld == null || byWorld.isEmpty()) {
            return;
        }

        int remainingBudget = MAX_DISTANCE_UPDATES_PER_TICK;
        Iterator<Map.Entry<RegistryKey<World>, DirtyQueue>> iterator = byWorld.entrySet().iterator();
        while (iterator.hasNext() && remainingBudget > 0) {
            Map.Entry<RegistryKey<World>, DirtyQueue> entry = iterator.next();
            ServerWorld world = server.getWorld(entry.getKey());
            DirtyQueue queue = entry.getValue();
            if (world == null) {
                iterator.remove();
                continue;
            }

            BuildingScaffoldingState state = BuildingScaffoldingState.get(server);
            while (remainingBudget > 0 && !queue.positions().isEmpty()) {
                long packedPos = queue.positions().removeFirst();
                queue.queued().remove(packedPos);
                updateExtendedPosition(world, BlockPos.fromLong(packedPos), state);
                remainingBudget--;
            }
            if (queue.positions().isEmpty()) {
                iterator.remove();
            }
        }

        if (byWorld.isEmpty()) {
            DIRTY_QUEUES.remove(server);
        }
    }

    private static void updateExtendedPosition(
            ServerWorld world,
            BlockPos pos,
            BuildingScaffoldingState state
    ) {
        if (!state.isEligible(world, pos) || !isChunkLoaded(world, pos)) {
            return;
        }

        int oldDistance = state.getDistance(world, pos);
        BlockState blockState = world.getBlockState(pos);
        if (!blockState.isOf(Blocks.SCAFFOLDING)) {
            state.removeAll(world, pos);
            queueDependents(world, pos, state);
            return;
        }

        int newDistance = calculateLocalDistance(world, pos, state);
        if (newDistance == UNKNOWN_DISTANCE) {
            // No chunk is loaded merely for this system. The touching chunk-load
            // event will queue this position again.
            return;
        }

        boolean remainsExtended = newDistance > VANILLA_STABLE_DISTANCE
                && newDistance <= MAX_HORIZONTAL_DISTANCE;
        int displayedDistance;
        if (remainsExtended) {
            if (!state.putDistance(world, pos, newDistance)) {
                warnIndexLimit(world.getServer());
                state.removeExtended(world, pos);
                remainsExtended = false;
                displayedDistance = VANILLA_UNSUPPORTED_DISTANCE;
            } else {
                displayedDistance = VANILLA_STABLE_DISTANCE;
            }
        } else {
            state.removeExtended(world, pos);
            displayedDistance = newDistance <= VANILLA_STABLE_DISTANCE
                    ? newDistance
                    : VANILLA_UNSUPPORTED_DISTANCE;
        }

        int currentDisplayed = blockState.get(ScaffoldingBlock.DISTANCE);
        boolean bottom = displayedDistance > 0
                && !world.getBlockState(pos.down()).isOf(Blocks.SCAFFOLDING);
        boolean currentBottom = blockState.get(ScaffoldingBlock.BOTTOM);
        if (currentDisplayed != displayedDistance || currentBottom != bottom) {
            BlockState corrected = blockState
                    .with(ScaffoldingBlock.DISTANCE, displayedDistance)
                    .with(ScaffoldingBlock.BOTTOM, bottom);
            int flags = remainsExtended
                    ? Block.NOTIFY_LISTENERS | Block.FORCE_STATE
                    : Block.NOTIFY_ALL | Block.FORCE_STATE;
            world.setBlockState(pos, corrected, flags);
        }

        int indexedDistance = remainsExtended ? newDistance : -1;
        if (indexedDistance != oldDistance) {
            queueDependents(world, pos, state);
        }
        if (!remainsExtended && displayedDistance == VANILLA_UNSUPPORTED_DISTANCE) {
            // Vanilla performs its normal falling behavior on the next tick.
            world.scheduleBlockTick(pos, Blocks.SCAFFOLDING, 1);
        }
    }

    /**
     * Calculates one position from its direct predecessors only: solid block
     * below, scaffolding below (zero cost), and four horizontal neighbors (+1).
     */
    private static int calculateLocalDistance(
            ServerWorld world,
            BlockPos pos,
            BuildingScaffoldingState extendedState
    ) {
        int best = INTERNAL_UNSUPPORTED_DISTANCE;
        boolean unknownNeighbor = false;

        BlockPos below = pos.down();
        if (world.isInBuildLimit(below)) {
            BlockState belowState = world.getBlockState(below);
            if (belowState.isOf(Blocks.SCAFFOLDING)) {
                int belowDistance = effectiveDistance(extendedState, world, below, belowState);
                if (belowDistance <= MAX_HORIZONTAL_DISTANCE) {
                    best = belowDistance;
                }
            } else if (belowState.isSideSolidFullSquare(world, below, Direction.UP)) {
                return 0;
            }
        }

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos neighborPos = pos.offset(direction);
            if (!world.getChunkManager().isChunkLoaded(
                    neighborPos.getX() >> 4,
                    neighborPos.getZ() >> 4
            )) {
                unknownNeighbor = true;
                continue;
            }

            BlockState neighborState = world.getBlockState(neighborPos);
            if (!neighborState.isOf(Blocks.SCAFFOLDING)) {
                continue;
            }
            int neighborDistance = effectiveDistance(
                    extendedState,
                    world,
                    neighborPos,
                    neighborState
            );
            if (neighborDistance <= MAX_HORIZONTAL_DISTANCE) {
                best = Math.min(best, neighborDistance + 1);
            }
        }

        if (best > MAX_HORIZONTAL_DISTANCE && unknownNeighbor) {
            return UNKNOWN_DISTANCE;
        }
        return Math.min(best, INTERNAL_UNSUPPORTED_DISTANCE);
    }

    private static int effectiveDistance(
            BuildingScaffoldingState extendedState,
            ServerWorld world,
            BlockPos pos,
            BlockState blockState
    ) {
        int extendedDistance = extendedState.getDistance(world, pos);
        if (extendedDistance >= BuildingScaffoldingState.MIN_EXTENDED_DISTANCE) {
            return extendedDistance;
        }

        int vanillaDistance = blockState.get(ScaffoldingBlock.DISTANCE);
        // Vanilla distance 7 means unsupported; it must never become a root for
        // another extended block.
        return vanillaDistance <= VANILLA_STABLE_DISTANCE
                ? vanillaDistance
                : INTERNAL_UNSUPPORTED_DISTANCE;
    }

    private static void queueDependents(
            ServerWorld world,
            BlockPos changedPos,
            BuildingScaffoldingState state
    ) {
        BlockPos above = changedPos.up();
        if (state.isEligible(world, above)) {
            queueDirty(world, above);
        }
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos neighbor = changedPos.offset(direction);
            if (state.isEligible(world, neighbor)) {
                queueDirty(world, neighbor);
            }
        }
    }

    private static void queueDirty(ServerWorld world, BlockPos pos) {
        MinecraftServer server = world.getServer();
        Map<RegistryKey<World>, DirtyQueue> byWorld = DIRTY_QUEUES.computeIfAbsent(
                server,
                ignored -> new HashMap<>()
        );
        DirtyQueue queue = byWorld.computeIfAbsent(world.getRegistryKey(), ignored -> new DirtyQueue());
        long packedPos = pos.asLong();
        if (queue.queued().contains(packedPos)) {
            return;
        }
        if (queue.queued().size() >= MAX_QUEUED_POSITIONS_PER_WORLD) {
            warnQueueLimit(server, world);
            return;
        }
        queue.queued().add(packedPos);
        queue.positions().addLast(packedPos);
    }


    private static boolean isChunkLoaded(ServerWorld world, BlockPos pos) {
        return world.getChunkManager().isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static void warnIndexLimit(MinecraftServer server) {
        if (INDEX_LIMIT_WARNING_EMITTED.add(server)) {
            MythicRPG.LOGGER.warn(
                    "Extended scaffolding index reached a safety limit; new extended blocks will use vanilla support."
            );
        }
    }

    private static void warnQueueLimit(MinecraftServer server, ServerWorld world) {
        if (QUEUE_LIMIT_WARNING_EMITTED.add(server)) {
            MythicRPG.LOGGER.warn(
                    "Extended scaffolding update queue reached its safety limit in {}.",
                    world.getRegistryKey().getValue()
            );
        }
    }

    private static PlacementKey key(ServerWorld world, BlockPos pos) {
        return new PlacementKey(world.getServer(), world.getRegistryKey(), pos.asLong());
    }

    private record DirtyQueue(ArrayDeque<Long> positions, Set<Long> queued) {
        private DirtyQueue() {
            this(new ArrayDeque<>(), new HashSet<>());
        }
    }

    private record ArmedPlacement(long expiresAt, boolean eligibilityAdded) {
    }

    private record PlacementKey(
            MinecraftServer server,
            RegistryKey<World> worldKey,
            long packedPos
    ) {
    }
}
