package com.mythicrpg.crafting.station;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

public final class CraftingTableDurabilityItemData {

    private static final String DURABILITY_KEY = "MythicCraftingTableDurability";

    private CraftingTableDurabilityItemData() {
    }

    public static boolean hasDurability(ItemStack stack) {
        if (!isCraftingTable(stack)) {
            return false;
        }

        NbtCompound nbt = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();

        return nbt.contains(DURABILITY_KEY);
    }

    public static int getDurabilityOrDefault(ItemStack stack) {
        if (!isCraftingTable(stack)) {
            return CraftingTableDurabilityState.MAX_DURABILITY;
        }

        NbtCompound nbt = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();

        if (!nbt.contains(DURABILITY_KEY)) {
            return CraftingTableDurabilityState.MAX_DURABILITY;
        }

        return clamp(nbt.getInt(DURABILITY_KEY));
    }

    public static void setDurability(ItemStack stack, int durability) {
        if (!isCraftingTable(stack) || stack.isEmpty()) {
            return;
        }

        int clamped = clamp(durability);
        NbtComponent customData = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        );

        NbtCompound nbt = customData.copyNbt();

        if (clamped >= CraftingTableDurabilityState.MAX_DURABILITY) {
            nbt.remove(DURABILITY_KEY);
        } else {
            nbt.putInt(DURABILITY_KEY, clamped);
        }

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    public static ItemStack createStackWithDurability(int durability) {
        ItemStack stack = new ItemStack(Items.CRAFTING_TABLE);
        setDurability(stack, durability);
        return stack;
    }

    private static boolean isCraftingTable(ItemStack stack) {
        return !stack.isEmpty() && stack.isOf(Items.CRAFTING_TABLE);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(CraftingTableDurabilityState.MAX_DURABILITY, value));
    }
}
