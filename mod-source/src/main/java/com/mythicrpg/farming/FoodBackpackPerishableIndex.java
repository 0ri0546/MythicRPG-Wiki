package com.mythicrpg.farming;

import com.mythicrpg.core.ItemContainerUtils;
import com.mythicrpg.eating.PreparedDishData;
import com.mythicrpg.eating.ServingPlateData;
import com.mythicrpg.eating.ServingPlateItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/** Cached hint used to skip empty/non-perishable Food Backpack scans. */
public final class FoodBackpackPerishableIndex {
    private static final String KEY = "MythicFoodBackpackHasPerishables";

    private FoodBackpackPerishableIndex() {
    }

    public static boolean hasPerishables(ItemStack backpack, int size) {
        NbtCompound data = backpack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (data.contains(KEY)) {
            return data.getBoolean(KEY);
        }
        boolean result = ItemContainerUtils.read(backpack, size).stream().anyMatch(FoodBackpackPerishableIndex::isPerishable);
        write(backpack, result);
        return result;
    }

    public static void refresh(ItemStack backpack, int size) {
        boolean result = ItemContainerUtils.read(backpack, size).stream().anyMatch(FoodBackpackPerishableIndex::isPerishable);
        write(backpack, result);
    }

    public static void refresh(ItemStack backpack, Inventory inventory) {
        boolean result = false;
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (isPerishable(inventory.getStack(slot))) {
                result = true;
                break;
            }
        }
        write(backpack, result);
    }

    private static boolean isPerishable(ItemStack stack) {
        if (PreparedDishData.read(stack).filter(dish -> !dish.dubious()).isPresent()) {
            return true;
        }
        return stack.getItem() instanceof ServingPlateItem && ServingPlateData.contents(stack).stream()
                .anyMatch(portion -> PreparedDishData.read(portion).filter(dish -> !dish.dubious()).isPresent());
    }

    private static void write(ItemStack backpack, boolean value) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, backpack, nbt -> nbt.putBoolean(KEY, value));
    }
}
