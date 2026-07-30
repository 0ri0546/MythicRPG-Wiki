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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent quota index with bounded, lazy reconciliation of only relevant entries. */
public final class StaticDecorationState extends PersistentState {
    public static final int MAX_PER_PLAYER = 16;
    public static final int MAX_PER_CHUNK = 8;
    private static final int FORMAT_VERSION = 2;
    private static final int MAX_ENTRIES = 262_144;

    private static final String STATE_ID = "mythicrpg_static_decorations";
    private static final Type<StaticDecorationState> TYPE = new Type<>(
            StaticDecorationState::new,
            StaticDecorationState::fromNbt,
            null
    );

    private final Map<String, Entry> entries = new HashMap<>();
    private final Map<UUID, Set<String>> keysByOwner = new HashMap<>();
    private final Map<String, Set<String>> keysByChunk = new HashMap<>();

    public static StaticDecorationState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public boolean canPlace(ServerWorld world, BlockPos pos, UUID owner) {
        reconcileRelevant(world, pos, owner);
        String key = entryKey(world, pos);
        Entry previous = entries.get(key);
        if (previous != null && previous.owner().equals(owner)) return true;
        return entries.size() < MAX_ENTRIES
                && size(keysByOwner.get(owner)) < MAX_PER_PLAYER
                && size(keysByChunk.get(chunkKey(world, pos))) < MAX_PER_CHUNK;
    }

    public boolean add(ServerWorld world, BlockPos pos, UUID owner) {
        reconcileRelevant(world, pos, owner);
        String key = entryKey(world, pos);
        Entry previous = entries.get(key);
        if (previous != null) return previous.owner().equals(owner);
        if (entries.size() >= MAX_ENTRIES
                || size(keysByOwner.get(owner)) >= MAX_PER_PLAYER
                || size(keysByChunk.get(chunkKey(world, pos))) >= MAX_PER_CHUNK) {
            return false;
        }
        addInternal(new Entry(dimensionId(world), pos.asLong(), owner));
        markDirty();
        return true;
    }

    public void remove(ServerWorld world, BlockPos pos) {
        Entry removed = entries.get(entryKey(world, pos));
        if (removed == null) return;
        removeInternal(removed);
        markDirty();
    }

    private void reconcileRelevant(ServerWorld context, BlockPos target, UUID owner) {
        Set<String> relevant = new HashSet<>();
        Set<String> ownerKeys = keysByOwner.get(owner);
        if (ownerKeys != null) relevant.addAll(ownerKeys);
        Set<String> chunkKeys = keysByChunk.get(chunkKey(context, target));
        if (chunkKeys != null) relevant.addAll(chunkKeys);
        reconcileKeys(context.getServer(), relevant);
    }

    private void reconcileKeys(MinecraftServer server, Set<String> keys) {
        boolean changed = false;
        for (String key : Set.copyOf(keys)) {
            Entry entry = entries.get(key);
            if (entry == null) continue;
            ServerWorld world = findWorld(server, entry.dimension());
            if (world == null) {
                removeInternal(entry);
                changed = true;
                continue;
            }
            BlockPos pos = BlockPos.fromLong(entry.packedPos());
            if (!world.isChunkLoaded(pos)) continue;
            if (!(world.getBlockEntity(pos) instanceof StaticDecorationBlockEntity decoration)
                    || decoration.owner() == null) {
                removeInternal(entry);
                changed = true;
                continue;
            }
            if (!decoration.owner().equals(entry.owner())) {
                removeInternal(entry);
                addInternal(new Entry(entry.dimension(), entry.packedPos(), decoration.owner()));
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
        for (Entry entry : java.util.List.copyOf(entries.values())) {
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

    private void addInternal(Entry entry) {
        String key = entryKey(entry.dimension(), entry.packedPos());
        entries.put(key, entry);
        keysByOwner.computeIfAbsent(entry.owner(), ignored -> new HashSet<>()).add(key);
        keysByChunk.computeIfAbsent(
                chunkKey(entry.dimension(), BlockPos.fromLong(entry.packedPos())),
                ignored -> new HashSet<>()
        ).add(key);
    }

    private void removeInternal(Entry entry) {
        String key = entryKey(entry.dimension(), entry.packedPos());
        entries.remove(key);
        removeFromIndex(keysByOwner, entry.owner(), key);
        removeFromIndex(
                keysByChunk,
                chunkKey(entry.dimension(), BlockPos.fromLong(entry.packedPos())),
                key
        );
    }

    private static <K> void removeFromIndex(Map<K, Set<String>> index, K group, String key) {
        Set<String> keys = index.get(group);
        if (keys == null) return;
        keys.remove(key);
        if (keys.isEmpty()) index.remove(group);
    }

    private static int size(Set<?> values) {
        return values == null ? 0 : values.size();
    }

    private static String dimensionId(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static String entryKey(World world, BlockPos pos) {
        return entryKey(dimensionId(world), pos.asLong());
    }

    private static String entryKey(String dimension, long packedPos) {
        return dimension + "|" + packedPos;
    }

    private static String chunkKey(World world, BlockPos pos) {
        return chunkKey(dimensionId(world), pos);
    }

    private static String chunkKey(String dimension, BlockPos pos) {
        return dimension + "|" + (pos.getX() >> 4) + "|" + (pos.getZ() >> 4);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        nbt.putInt("Version", FORMAT_VERSION);
        NbtList list = new NbtList();
        int written = 0;
        for (Entry entry : entries.values()) {
            if (written++ >= MAX_ENTRIES) break;
            NbtCompound tag = new NbtCompound();
            tag.putString("Dimension", entry.dimension());
            tag.putLong("Position", entry.packedPos());
            tag.putUuid("Owner", entry.owner());
            list.add(tag);
        }
        nbt.put("Entries", list);
        return nbt;
    }

    private static StaticDecorationState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        StaticDecorationState state = new StaticDecorationState();
        NbtList list = nbt.getList("Entries", NbtElement.COMPOUND_TYPE);
        int limit = Math.min(list.size(), MAX_ENTRIES);
        for (int i = 0; i < limit; i++) {
            NbtCompound tag = list.getCompound(i);
            if (!tag.containsUuid("Owner")) continue;
            String dimension = tag.getString("Dimension");
            if (dimension.isBlank() || dimension.length() > 256
                    || Identifier.tryParse(dimension) == null) continue;
            Entry entry = new Entry(dimension, tag.getLong("Position"), tag.getUuid("Owner"));
            if (!state.entries.containsKey(entryKey(entry.dimension(), entry.packedPos()))) {
                state.addInternal(entry);
            }
        }
        return state;
    }

    private record Entry(String dimension, long packedPos, UUID owner) {}
}
