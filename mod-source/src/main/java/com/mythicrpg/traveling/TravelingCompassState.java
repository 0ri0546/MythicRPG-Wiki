package com.mythicrpg.traveling;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TravelingCompassState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_traveling_compass";

    private static final Type<TravelingCompassState> TYPE = new Type<>(
            TravelingCompassState::new,
            TravelingCompassState::fromNbt,
            null
    );

    private final Map<UUID, String> moduleByPlayer = new HashMap<>();

    public static TravelingCompassState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);

        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }

        return overworld.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public String getModuleId(UUID playerUuid) {
        return moduleByPlayer.getOrDefault(playerUuid, "");
    }

    public List<UUID> getStoredPlayerUuidsSnapshot() {
        return List.copyOf(moduleByPlayer.keySet());
    }

    public void setModuleId(UUID playerUuid, String moduleId) {
        String normalized = StructureModuleRegistry.get(moduleId).isPresent() ? moduleId : "";
        String previous = moduleByPlayer.getOrDefault(playerUuid, "");

        if (normalized.equals(previous)) {
            return;
        }

        if (normalized.isEmpty()) {
            moduleByPlayer.remove(playerUuid);
        } else {
            moduleByPlayer.put(playerUuid, normalized);
        }

        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList modules = new NbtList();

        for (Map.Entry<UUID, String> entry : moduleByPlayer.entrySet()) {
            NbtCompound moduleTag = new NbtCompound();
            moduleTag.putString("uuid", entry.getKey().toString());
            moduleTag.putString("module", entry.getValue());
            modules.add(moduleTag);
        }

        nbt.put("modules", modules);
        return nbt;
    }

    private static TravelingCompassState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        TravelingCompassState state = new TravelingCompassState();
        NbtList modules = nbt.getList("modules", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < modules.size(); i++) {
            NbtCompound moduleTag = modules.getCompound(i);

            try {
                UUID playerUuid = UUID.fromString(moduleTag.getString("uuid"));
                String moduleId = moduleTag.getString("module");

                if (StructureModuleRegistry.get(moduleId).isPresent()) {
                    state.moduleByPlayer.put(playerUuid, moduleId);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed entries instead of preventing world load.
            }
        }

        return state;
    }
}
