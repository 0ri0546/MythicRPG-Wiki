package com.mythicrpg.crafting.station;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public class CraftingTableDurabilityState extends PersistentState {

    private static final String STATE_ID = "mythicrpg_crafting_table_durability";
    public static final int MAX_DURABILITY = 256;

    private static final Type<CraftingTableDurabilityState> TYPE = new Type<>(
            CraftingTableDurabilityState::new,
            CraftingTableDurabilityState::fromNbt,
            null
    );

    private final Map<String, Integer> durabilityByTable = new HashMap<>();

    public static CraftingTableDurabilityState get(MinecraftServer server) {
        ServerWorld world = server.getWorld(World.OVERWORLD);

        if (world == null) {
            throw new IllegalStateException("Overworld is not loaded");
        }

        return world.getPersistentStateManager().getOrCreate(TYPE, STATE_ID);
    }

    public int getDurability(World world, BlockPos pos) {
        return durabilityByTable.getOrDefault(key(world, pos), MAX_DURABILITY);
    }

    public void setDurability(World world, BlockPos pos, int durability) {
        int clamped = clamp(durability);
        String key = key(world, pos);

        if (clamped >= MAX_DURABILITY) {
            durabilityByTable.remove(key);
        } else {
            durabilityByTable.put(key, clamped);
        }

        markDirty();
    }

    public boolean consume(World world, BlockPos pos, int amount) {
        if (amount <= 0) {
            return false;
        }

        int durability = getDurability(world, pos);

        if (durability < amount) {
            return false;
        }

        setDurability(world, pos, durability - amount);
        return true;
    }

    public int repair(World world, BlockPos pos, int amount) {
        if (amount <= 0) {
            return 0;
        }

        int oldDurability = getDurability(world, pos);
        int newDurability = clamp(oldDurability + amount);

        setDurability(world, pos, newDurability);
        return newDurability - oldDurability;
    }

    public void remove(World world, BlockPos pos) {
        durabilityByTable.remove(key(world, pos));
        markDirty();
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(MAX_DURABILITY, value));
    }

    private static String key(World world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "|" + pos.asLong();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound tablesTag = new NbtCompound();

        for (Map.Entry<String, Integer> entry : durabilityByTable.entrySet()) {
            tablesTag.putInt(entry.getKey(), entry.getValue());
        }

        nbt.put("tables", tablesTag);
        return nbt;
    }

    private static CraftingTableDurabilityState fromNbt(
            NbtCompound nbt,
            RegistryWrapper.WrapperLookup registryLookup
    ) {
        CraftingTableDurabilityState state = new CraftingTableDurabilityState();
        NbtCompound tablesTag = nbt.getCompound("tables");

        for (String key : tablesTag.getKeys()) {
            state.durabilityByTable.put(key, clamp(tablesTag.getInt(key)));
        }

        return state;
    }
}
