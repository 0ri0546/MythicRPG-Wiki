package com.mythicrpg.fishing;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Persistent one-net-per-player index. It never loads a chunk to validate an entry. */
public final class FishingNetState extends PersistentState {
    private static final String STATE_ID = "mythicrpg_fishing_nets";
    private static final int MAX_ENTRIES = 100_000;

    private static final Type<FishingNetState> TYPE = new Type<>(
            FishingNetState::new,
            FishingNetState::fromNbt,
            null
    );

    private final Map<UUID, NetLocation> byOwner = new HashMap<>();

    public static FishingNetState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public boolean claim(ServerWorld world, BlockPos pos, UUID owner) {
        if (owner == null) {
            return false;
        }

        NetLocation requested = NetLocation.of(world, pos);
        NetLocation current = byOwner.get(owner);
        if (requested.equals(current)) {
            return true;
        }
        if (current != null && isStillReserved(world.getServer(), owner, current)) {
            return false;
        }
        if (current == null && byOwner.size() >= MAX_ENTRIES) {
            return false;
        }

        byOwner.put(owner, requested);
        markDirty();
        return true;
    }

    public boolean ensureActive(ServerWorld world, BlockPos pos, UUID owner) {
        if (owner == null) {
            return false;
        }

        NetLocation requested = NetLocation.of(world, pos);
        NetLocation current = byOwner.get(owner);
        if (requested.equals(current)) {
            return true;
        }
        if (current != null && isStillReserved(world.getServer(), owner, current)) {
            return false;
        }
        if (current == null && byOwner.size() >= MAX_ENTRIES) {
            return false;
        }

        byOwner.put(owner, requested);
        markDirty();
        return true;
    }

    public void release(ServerWorld world, BlockPos pos, UUID owner) {
        if (owner == null) {
            return;
        }
        NetLocation requested = NetLocation.of(world, pos);
        if (requested.equals(byOwner.get(owner))) {
            byOwner.remove(owner);
            markDirty();
        }
    }

    public void pruneMissingDimensions(MinecraftServer server) {
        boolean changed = byOwner.entrySet().removeIf(entry ->
                findWorld(server, entry.getValue().dimension()) == null
        );
        if (changed) {
            markDirty();
        }
    }

    private static boolean isStillReserved(
            MinecraftServer server,
            UUID owner,
            NetLocation location
    ) {
        ServerWorld world = findWorld(server, location.dimension());
        if (world == null) {
            return false;
        }

        BlockPos pos = BlockPos.fromLong(location.packedPos());
        if (!world.isChunkLoaded(pos)) {
            // An unloaded entry remains reserved. Validation must never force the chunk.
            return true;
        }
        return world.getBlockEntity(pos) instanceof FishNetBlockEntity net
                && owner.equals(net.owner());
    }

    private static ServerWorld findWorld(MinecraftServer server, String dimension) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(dimension)) {
                return world;
            }
        }
        return null;
    }

    @Override
    public NbtCompound writeNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        NbtList entries = new NbtList();
        for (Map.Entry<UUID, NetLocation> entry : byOwner.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("owner", entry.getKey());
            tag.putString("dimension", entry.getValue().dimension());
            tag.putLong("pos", entry.getValue().packedPos());
            entries.add(tag);
        }
        nbt.put("entries", entries);
        return nbt;
    }

    private static FishingNetState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        FishingNetState state = new FishingNetState();
        NbtList entries = nbt.getList("entries", NbtElement.COMPOUND_TYPE);
        int limit = Math.min(MAX_ENTRIES, entries.size());
        for (int index = 0; index < limit; index++) {
            NbtCompound tag = entries.getCompound(index);
            if (!tag.containsUuid("owner")) {
                continue;
            }
            String dimension = tag.getString("dimension");
            if (dimension.isBlank() || dimension.length() > 128) {
                continue;
            }
            state.byOwner.put(
                    tag.getUuid("owner"),
                    new NetLocation(dimension, tag.getLong("pos"))
            );
        }
        return state;
    }

    private record NetLocation(String dimension, long packedPos) {
        private static NetLocation of(ServerWorld world, BlockPos pos) {
            return new NetLocation(
                    world.getRegistryKey().getValue().toString(),
                    pos.asLong()
            );
        }
    }
}
