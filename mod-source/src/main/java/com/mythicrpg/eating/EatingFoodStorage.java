package com.mythicrpg.eating;

import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class EatingFoodStorage {
    private EatingFoodStorage() {
    }

    public static boolean isFood(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // The FOOD data component is the 1.21 food contract and automatically includes
        // standard modded foods without an item-by-item whitelist. The conventional
        // placed-food tag additionally covers modded cake-like foods that are eaten as
        // blocks and therefore do not necessarily carry a FOOD component.
        return PreparedDishData.read(stack).isPresent()
                || (stack.getItem() instanceof ServingPlateItem && ServingPlateData.count(stack) > 0)
                || stack.contains(DataComponentTypes.FOOD)
                || stack.isIn(ConventionalItemTags.EDIBLE_WHEN_PLACED_FOODS)
                || stack.isOf(Items.CAKE);
    }

    public static boolean isFridgeAccepted(ItemStack stack) {
        return isFood(stack) || stack.isOf(Items.PLAYER_HEAD);
    }
}
