package com.mythicrpg.crafting;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class LuckyBlockLuckManager {

    private static final String LUCK_KEY = "LuckyBlockLuck";
    public static final int MIN_LUCK = -10;
    public static final int MAX_LUCK = 10;

    private LuckyBlockLuckManager() {
    }

    public static int getLuck(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        NbtComponent customData = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        );

        NbtCompound nbt = customData.copyNbt();

        if (!nbt.contains(LUCK_KEY)) {
            return 0;
        }

        return clamp(nbt.getInt(LUCK_KEY));
    }

    public static void setLuck(ItemStack stack, int luck) {
        if (stack.isEmpty()) {
            return;
        }

        NbtComponent customData = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        );

        NbtCompound nbt = customData.copyNbt();
        nbt.putInt(LUCK_KEY, clamp(luck));

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static int clamp(int luck) {
        return Math.max(MIN_LUCK, Math.min(MAX_LUCK, luck));
    }
}