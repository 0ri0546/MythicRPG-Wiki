package com.mythicrpg.crafting;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PortableCraftingState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_portable_crafting";
    public static final int MAX_DURABILITY = 256;

    private static final Type<PortableCraftingState> TYPE = new Type<>(
            PortableCraftingState::new,
            PortableCraftingState::fromNbt,
            null
    );

    private final Map<UUID, Integer> durabilityByPlayer = new HashMap<>();

    public static PortableCraftingState get(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);

        if (world == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }

        return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public int getDurability(UUID playerUuid) {
        return durabilityByPlayer.getOrDefault(playerUuid, MAX_DURABILITY);
    }

    public void setDurability(UUID playerUuid, int durability) {
        durabilityByPlayer.put(playerUuid, clamp(durability));
        markDirty();
    }

    public boolean consumeCharge(UUID playerUuid) {
        int durability = getDurability(playerUuid);

        if (durability <= 0) {
            return false;
        }

        setDurability(playerUuid, durability - 1);
        return true;
    }

    public int repair(UUID playerUuid, int amount) {
        int oldDurability = getDurability(playerUuid);
        int newDurability = clamp(oldDurability + amount);

        setDurability(playerUuid, newDurability);

        return newDurability - oldDurability;
    }

    public void resetPlayer(UUID playerUuid) {
        durabilityByPlayer.remove(playerUuid);
        markDirty();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_DURABILITY, value));
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound playersTag = new NbtCompound();

        for (Map.Entry<UUID, Integer> entry : durabilityByPlayer.entrySet()) {
            playersTag.putInt(entry.getKey().toString(), entry.getValue());
        }

        nbt.put("players", playersTag);
        return nbt;
    }

    private static PortableCraftingState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        PortableCraftingState state = new PortableCraftingState();

        NbtCompound playersTag = nbt.getCompound("players");

        for (String key : playersTag.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                state.durabilityByPlayer.put(uuid, clamp(playersTag.getInt(key)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved data.
            }
        }

        return state;
    }
}