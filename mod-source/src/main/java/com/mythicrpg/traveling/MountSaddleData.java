package com.mythicrpg.traveling;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Persistent history carried by an adoption saddle between successive mounts. */
public final class MountSaddleData {
    private static final String DISTANCE_KEY = "MythicRpgMountDistance";

    private MountSaddleData() {
    }

    public static double getDistance(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }

        NbtCompound nbt = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();

        return Math.max(0.0D, nbt.getDouble(DISTANCE_KEY));
    }

    public static void setDistance(ItemStack stack, double distance) {
        if (stack.isEmpty()) {
            return;
        }

        NbtCompound nbt = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();

        nbt.putDouble(DISTANCE_KEY, Math.max(0.0D, distance));
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }
}
