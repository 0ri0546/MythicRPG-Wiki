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
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * World-global persistent ownership and cooldown state for Fossil Drills.
 *
 * The state is intentionally stored in the Overworld so one player can only
 * own one active drill across every dimension.
 */
public final class FossilDrillState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_fossil_drills";
    private static final Type<FossilDrillState> TYPE = new Type<>(
            FossilDrillState::new,
            FossilDrillState::fromNbt,
            null
    );

    private final Map<UUID, ActiveDrill> activeDrills = new HashMap<>();
    private final Map<UUID, Long> cooldownUntilMillis = new HashMap<>();

    public static FossilDrillState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }
        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public Optional<ActiveDrill> active(UUID owner) {
        return Optional.ofNullable(activeDrills.get(owner));
    }

    public boolean claim(UUID owner, RegistryKey<World> dimension, BlockPos pos) {
        ActiveDrill existing = activeDrills.get(owner);
        ActiveDrill requested = new ActiveDrill(dimension, pos.toImmutable());
        if (existing != null && !existing.equals(requested)) {
            return false;
        }
        if (existing == null) {
            activeDrills.put(owner, requested);
            markDirty();
        }
        return true;
    }

    public void release(UUID owner, RegistryKey<World> dimension, BlockPos pos) {
        ActiveDrill existing = activeDrills.get(owner);
        if (existing != null
                && existing.dimension().equals(dimension)
                && existing.pos().equals(pos)) {
            activeDrills.remove(owner);
            markDirty();
        }
    }

    public void forceRelease(UUID owner) {
        if (activeDrills.remove(owner) != null) {
            markDirty();
        }
    }

    public void setCooldownUntil(UUID owner, long untilMillis) {
        if (untilMillis <= 0L) {
            if (cooldownUntilMillis.remove(owner) != null) {
                markDirty();
            }
            return;
        }
        Long previous = cooldownUntilMillis.put(owner, untilMillis);
        if (previous == null || previous != untilMillis) {
            markDirty();
        }
    }

    public long cooldownRemainingMillis(UUID owner, long nowMillis) {
        Long until = cooldownUntilMillis.get(owner);
        if (until == null) {
            return 0L;
        }
        long remaining = until - nowMillis;
        if (remaining <= 0L) {
            cooldownUntilMillis.remove(owner);
            markDirty();
            return 0L;
        }
        return remaining;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList activeList = new NbtList();
        for (Map.Entry<UUID, ActiveDrill> entry : activeDrills.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Owner", entry.getKey());
            tag.putString("Dimension", entry.getValue().dimension().getValue().toString());
            tag.putLong("Pos", entry.getValue().pos().asLong());
            activeList.add(tag);
        }
        nbt.put("Active", activeList);

        NbtList cooldownList = new NbtList();
        for (Map.Entry<UUID, Long> entry : cooldownUntilMillis.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Owner", entry.getKey());
            tag.putLong("UntilMillis", entry.getValue());
            cooldownList.add(tag);
        }
        nbt.put("Cooldowns", cooldownList);
        return nbt;
    }

    private static FossilDrillState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        FossilDrillState state = new FossilDrillState();

        NbtList activeList = nbt.getList("Active", NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < activeList.size(); index++) {
            NbtCompound tag = activeList.getCompound(index);
            try {
                UUID owner = tag.getUuid("Owner");
                Identifier dimensionId = Identifier.of(tag.getString("Dimension"));
                RegistryKey<World> dimension = RegistryKey.of(RegistryKeys.WORLD, dimensionId);
                state.activeDrills.put(owner, new ActiveDrill(
                        dimension,
                        BlockPos.fromLong(tag.getLong("Pos"))
                ));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed legacy or manually edited entries.
            }
        }

        long now = System.currentTimeMillis();
        NbtList cooldownList = nbt.getList("Cooldowns", NbtElement.COMPOUND_TYPE);
        for (int index = 0; index < cooldownList.size(); index++) {
            NbtCompound tag = cooldownList.getCompound(index);
            try {
                UUID owner = tag.getUuid("Owner");
                long until = tag.getLong("UntilMillis");
                if (until > now) {
                    state.cooldownUntilMillis.put(owner, until);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUIDs.
            }
        }
        return state;
    }

    public record ActiveDrill(RegistryKey<World> dimension, BlockPos pos) {
        public ActiveDrill {
            pos = pos.toImmutable();
        }
    }
}
