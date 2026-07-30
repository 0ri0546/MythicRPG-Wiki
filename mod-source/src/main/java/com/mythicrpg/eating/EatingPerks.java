package com.mythicrpg.eating;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.server.network.ServerPlayerEntity;

public final class EatingPerks {
    private EatingPerks() {
    }

    public static boolean canCook(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_COOKING);
    }

    public static int maxIngredients(ServerPlayerEntity player) {
        if (!canCook(player)) {
            return 0;
        }
        int additions = (int) Math.round(SkillTreeManager.getBonusSum(
                player,
                SkillType.EATING,
                BonusType.EATING_POT_SLOTS
        ));
        return Math.max(2, Math.min(5, 2 + additions));
    }

    public static boolean canCraftSmallPlate(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_SMALL_PLATE);
    }

    public static boolean canCraftMediumPlate(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_MEDIUM_PLATE);
    }

    public static boolean canCraftLargePlate(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_LARGE_PLATE);
    }

    public static boolean canCraftFridge(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_FRIDGE);
    }

    public static boolean canEatWhenFull(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_WHEN_FULL);
    }


    public static boolean hasChefAura(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_CHEF_AURA);
    }

    public static boolean hasCompleteMeal(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_COMPLETE_MEAL);
    }

    public static boolean hasRiskTaste(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_RISK_TASTE);
    }

    public static boolean hasDelivery(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_DELIVERY);
    }

    public static boolean hasInternationalGastronomy(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_INTERNATIONAL_GASTRONOMY);
    }

    public static boolean hasChefRenown(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_CHEF_RENOWN);
    }

    public static boolean hasRarityUpgrade(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_RARITY_UP);
    }

    public static boolean hasSignatureDish(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_SIGNATURE_DISH);
    }

    public static boolean hasAutoFeed(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_AUTO_FEED);
    }

    public static boolean canCompostDubiousDish(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_DUBIOUS_COMPOST);
    }

    public static boolean hasPortableFridgePerk(ServerPlayerEntity player) {
        return has(player, BonusType.EATING_PORTABLE_FRIDGE);
    }

    private static boolean has(ServerPlayerEntity player, BonusType bonusType) {
        return SkillTreeManager.hasBonus(player, SkillType.EATING, bonusType);
    }
}
