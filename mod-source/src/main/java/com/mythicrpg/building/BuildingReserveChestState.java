package com.mythicrpg.building;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent, chunk-bucketed reserve chest index with bounded lazy repair. */
public final class BuildingReserveChestState extends PersistentState {
    private static final int FORMAT_VERSION = 2;
    private static final int MAX_ENTRIES = 131_072;
    private static final String STATE_ID = "mythicrpg_building_reserve_chests";
    private static final Type<BuildingReserveChestState> TYPE = new Type<>(
            BuildingReserveChestState::new,
            BuildingReserveChestState::fromNbt,
            null
    );

    private final Map<String, Entry> entriesByKey = new HashMap<>();
    private final Map<String, Map<Long, Set<String>>> keysByDimensionAndChunk = new HashMap<>();
    private final Map<UUID, Set<String>> keysByOwner = new HashMap<>();

    public static BuildingReserveChestState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public int count(UUID owner) {
        Set<String> values = keysByOwner.get(owner);
        return values == null ? 0 : values.size();
    }

    public boolean contains(ServerWorld world, BlockPos pos) {
        String key = key(dimensionId(world), pos.asLong());
        Entry entry = entriesByKey.get(key);
        if (entry == null) return false;
        if (world.isChunkLoaded(pos)) {
            if (!(world.getBlockEntity(pos) instanceof BuildingReserveChestBlockEntity chest)
                    || !chest.hasOwner()) {
                removeInternal(entry);
                markDirty();
                return false;
            }
            UUID actualOwner = chest.owner();
            if (actualOwner != null && !actualOwner.equals(entry.owner())) {
                removeInternal(entry);
                addInternal(new Entry(entry.dimension(), entry.packedPos(), actualOwner));
                markDirty();
            }
        }
        return true;
    }

    public boolean add(ServerWorld world, BlockPos pos, UUID owner, int maximum, boolean enforceMaximum) {
        reconcileRelevant(world, pos, owner);
        String dimension = dimensionId(world);
        long packedPos = pos.asLong();
        String key = key(dimension, packedPos);
        Entry previous = entriesByKey.get(key);

        if (previous != null) {
            if (previous.owner().equals(owner)) return true;
            removeInternal(previous);
        }

        if (entriesByKey.size() >= MAX_ENTRIES
                || (enforceMaximum && count(owner) >= maximum)) {
            if (previous != null) addInternal(previous);
            return false;
        }

        addInternal(new Entry(dimension, packedPos, owner));
        markDirty();
        return true;
    }

    public void remove(ServerWorld world, BlockPos pos) {
        Entry removed = entriesByKey.get(key(dimensionId(world), pos.asLong()));
        if (removed == null) return;
        removeInternal(removed);
        markDirty();
    }

