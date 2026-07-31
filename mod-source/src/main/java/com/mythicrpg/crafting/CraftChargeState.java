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

public class CraftChargeState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_craft_charge";

    private static final Type<CraftChargeState> TYPE = new Type<>(
            CraftChargeState::new,
            CraftChargeState::fromNbt,
            null
    );

    private final Map<UUID, Double> chargeByPlayer = new HashMap<>();

    public static CraftChargeState get(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);

        if (world == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }

        return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public double getCharge(UUID playerUuid) {
        return chargeByPlayer.getOrDefault(playerUuid, 0.0);
    }

    public void setCharge(UUID playerUuid, double charge) {
        double clamped = Math.max(0.0, Math.min(100.0, charge));
        chargeByPlayer.put(playerUuid, clamped);
        markDirty();
    }

    public void clearPlayer(UUID playerUuid) {
        chargeByPlayer.remove(playerUuid);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound playersTag = new NbtCompound();

        for (Map.Entry<UUID, Double> entry : chargeByPlayer.entrySet()) {
            playersTag.putDouble(entry.getKey().toString(), entry.getValue());
        }

        nbt.put("players", playersTag);
        return nbt;
    }

    private static CraftChargeState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        CraftChargeState state = new CraftChargeState();

        NbtCompound playersTag = nbt.getCompound("players");

        for (String key : playersTag.getKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                double charge = playersTag.getDouble(key);
                state.chargeByPlayer.put(uuid, Math.max(0.0, Math.min(100.0, charge)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved data.
            }
        }

        return state;
    }
}