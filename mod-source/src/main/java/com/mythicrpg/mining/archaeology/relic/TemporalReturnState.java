package com.mythicrpg.mining.archaeology.relic;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persistent temporal anchors and cooldowns, stored globally in the Overworld. */
public final class TemporalReturnState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_temporal_returns";
    private static final Type<TemporalReturnState> TYPE = new Type<>(
            TemporalReturnState::new,
            TemporalReturnState::fromNbt,
            null
    );

    private final Map<UUID, PendingReturn> pending = new HashMap<>();
    private final Map<UUID, Long> cooldownUntilMillis = new HashMap<>();

    public static TemporalReturnState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public boolean hasPending(UUID playerUuid) {
        return pending.containsKey(playerUuid);
    }

    public Optional<PendingReturn> pending(UUID playerUuid) {
        return Optional.ofNullable(pending.get(playerUuid));
    }

    public List<Map.Entry<UUID, PendingReturn>> pendingEntries() {
        ArrayList<Map.Entry<UUID, PendingReturn>> snapshot = new ArrayList<>(pending.size());
        for (Map.Entry<UUID, PendingReturn> entry : pending.entrySet()) {
            snapshot.add(Map.entry(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(snapshot);
    }

    public void setPending(UUID playerUuid, PendingReturn value) {
        pending.put(playerUuid, value);
        markDirty();
    }

    public void removePending(UUID playerUuid) {
        if (pending.remove(playerUuid) != null) {
            markDirty();
        }
    }

    public void setCooldownUntil(UUID playerUuid, long untilMillis) {
        if (untilMillis <= 0L) {
            if (cooldownUntilMillis.remove(playerUuid) != null) {
                markDirty();
            }
            return;
        }
        Long previous = cooldownUntilMillis.put(playerUuid, untilMillis);
        if (previous == null || previous != untilMillis) {
            markDirty();
        }
    }

    public long cooldownRemainingMillis(UUID playerUuid, long nowMillis) {
        Long until = cooldownUntilMillis.get(playerUuid);
        if (until == null) {
            return 0L;
        }
        long remaining = until - nowMillis;
        if (remaining <= 0L) {
            cooldownUntilMillis.remove(playerUuid);
            markDirty();
            return 0L;
        }
        return remaining;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList pendingList = new NbtList();
        for (Map.Entry<UUID, PendingReturn> entry : pending.entrySet()) {
            PendingReturn value = entry.getValue();
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Player", entry.getKey());
            tag.putString("Dimension", value.dimension().getValue().toString());
            tag.putDouble("X", value.x());
            tag.putDouble("Y", value.y());
            tag.putDouble("Z", value.z());
            tag.putFloat("Yaw", value.yaw());
            tag.putFloat("Pitch", value.pitch());
            tag.putLong("ReturnAtMillis", value.returnAtMillis());
            pendingList.add(tag);
        }
        nbt.put("Pending", pendingList);

        NbtList cooldownList = new NbtList();
        for (Map.Entry<UUID, Long> entry : cooldownUntilMillis.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Player", entry.getKey());
            tag.putLong("UntilMillis", entry.getValue());
            cooldownList.add(tag);
        }
        nbt.put("Cooldowns", cooldownList);
        return nbt;
    }

    private static TemporalReturnState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        TemporalReturnState state = new TemporalReturnState();

        NbtList pendingList = nbt.getList("Pending", NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < pendingList.size(); index++) {
            NbtCompound tag = pendingList.getCompound(index);
            try {
                UUID playerUuid = tag.getUuid("Player");
                Identifier dimensionId = Identifier.of(tag.getString("Dimension"));
                RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
                state.pending.put(playerUuid, new PendingReturn(
                        dimension,
                        tag.getDouble("X"),
                        tag.getDouble("Y"),
                        tag.getDouble("Z"),
                        tag.getFloat("Yaw"),
                        tag.getFloat("Pitch"),
                        tag.getLong("ReturnAtMillis")
                ));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed entries without preventing world load.
            }
        }

        long now = System.currentTimeMillis();
        NbtList cooldownList = nbt.getList("Cooldowns", NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < cooldownList.size(); index++) {
            NbtCompound tag = cooldownList.getCompound(index);
            try {
                UUID playerUuid = tag.getUuid("Player");
                long until = tag.getLong("UntilMillis");
                if (until > now) {
                    state.cooldownUntilMillis.put(playerUuid, until);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUIDs.
            }
        }
        return state;
    }

    public record PendingReturn(
            RegistryKey<World> dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            long returnAtMillis
    ) {
    }
}