    public List<Entry> nearby(ServerWorld world, BlockPos center, int radius, UUID owner) {
        reconcileRelevant(world, center, owner);
        String dimension = dimensionId(world);
        Map<Long, Set<String>> byChunk = keysByDimensionAndChunk.get(dimension);
        if (byChunk == null || byChunk.isEmpty()) return List.of();

        int minChunkX = Math.floorDiv(center.getX() - radius, 16);
        int maxChunkX = Math.floorDiv(center.getX() + radius, 16);
        int minChunkZ = Math.floorDiv(center.getZ() - radius, 16);
        int maxChunkZ = Math.floorDiv(center.getZ() + radius, 16);
        long radiusSquared = (long) radius * radius;
        Set<String> candidateKeys = new HashSet<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Set<String> keys = byChunk.get(chunkKey(chunkX, chunkZ));
                if (keys != null) candidateKeys.addAll(keys);
            }
        }
        reconcileKeys(world.getServer(), candidateKeys);

        ArrayList<Entry> result = new ArrayList<>();
        for (String candidateKey : candidateKeys) {
            Entry entry = entriesByKey.get(candidateKey);
            if (entry == null || !entry.owner().equals(owner)) continue;
            BlockPos pos = BlockPos.fromLong(entry.packedPos());
            long dx = (long) pos.getX() - center.getX();
            long dy = (long) pos.getY() - center.getY();
            long dz = (long) pos.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) result.add(entry);
        }
        result.sort(Comparator.comparingLong(entry -> squaredDistance(center, entry.packedPos())));
        return List.copyOf(result);
    }

    private void reconcileRelevant(ServerWorld context, BlockPos target, UUID owner) {
        Set<String> relevant = new HashSet<>();
        Set<String> ownerKeys = keysByOwner.get(owner);
        if (ownerKeys != null) relevant.addAll(ownerKeys);
        Map<Long, Set<String>> byChunk = keysByDimensionAndChunk.get(dimensionId(context));
        if (byChunk != null) {
            Set<String> chunkKeys = byChunk.get(chunkKey(target.getX() >> 4, target.getZ() >> 4));
            if (chunkKeys != null) relevant.addAll(chunkKeys);
        }
        reconcileKeys(context.getServer(), relevant);
    }

    private void reconcileKeys(MinecraftServer server, Set<String> keys) {
        boolean changed = false;
        for (String key : Set.copyOf(keys)) {
            Entry entry = entriesByKey.get(key);
            if (entry == null) continue;
            ServerWorld world = findWorld(server, entry.dimension());
            if (world == null) {
                removeInternal(entry);
                changed = true;
                continue;
            }
            BlockPos pos = BlockPos.fromLong(entry.packedPos());
            if (!world.isChunkLoaded(pos)) continue;
            if (!(world.getBlockEntity(pos) instanceof BuildingReserveChestBlockEntity chest)
                    || !chest.hasOwner()) {
                removeInternal(entry);
                changed = true;
                continue;
            }
            UUID actualOwner = chest.owner();
            if (actualOwner != null && !actualOwner.equals(entry.owner())) {
                removeInternal(entry);
                addInternal(new Entry(entry.dimension(), entry.packedPos(), actualOwner));
                changed = true;
            }
        }
        if (changed) markDirty();
    }

    /** Removes only entries that reference dimensions absent from this server. */
    public int pruneMissingDimensions(MinecraftServer server) {
        Set<String> loadedDimensions = new HashSet<>();
        for (ServerWorld world : server.getWorlds()) {
            loadedDimensions.add(dimensionId(world));
        }
        int removed = 0;
        for (Entry entry : java.util.List.copyOf(entriesByKey.values())) {
            if (!loadedDimensions.contains(entry.dimension())) {
                removeInternal(entry);
                removed++;
            }
        }
        if (removed > 0) markDirty();
        return removed;
    }

    private static ServerWorld findWorld(MinecraftServer server, String dimension) {
        for (ServerWorld world : server.getWorlds()) {
            if (dimensionId(world).equals(dimension)) return world;
        }
        return null;
    }

    private static long squaredDistance(BlockPos center, long packedPos) {
        BlockPos pos = BlockPos.fromLong(packedPos);
        long dx = (long) pos.getX() - center.getX();
        long dy = (long) pos.getY() - center.getY();
        long dz = (long) pos.getZ() - center.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private void addInternal(Entry entry) {
        String key = key(entry.dimension(), entry.packedPos());
        entriesByKey.put(key, entry);
        BlockPos pos = BlockPos.fromLong(entry.packedPos());
        keysByDimensionAndChunk
                .computeIfAbsent(entry.dimension(), ignored -> new HashMap<>())
                .computeIfAbsent(chunkKey(pos.getX() >> 4, pos.getZ() >> 4), ignored -> new HashSet<>())
                .add(key);
        keysByOwner.computeIfAbsent(entry.owner(), ignored -> new HashSet<>()).add(key);
    }

    private void removeInternal(Entry entry) {
        String key = key(entry.dimension(), entry.packedPos());
        entriesByKey.remove(key);
        unindexChunk(entry, key);
        Set<String> ownerKeys = keysByOwner.get(entry.owner());
        if (ownerKeys != null) {
            ownerKeys.remove(key);
            if (ownerKeys.isEmpty()) keysByOwner.remove(entry.owner());
        }
    }

    private void unindexChunk(Entry entry, String key) {
        Map<Long, Set<String>> byChunk = keysByDimensionAndChunk.get(entry.dimension());
        if (byChunk == null) return;
        BlockPos pos = BlockPos.fromLong(entry.packedPos());
        long chunk = chunkKey(pos.getX() >> 4, pos.getZ() >> 4);
        Set<String> keys = byChunk.get(chunk);
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) byChunk.remove(chunk);
        }
        if (byChunk.isEmpty()) keysByDimensionAndChunk.remove(entry.dimension());
    }

    private static String dimensionId(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static String key(String dimension, long packedPos) {
        return dimension + "|" + packedPos;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX & 0xFFFFFFFFL) | (((long) chunkZ & 0xFFFFFFFFL) << 32);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("Version", FORMAT_VERSION);
        NbtList entries = new NbtList();
        int written = 0;
        for (Entry entry : entriesByKey.values()) {
            if (written++ >= MAX_ENTRIES) break;
            NbtCompound tag = new NbtCompound();
            tag.putString("Dimension", entry.dimension());
            tag.putLong("Position", entry.packedPos());
            tag.putUuid("Owner", entry.owner());
            entries.add(tag);
        }
        nbt.put("Entries", entries);
        return nbt;
    }

    private static BuildingReserveChestState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        BuildingReserveChestState state = new BuildingReserveChestState();
        NbtList entries = nbt.getList("Entries", NbtElement.COMPOUND_TYPE);
        int limit = Math.min(entries.size(), MAX_ENTRIES);
        for (int index = 0; index < limit; index++) {
            NbtCompound tag = entries.getCompound(index);
            if (!tag.containsUuid("Owner")) continue;
            String dimension = tag.getString("Dimension");
            if (dimension.isBlank() || dimension.length() > 256
                    || Identifier.tryParse(dimension) == null) continue;
            Entry entry = new Entry(dimension, tag.getLong("Position"), tag.getUuid("Owner"));
            if (!state.entriesByKey.containsKey(key(entry.dimension(), entry.packedPos()))) {
                state.addInternal(entry);
            }
        }
        return state;
    }

    public record Entry(String dimension, long packedPos, UUID owner) {}
}
