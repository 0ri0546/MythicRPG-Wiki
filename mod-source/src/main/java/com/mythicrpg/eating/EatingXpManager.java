package com.mythicrpg.eating;

import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public final class EatingXpManager {
    private EatingXpManager() {
    }

    public static void awardDiscovery(ServerPlayerEntity player) {
        SkillXpManager.addXp(player, SkillType.EATING, EatingBalance.DISCOVERY_XP);
    }

    public static void awardDish(ServerPlayerEntity player, PreparedDishData.Dish dish) {
        if (!dish.dubious()) {
            SkillXpManager.addXp(player, SkillType.EATING, EatingBalance.CUSTOM_DISH_XP);
        }
    }

    public static void awardVanillaFood(ServerPlayerEntity player, ItemStack consumedStack) {
        int xp = isVanillaSoup(consumedStack)
                ? EatingBalance.VANILLA_SOUP_XP
                : EatingBalance.VANILLA_FOOD_XP;

        SkillXpManager.addXp(player, SkillType.EATING, xp);
    }

    public static void awardCakeSlice(ServerPlayerEntity player) {
        SkillXpManager.addXp(player, SkillType.EATING, EatingBalance.CAKE_SLICE_XP);
    }

    public static boolean isVanillaSoup(ItemStack stack) {
        return stack.isOf(Items.MUSHROOM_STEW)
                || stack.isOf(Items.RABBIT_STEW)
                || stack.isOf(Items.BEETROOT_SOUP)
                || stack.isOf(Items.SUSPICIOUS_STEW);
    }
}
