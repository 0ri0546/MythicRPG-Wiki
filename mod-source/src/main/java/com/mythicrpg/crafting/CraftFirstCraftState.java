package com.mythicrpg.crafting;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CraftFirstCraftState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_first_crafts";

    private static final Type<CraftFirstCraftState> TYPE = new Type<>(
            CraftFirstCraftState::new,
            CraftFirstCraftState::fromNbt,
            null
    );

    private final Map<UUID, Set<String>> craftedItemsByPlayer = new HashMap<>();

    public static CraftFirstCraftState get(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);

        if (world == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }

        return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public boolean markCrafted(UUID playerUuid, Identifier itemId) {
        Set<String> craftedItems = craftedItemsByPlayer.computeIfAbsent(
                playerUuid,
                uuid -> new HashSet<>()
        );

        boolean added = craftedItems.add(itemId.toString());

        if (added) {
            markDirty();
        }

        return added;
    }

    public boolean hasCrafted(UUID playerUuid, Identifier itemId) {
        Set<String> craftedItems = craftedItemsByPlayer.get(playerUuid);
        return craftedItems != null && craftedItems.contains(itemId.toString());
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtList playersList = new NbtList();

        for (Map.Entry<UUID, Set<String>> entry : craftedItemsByPlayer.entrySet()) {
            NbtCompound playerTag = new NbtCompound();
            playerTag.putString("uuid", entry.getKey().toString());

            NbtList itemsList = new NbtList();

            for (String itemId : entry.getValue()) {
                itemsList.add(NbtString.of(itemId));
            }

            playerTag.put("items", itemsList);
            playersList.add(playerTag);
        }

        nbt.put("players", playersList);
        return nbt;
    }

    private static CraftFirstCraftState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        CraftFirstCraftState state = new CraftFirstCraftState();

        NbtList playersList = nbt.getList("players", NbtElement.COMPOUND_TYPE);

        for (int i = 0; i < playersList.size(); i++) {
            NbtCompound playerTag = playersList.getCompound(i);

            try {
                UUID playerUuid = UUID.fromString(playerTag.getString("uuid"));
                NbtList itemsList = playerTag.getList("items", NbtElement.STRING_TYPE);

                Set<String> craftedItems = new HashSet<>();

                for (int j = 0; j < itemsList.size(); j++) {
                    craftedItems.add(itemsList.getString(j));
                }

                state.craftedItemsByPlayer.put(playerUuid, craftedItems);
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed saved data instead of crashing the world load.
            }
        }

        return state;
    }

    public void clearPlayer(UUID playerUuid) {
        craftedItemsByPlayer.remove(playerUuid);
        markDirty();
    }
}