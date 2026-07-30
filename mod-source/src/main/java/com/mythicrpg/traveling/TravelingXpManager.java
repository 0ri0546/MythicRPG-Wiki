package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class TravelingXpManager {

    private static final int STRUCTURE_SCAN_INTERVAL_TICKS = 20;
    private static final double TELEPORT_DISTANCE_PER_TICK = 16.0;
    private static final double MIN_TRACKED_MOVEMENT = 0.001;

    private static final Map<UUID, MovementTracker> MOVEMENT_TRACKERS = new HashMap<>();

    private TravelingXpManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TravelingXpManager::tick);

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)
                    || !(world instanceof ServerWorld serverWorld)
                    || serverPlayer.isSpectator()) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = serverWorld.getBlockState(pos);

            if (!state.isOf(Blocks.CHEST)) {
                return ActionResult.PASS;
            }

            tryRewardBuriedTreasureChest(serverPlayer, serverWorld, pos);
            return ActionResult.PASS;
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                MOVEMENT_TRACKERS.remove(newPlayer.getUuid())
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                MOVEMENT_TRACKERS.remove(handler.player.getUuid())
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MOVEMENT_TRACKERS.clear());
    }

    public static void clearRuntimePlayer(UUID playerUuid) {
        MOVEMENT_TRACKERS.remove(playerUuid);
    }

    private static void tick(MinecraftServer server) {
        var players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            return;
        }

        TravelingProgressState state = TravelingProgressState.get(server);

        for (ServerPlayerEntity player : players) {
            TravelingDoubleJumpManager.tickPlayer(player);

            if (player.isSpectator() || !player.isAlive()) {
                MOVEMENT_TRACKERS.remove(player.getUuid());
                continue;
            }

            MovementTracker tracker = handleMovementAndDimension(player, state);

            long worldTick = player.getServerWorld().getTime();
            int scanOffset = Math.floorMod(player.getUuid().hashCode(), STRUCTURE_SCAN_INTERVAL_TICKS);
            if ((worldTick + scanOffset) % STRUCTURE_SCAN_INTERVAL_TICKS == 0) {
                BlockPos currentBlockPos = player.getBlockPos();
                if (tracker.shouldScanStructures(currentBlockPos, worldTick)) {
                    scanCurrentStructures(player, state);
                    tracker.recordStructureScan(currentBlockPos, worldTick);
                }
            }
        }
    }

    private static MovementTracker handleMovementAndDimension(
            ServerPlayerEntity player,
            TravelingProgressState state
    ) {
        UUID playerUuid = player.getUuid();
        ServerWorld world = player.getServerWorld();
        RegistryKey<World> dimension = world.getRegistryKey();
        Vec3d currentPos = player.getPos();
        MovementTracker tracker = MOVEMENT_TRACKERS.get(playerUuid);

        if (tracker == null || !tracker.dimension.equals(dimension)) {
            // The starting Overworld is never a rewarded first visit.
            state.markDimensionVisited(playerUuid, TravelingXpConfig.getOverworldId());

            tracker = new MovementTracker(dimension, currentPos);
            MOVEMENT_TRACKERS.put(playerUuid, tracker);
            rewardDimensionIfFirstVisit(player, state, dimension);
            return tracker;
        }

        if (player.isInTeleportationState()) {
            tracker.reset(currentPos);
            return tracker;
        }

        double movedThisTick = horizontalDistance(currentPos, tracker.lastPos);
        tracker.lastPos = currentPos;

        if (movedThisTick > TELEPORT_DISTANCE_PER_TICK) {
            tracker.reset(currentPos);
            return tracker;
        }

        if (movedThisTick < MIN_TRACKED_MOVEMENT) {
            return tracker;
        }

        tracker.traveledDistance += movedThisTick;

        if (tracker.traveledDistance < TravelingXpConfig.getMovementDistanceRequired()) {
            return tracker;
        }

        double directDistance = horizontalDistance(currentPos, tracker.segmentStart);
        if (directDistance < TravelingXpConfig.getMovementDirectDistanceRequired()) {
            return tracker;
        }

        MovementCell destinationCell = createMovementCell(dimension, currentPos);
        boolean newCell = state.markMovementCellRewarded(
                playerUuid,
                destinationCell.dimensionId(),
                destinationCell.x(),
                destinationCell.z()
        );

        tracker.reset(currentPos);

        if (newCell) {
            grantXp(player, TravelingXpSource.MOVEMENT, TravelingXpConfig.getMovementXp());
        }

        return tracker;
    }

    private static void rewardDimensionIfFirstVisit(
            ServerPlayerEntity player,
            TravelingProgressState state,
            RegistryKey<World> dimension
    ) {
        boolean firstVisit = state.markDimensionVisited(player.getUuid(), dimension.getValue());
        if (!firstVisit) {
            return;
        }

        int xp = TravelingXpConfig.getFirstVisitDimensionXp(dimension);
        grantXp(player, TravelingXpSource.DIMENSION, xp);
    }

    private static void scanCurrentStructures(ServerPlayerEntity player, TravelingProgressState state) {
        ServerWorld world = player.getServerWorld();
        BlockPos playerPos = player.getBlockPos();
        StructureAccessor accessor = world.getStructureAccessor();
        Registry<Structure> structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        for (Structure structure : accessor.getStructureReferences(playerPos).keySet()) {
            Optional<RegistryKey<Structure>> keyOptional = structureRegistry.getKey(structure);
            if (keyOptional.isEmpty()) {
                continue;
            }

            RegistryKey<Structure> structureKey = keyOptional.get();
            int xp = TravelingXpConfig.getStructureXp(structureKey);
            if (xp <= 0) {
                continue;
            }

            StructureStart start = accessor.getStructureContaining(playerPos, structure);
            if (start == StructureStart.DEFAULT || !start.hasChildren()) {
                continue;
            }

            ChunkPos startChunk = start.getPos();
            if (state.markStructureDiscovered(
                    player.getUuid(),
                    world.getRegistryKey().getValue(),
                    structureKey.getValue(),
                    startChunk.x,
                    startChunk.z
            )) {
                grantXp(player, TravelingXpSource.STRUCTURE, xp);
            }
        }
    }

    private static void tryRewardBuriedTreasureChest(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos chestPos
    ) {
        if (!(world.getBlockEntity(chestPos) instanceof LootableContainerBlockEntity container)) {
            return;
        }

        TravelingProgressState state = TravelingProgressState.get(player.getServer());
        Identifier dimensionId = world.getRegistryKey().getValue();

        boolean genuineTreasureChest = LootTables.BURIED_TREASURE_CHEST.equals(container.getLootTable());
        if (genuineTreasureChest) {
            state.markVerifiedTreasureChest(dimensionId, chestPos);
        } else if (!state.isVerifiedTreasureChest(dimensionId, chestPos)) {
            return;
        }

        if (state.markTreasureChestOpened(player.getUuid(), dimensionId, chestPos)) {
            grantXp(player, TravelingXpSource.TREASURE, TravelingXpConfig.getTreasureChestXp());

            if (TravelingBonusCache.hasBonus(player, BonusType.TREASURE_VANILLA_XP)) {
                player.addExperience(TravelingXpConfig.getTreasureVanillaXp());
            }
        }
    }

    private static void grantXp(ServerPlayerEntity player, TravelingXpSource source, int baseXp) {
        if (baseXp <= 0) {
            return;
        }

        double bonusMultiplier = TravelingBonusCache.getXpMultiplier(player);

        if (source.isDiscovery()) {
            bonusMultiplier += TravelingBonusCache.getDiscoveryXpMultiplier(player);
        }

        int finalXp = Math.max(1, (int) Math.round(baseXp * (1.0 + bonusMultiplier)));
        SkillXpManager.addXp(player, SkillType.TRAVELING, finalXp, false);
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static MovementCell createMovementCell(RegistryKey<World> dimension, Vec3d pos) {
        int cellSize = TravelingXpConfig.getMovementCellSize();
        int cellX = Math.floorDiv((int) Math.floor(pos.x), cellSize);
        int cellZ = Math.floorDiv((int) Math.floor(pos.z), cellSize);
        return new MovementCell(dimension.getValue(), cellX, cellZ);
    }

    private record MovementCell(Identifier dimensionId, int x, int z) {
    }

    private static final class MovementTracker {
        private static final long STATIONARY_STRUCTURE_RECHECK_TICKS = 200L;

        private final RegistryKey<World> dimension;
        private Vec3d segmentStart;
        private Vec3d lastPos;
        private double traveledDistance;
        private BlockPos lastStructureScanPos;
        private long lastStructureScanTick = Long.MIN_VALUE;

        private MovementTracker(RegistryKey<World> dimension, Vec3d startPos) {
            this.dimension = dimension;
            reset(startPos);
        }

        private void reset(Vec3d position) {
            segmentStart = position;
            lastPos = position;
            traveledDistance = 0.0;
        }

        private boolean shouldScanStructures(BlockPos currentPos, long worldTick) {
            return lastStructureScanPos == null
                    || !lastStructureScanPos.equals(currentPos)
                    || worldTick - lastStructureScanTick >= STATIONARY_STRUCTURE_RECHECK_TICKS;
        }

        private void recordStructureScan(BlockPos currentPos, long worldTick) {
            lastStructureScanPos = currentPos.toImmutable();
            lastStructureScanTick = worldTick;
        }
    }
}
