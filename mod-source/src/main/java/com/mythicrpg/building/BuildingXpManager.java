package com.mythicrpg.building;

import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Event-only Building XP with a persistent, bounded anti-farm history. */
public final class BuildingXpManager {
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_POSITION_HISTORY = 512;
    private static final int MAX_MATERIAL_HISTORY = 256;
    private static final int MAX_PENDING_PLACEMENTS = 8;
    private static final long POSITION_EXPIRY_MILLIS = 30L * 60L * 1_000L;
    private static final long CHECKPOINT_INTERVAL_MILLIS = 2_000L;
    private static final int CHECKPOINT_AWARDS = 20;
    private static final double MATERIAL_DECAY = 0.90;
    private static final double MATERIAL_RECOVERY = 0.10;
    private static final double MIN_MATERIAL_MULTIPLIER = 0.30;

    private static final Map<UUID, ArrayDeque<PendingPlacement>> PENDING = new HashMap<>();
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    private BuildingXpManager() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }
            if (!(player.getStackInHand(hand).getItem() instanceof BlockItem blockItem)
                    || !BuildingBlockCatalog.isEligible(blockItem.getBlock())) {
                return ActionResult.PASS;
            }

            ItemPlacementContext context = new ItemPlacementContext(
                    player,
                    hand,
                    player.getStackInHand(hand),
                    hitResult
            );
            BlockPos placementPos = context.getBlockPos().toImmutable();
            ArrayDeque<PendingPlacement> queue = PENDING.computeIfAbsent(
                    player.getUuid(),
                    ignored -> new ArrayDeque<>(MAX_PENDING_PLACEMENTS)
            );
            String worldId = world.getRegistryKey().getValue().toString();
            queue.removeIf(pending -> pending.worldId().equals(worldId)
                    && pending.pos().equals(placementPos)
                    && pending.block() == blockItem.getBlock());
            while (queue.size() >= MAX_PENDING_PLACEMENTS) {
                queue.removeFirst();
            }
            queue.addLast(new PendingPlacement(
                    worldId,
                    placementPos,
                    world.getBlockState(placementPos),
                    blockItem.getBlock(),
                    world.getTime() + 2L
            ));
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(BuildingXpManager::processPending);
    }

    /** Writes the latest bounded state before a normal disconnect or explicit save boundary. */
    public static void flushPlayer(ServerPlayerEntity player) {
        if (player == null) return;
        PlayerState state = STATES.get(player.getUuid());
        if (state != null) save(player, state, System.currentTimeMillis());
    }

    /** Releases only transient memory. Persistent anti-farm data remains on the player. */
    public static void clearPlayer(UUID playerId) {
        PENDING.remove(playerId);
        STATES.remove(playerId);
    }

    /** Records an eligible placement performed directly by a Building perk. */
    public static void recordDirectPlacement(ServerPlayerEntity player, Block block, BlockPos pos) {
        if (player == null || block == null || pos == null) {
            return;
        }
        award(player, block, pos, System.currentTimeMillis());
    }

    private static void processPending(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ArrayDeque<PendingPlacement>>> players = PENDING.entrySet().iterator();
        while (players.hasNext()) {
            Map.Entry<UUID, ArrayDeque<PendingPlacement>> playerEntry = players.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerEntry.getKey());
            if (player == null) {
                players.remove();
                continue;
            }

            World world = player.getWorld();
            long worldTick = world.getTime();
            String worldId = world.getRegistryKey().getValue().toString();
            Iterator<PendingPlacement> placements = playerEntry.getValue().iterator();
            while (placements.hasNext()) {
                PendingPlacement pending = placements.next();
                if (!worldId.equals(pending.worldId())) {
                    placements.remove();
                    continue;
                }
                BlockState placedState = world.getBlockState(pending.pos());
                if (!placedState.equals(pending.previousState()) && placedState.isOf(pending.block())) {
                    award(player, pending.block(), pending.pos(), System.currentTimeMillis());
                    placements.remove();
                } else if (worldTick >= pending.expiresAtTick()) {
                    placements.remove();
                }
            }
            if (playerEntry.getValue().isEmpty()) {
                players.remove();
            }
        }
    }

    private static void award(ServerPlayerEntity player, Block block, BlockPos pos, long nowMillis) {
        int baseXp = BuildingBlockCatalog.baseXp(block);
        if (baseXp <= 0) {
            return;
        }

        PlayerState state = STATES.computeIfAbsent(player.getUuid(), ignored -> load(player, nowMillis));
        double materialMultiplier = state.materialMultiplier(block);
        double positionMultiplier = state.positionMultiplier(player.getWorld(), pos, nowMillis);
        double total = baseXp * materialMultiplier * positionMultiplier + state.fractionalXp;
        int wholeXp = (int) Math.floor(total);
        state.fractionalXp = total - wholeXp;
        state.dirtyAwards++;
        if (wholeXp > 0) {
            SkillXpManager.addXp(player, SkillType.BUILDING, wholeXp);
        }
        if (state.dirtyAwards >= CHECKPOINT_AWARDS
                || nowMillis - state.lastCheckpointMillis >= CHECKPOINT_INTERVAL_MILLIS) {
            save(player, state, nowMillis);
        }
    }

    private static PlayerState load(ServerPlayerEntity player, long nowMillis) {
        PlayerState state = new PlayerState();
        if (!(player instanceof BuildingXpDataHolder holder)) {
            return state;
        }
        NbtCompound root = holder.mythicrpg$getBuildingXpData();
        if (root.getInt("Version") != FORMAT_VERSION) {
            return state;
        }

        double fractional = root.getDouble("FractionalXp");
        if (Double.isFinite(fractional) && fractional >= 0.0D && fractional < 1.0D) {
            state.fractionalXp = fractional;
        }

        if (root.contains("Positions", NbtElement.LIST_TYPE)) {
            NbtList positions = root.getList("Positions", NbtElement.COMPOUND_TYPE);
            int start = Math.max(0, positions.size() - MAX_POSITION_HISTORY);
            for (int index = start; index < positions.size(); index++) {
                NbtCompound tag = positions.getCompound(index);
                String dimension = tag.getString("Dimension");
                long lastUse = tag.getLong("LastUseMillis");
                if (dimension.isBlank() || dimension.length() > 256
                        || Identifier.tryParse(dimension) == null
                        || lastUse <= 0L
                        || nowMillis - lastUse > POSITION_EXPIRY_MILLIS) {
                    continue;
                }
                int reuse = Math.max(0, Math.min(3, tag.getInt("ReuseCount")));
                PositionKey key = new PositionKey(dimension, tag.getLong("Position"));
                long revision = ++state.positionRevision;
                state.positions.put(key, new PositionUse(reuse, lastUse, revision));
                state.positionExpiries.addLast(new PositionExpiry(key, lastUse, revision));
            }
        }

        if (root.contains("Materials", NbtElement.LIST_TYPE)) {
            NbtList materials = root.getList("Materials", NbtElement.COMPOUND_TYPE);
            int limit = Math.min(materials.size(), MAX_MATERIAL_HISTORY);
            for (int index = 0; index < limit; index++) {
                NbtCompound tag = materials.getCompound(index);
                Identifier id = Identifier.tryParse(tag.getString("Block"));
                double multiplier = tag.getDouble("Multiplier");
                if (id == null || !Registries.BLOCK.containsId(id)
                        || !Double.isFinite(multiplier)
                        || multiplier < MIN_MATERIAL_MULTIPLIER
                        || multiplier > 1.0D) {
                    continue;
                }
                state.materialMultipliers.put(
                        Registries.BLOCK.get(id),
                        new MaterialUse(multiplier, -1L)
                );
            }
        }
        state.rebuildExpiryQueue();
        state.prune(nowMillis);
        state.lastCheckpointMillis = nowMillis;
        return state;
    }

    private static void save(ServerPlayerEntity player, PlayerState state, long nowMillis) {
        if (!(player instanceof BuildingXpDataHolder holder)) {
            return;
        }
        state.prune(nowMillis);
        NbtCompound root = new NbtCompound();
        root.putInt("Version", FORMAT_VERSION);
        root.putDouble("FractionalXp", Math.max(0.0D, Math.min(Math.nextDown(1.0D), state.fractionalXp)));

        NbtList positions = new NbtList();
        for (Map.Entry<PositionKey, PositionUse> entry : state.positions.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putString("Dimension", entry.getKey().worldId());
            tag.putLong("Position", entry.getKey().packedPos());
            tag.putInt("ReuseCount", Math.max(0, Math.min(3, entry.getValue().reuseCount)));
            tag.putLong("LastUseMillis", entry.getValue().lastUseMillis);
            positions.add(tag);
        }
        root.put("Positions", positions);

        state.normalizeMaterialRecovery();
        NbtList materials = new NbtList();
        int written = 0;
        for (Map.Entry<Block, MaterialUse> entry : state.materialMultipliers.entrySet()) {
            if (written++ >= MAX_MATERIAL_HISTORY) {
                break;
            }
            NbtCompound tag = new NbtCompound();
            tag.putString("Block", Registries.BLOCK.getId(entry.getKey()).toString());
            tag.putDouble("Multiplier", Math.max(
                    MIN_MATERIAL_MULTIPLIER,
                    Math.min(1.0D, entry.getValue().multiplier)
            ));
            materials.add(tag);
        }
        root.put("Materials", materials);
        holder.mythicrpg$setBuildingXpData(root);
        state.dirtyAwards = 0;
        state.lastCheckpointMillis = nowMillis;
    }

    private record PendingPlacement(
            String worldId,
            BlockPos pos,
            BlockState previousState,
            Block block,
            long expiresAtTick
    ) {}

    private record PositionKey(String worldId, long packedPos) {}

    private record PositionExpiry(PositionKey key, long lastUseMillis, long revision) {}

    private static final class PositionUse {
        int reuseCount;
        long lastUseMillis;
        long revision;

        PositionUse(int reuseCount, long lastUseMillis, long revision) {
            this.reuseCount = reuseCount;
            this.lastUseMillis = lastUseMillis;
            this.revision = revision;
        }
    }


    private static final class MaterialUse {
        private double multiplier;
        private long lastPlacedEvent;

        private MaterialUse(double multiplier, long lastPlacedEvent) {
            this.multiplier = multiplier;
            this.lastPlacedEvent = lastPlacedEvent;
        }
    }

    private static final class PlayerState {
        private final Map<PositionKey, PositionUse> positions = new HashMap<>();
        private final ArrayDeque<PositionExpiry> positionExpiries = new ArrayDeque<>();
        private final LinkedHashMap<Block, MaterialUse> materialMultipliers =
                new LinkedHashMap<>(32, 0.75F, true);
        private double fractionalXp;
        private int dirtyAwards;
        private long lastCheckpointMillis;
        private long materialEventIndex;
        private long positionRevision;

        double materialMultiplier(Block placed) {
            long eventIndex = materialEventIndex++;
            MaterialUse use = materialMultipliers.get(placed);
            double current = use == null
                    ? 1.0D
                    : recoveredMultiplier(use, eventIndex);

            materialMultipliers.put(
                    placed,
                    new MaterialUse(
                            Math.max(MIN_MATERIAL_MULTIPLIER, current * MATERIAL_DECAY),
                            eventIndex
                    )
            );
            while (materialMultipliers.size() > MAX_MATERIAL_HISTORY) {
                Iterator<Block> iterator = materialMultipliers.keySet().iterator();
                iterator.next();
                iterator.remove();
            }
            return current;
        }

        double positionMultiplier(World world, BlockPos pos, long nowMillis) {
            prune(nowMillis);
            PositionKey key = new PositionKey(
                    world.getRegistryKey().getValue().toString(),
                    pos.asLong()
            );
            PositionUse use = positions.get(key);
            double multiplier;
            if (use == null) {
                use = new PositionUse(0, nowMillis, ++positionRevision);
                positions.put(key, use);
                multiplier = 1.0D;
            } else {
                multiplier = switch (use.reuseCount) {
                    case 0 -> 0.20D;
                    case 1 -> 0.04D;
                    default -> 0.008D;
                };
                use.reuseCount = Math.min(3, use.reuseCount + 1);
                use.lastUseMillis = nowMillis;
                use.revision = ++positionRevision;
            }
            positionExpiries.addLast(new PositionExpiry(key, nowMillis, use.revision));
            compactExpiryQueueIfNeeded();

            while (positions.size() > MAX_POSITION_HISTORY) {
                evictOldestPosition();
            }
            return multiplier;
        }

        void prune(long nowMillis) {
            while (!positionExpiries.isEmpty()) {
                PositionExpiry expiry = positionExpiries.peekFirst();
                if (nowMillis - expiry.lastUseMillis() <= POSITION_EXPIRY_MILLIS) {
                    break;
                }
                positionExpiries.removeFirst();
                PositionUse current = positions.get(expiry.key());
                if (current != null && current.revision == expiry.revision()) {
                    positions.remove(expiry.key());
                }
            }
            compactExpiryQueueIfNeeded();
        }

        private void evictOldestPosition() {
            while (!positionExpiries.isEmpty()) {
                PositionExpiry expiry = positionExpiries.removeFirst();
                PositionUse current = positions.get(expiry.key());
                if (current != null && current.revision == expiry.revision()) {
                    positions.remove(expiry.key());
                    return;
                }
            }

            // Defensive fallback for malformed runtime state: preserve the bound.
            Iterator<PositionKey> iterator = positions.keySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        void normalizeMaterialRecovery() {
            long normalizedEvent = materialEventIndex - 1L;
            for (MaterialUse use : materialMultipliers.values()) {
                use.multiplier = recoveredMultiplier(use, materialEventIndex);
                use.lastPlacedEvent = normalizedEvent;
            }
        }

        private double recoveredMultiplier(MaterialUse use, long currentEvent) {
            long interveningEvents = Math.max(0L, currentEvent - use.lastPlacedEvent - 1L);
            if (interveningEvents == 0L || use.multiplier >= 1.0D) {
                return use.multiplier;
            }
            return Math.min(1.0D, use.multiplier + interveningEvents * MATERIAL_RECOVERY);
        }

        private void compactExpiryQueueIfNeeded() {
            int maximumUsefulEntries = positions.size() * 4 + 64;
            if (positionExpiries.size() > maximumUsefulEntries) {
                rebuildExpiryQueue();
            }
        }

        private void rebuildExpiryQueue() {
            ArrayList<PositionExpiry> ordered = new ArrayList<>(positions.size());
            for (Map.Entry<PositionKey, PositionUse> entry : positions.entrySet()) {
                ordered.add(new PositionExpiry(
                        entry.getKey(),
                        entry.getValue().lastUseMillis,
                        entry.getValue().revision
                ));
            }
            ordered.sort(java.util.Comparator.comparingLong(PositionExpiry::lastUseMillis));
            positionExpiries.clear();
            positionExpiries.addAll(ordered);
        }
    }
}
