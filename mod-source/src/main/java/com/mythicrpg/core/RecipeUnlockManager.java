package com.mythicrpg.core;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class RecipeUnlockManager {

    private RecipeUnlockManager() {
    }

    public static boolean canCraft(ServerPlayerEntity player, ItemStack result) {
        return LockedRecipeRegistry.canCraft(player, result);
    }
}
