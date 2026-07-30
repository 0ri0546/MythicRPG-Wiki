package com.mythicrpg.building;

import net.minecraft.entity.Entity;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistent quota index for miniature entities with bounded lazy reconciliation. */
public final class BuildingMiniatureState extends PersistentState {
    public static final int MAX_PER_PLAYER = 16;
    public static final int MAX_PER_CHUNK = 4;
    private static final int FORMAT_VERSION = 2;
    private static final int MAX_ENTRIES = 262_144;

    private static final String STATE_ID = "mythicrpg_building_miniatures";
    private static final Type<BuildingMiniatureState> TYPE = new Type<>(
            BuildingMiniatureState::new,
            BuildingMiniatureState::fromNbt,
            null
    );

    private final Map<UUID, Entry> entries = new HashMap<>();
    private final Map<UUID, Set<UUID>> idsByOwner = new HashMap<>();
    private final Map<String, Set<UUID>> idsByChunk = new HashMap<>();

    public static BuildingMiniatureState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public boolean canPlace(ServerWorld world, BlockPos pos, UUID owner) {
        reconcileRelevant(world, pos, owner);
        return canPlaceIndexed(world, pos, owner);
    }

    public boolean add(UUID entityId, ServerWorld world, BlockPos pos, UUID owner) {
        reconcileRelevant(world, pos, owner);
        Entry previous = entries.get(entityId);
        if (previous != null) {
            if (!previous.owner().equals(owner)) return false;
            String dimension = dimensionId(world);
            if (!previous.dimension().equals(dimension) || previous.packedPos() != pos.asLong()) {
                removeInternal(previous);
                addInternal(new Entry(entityId, dimension, pos.asLong(), owner));
                markDirty();
            }
            return true;
        }
        if (!canPlaceIndexed(world, pos, owner)) return false;
        addInternal(new Entry(entityId, dimensionId(world), pos.asLong(), owner));
        markDirty();
        return true;
    }

    public void remove(UUID entityId) {
        Entry removed = entries.get(entityId);
        if (removed == null) return;
        removeInternal(removed);
        markDirty();
    }

    private boolean canPlaceIndexed(ServerWorld world, BlockPos pos, UUID owner) {
        return entries.size() < MAX_ENTRIES
                && size(idsByOwner.get(owner)) < MAX_PER_PLAYER
                && size(idsByChunk.get(chunkKey(world, pos))) < MAX_PER_CHUNK;
    }

    private void reconcileRelevant(ServerWorld context, BlockPos target, UUID owner) {
        Set<UUID> relevant = new HashSet<>();
        Set<UUID> ownerIds = idsByOwner.get(owner);
        if (ownerIds != null) relevant.addAll(ownerIds);
        Set<UUID> chunkIds = idsByChunk.get(chunkKey(context, target));
        if (chunkIds != null) relevant.addAll(chunkIds);
        reconcileIds(context.getServer(), relevant);
    }

    private void reconcileIds(MinecraftServer server, Set<UUID> ids) {
        boolean changed = false;
        for (UUID id : Set.copyOf(ids)) {
            Entry entry = entries.get(id);
            if (entry == null) continue;
            ServerWorld world = findWorld(server, entry.dimension());
            if (world == null) {
                removeInternal(entry);
                changed = true;
                continue;
            }

            LocatedEntity located = findEntity(server, entry.entityId());
            if (located != null && located.entity() instanceof BuildingMiniatureEntity miniature) {
                Optional<UUID> actualOwner = miniature.owner();
                if (actualOwner.isEmpty()) {
                    removeInternal(entry);
                    changed = true;
                    continue;
                }
                BlockPos actualPos = miniature.getBlockPos();
                String actualDimension = dimensionId(located.world());
                UUID owner = actualOwner.get();
                if (!entry.dimension().equals(actualDimension)
                        || entry.packedPos() != actualPos.asLong()
                        || !entry.owner().equals(owner)) {
                    removeInternal(entry);
                    addInternal(new Entry(entry.entityId(), actualDimension, actualPos.asLong(), owner));
                    changed = true;
                }
                continue;
            }

            BlockPos stored = BlockPos.fromLong(entry.packedPos());
            if (world.isChunkLoaded(stored)) {
                removeInternal(entry);
                changed = true;
            }
        }
        if (changed) markDirty();
    }


    private static LocatedEntity findEntity(MinecraftServer server, UUID entityId) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(entityId);
            if (entity != null) return new LocatedEntity(world, entity);
        }
        return null;
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
        entries.put(entry.entityId(), entry);
        idsByOwner.computeIfAbsent(entry.owner(), ignored -> new HashSet<>()).add(entry.entityId());
        idsByChunk.computeIfAbsent(
                chunkKey(entry.dimension(), BlockPos.fromLong(entry.packedPos())),
                ignored -> new HashSet<>()
        ).add(entry.entityId());
    }

    private void removeInternal(Entry entry) {
        entries.remove(entry.entityId());
        removeFromIndex(idsByOwner, entry.owner(), entry.entityId());
        removeFromIndex(
                idsByChunk,
                chunkKey(entry.dimension(), BlockPos.fromLong(entry.packedPos())),
                entry.entityId()
        );
    }

    private static <K> void removeFromIndex(Map<K, Set<UUID>> index, K group, UUID id) {
        Set<UUID> values = index.get(group);
        if (values == null) return;
        values.remove(id);
        if (values.isEmpty()) index.remove(group);
    }

    private static int size(Set<?> values) {
        return values == null ? 0 : values.size();
    }

    private static String dimensionId(World world) {
        return world.getRegistryKey().getValue().toString();
    }

    private static String chunkKey(World world, BlockPos pos) {
        return chunkKey(dimensionId(world), pos);
    }

    private static String chunkKey(String dimension, BlockPos pos) {
        return dimension + "|" + (pos.getX() >> 4) + "|" + (pos.getZ() >> 4);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        nbt.putInt("Version", FORMAT_VERSION);
        NbtList list = new NbtList();
        int written = 0;
        for (Entry entry : entries.values()) {
            if (written++ >= MAX_ENTRIES) break;
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Entity", entry.entityId());
            tag.putUuid("Owner", entry.owner());
            tag.putString("Dimension", entry.dimension());
            tag.putLong("Position", entry.packedPos());
            list.add(tag);
        }
        nbt.put("Entries", list);
        return nbt;
    }

    private static BuildingMiniatureState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        BuildingMiniatureState state = new BuildingMiniatureState();
        NbtList list = nbt.getList("Entries", NbtElement.COMPOUND_TYPE);
        int limit = Math.min(list.size(), MAX_ENTRIES);
        for (int i = 0; i < limit; i++) {
            NbtCompound tag = list.getCompound(i);
            if (!tag.containsUuid("Entity") || !tag.containsUuid("Owner")) continue;
            String dimension = tag.getString("Dimension");
            if (dimension.isBlank() || dimension.length() > 256
                    || Identifier.tryParse(dimension) == null) continue;
            Entry entry = new Entry(
                    tag.getUuid("Entity"),
                    dimension,
                    tag.getLong("Position"),
                    tag.getUuid("Owner")
            );
            if (!state.entries.containsKey(entry.entityId())) state.addInternal(entry);
        }
        return state;
    }

    private record LocatedEntity(ServerWorld world, Entity entity) {}

    private record Entry(UUID entityId, String dimension, long packedPos, UUID owner) {}
}
