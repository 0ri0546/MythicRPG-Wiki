package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class CraftXpManager {

    private static final int MAX_XP_PER_CRAFT_ACTION = 80;
    private static final double CRAFT_XP_MULTIPLIER = 0.08;

    private static final double MIDNIGHT_WORKSHOP_MULTIPLIER = 1.3;

    private static final double GREEN_CRAFTING_VANILLA_XP_RATIO = 0.05;
    private static final int MAX_GREEN_CRAFTING_VANILLA_XP = 5;

    private CraftXpManager() {
    }

    public static void handleCraft(
            ServerPlayerEntity player,
            RecipeInputInventory input,
            ItemStack resultPerCraft,
            int craftedTimes
    ) {
        if (resultPerCraft.isEmpty()) {
            return;
        }

        int craftXp = calculateCraftXpForCraftCount(input, resultPerCraft, craftedTimes);

        if (craftXp <= 0) {
            return;
        }

        craftXp = applyMidnightWorkshop(player, craftXp);
        craftXp = MythicInspirationManager.applyIfReady(player, resultPerCraft, craftXp);
        craftXp = FirstCraftBonusManager.applyFirstCraftBonus(player, resultPerCraft, craftXp);

        grantCraftXp(player, craftXp);
        tryGrantGreenCraftingXp(player, craftXp);
        CraftChargeManager.handleCraftCharge(player, craftXp);
        CraftMasteryManager.handleCraftMastery(player, resultPerCraft, craftXp);

        MythicInspirationManager.tryGrantFromCraft(player, resultPerCraft);
    }

    public static int calculateCraftXpForCraftCount(
            RecipeInputInventory input,
            ItemStack resultPerCraft,
            int craftedTimes
    ) {
        if (resultPerCraft.isEmpty()) {
            return 0;
        }

        int scorePerCraft = CraftScoreManager.getCraftScore(input, resultPerCraft);

        if (scorePerCraft <= 0) {
            return 0;
        }

        int safeCraftedTimes = Math.max(1, craftedTimes);
        int totalScore = scorePerCraft * safeCraftedTimes;

        return scoreToXp(totalScore);
    }

    public static void grantCraftXp(ServerPlayerEntity player, int xp) {
        if (xp <= 0) {
            return;
        }

        SkillXpManager.addXp(player, SkillType.CRAFTING, xp, false);
    }

    private static int applyMidnightWorkshop(ServerPlayerEntity player, int xp) {
        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.MIDNIGHT_CRAFTING
        )) {
            return xp;
        }

        if (!isNight(player)) {
            return xp;
        }

        return Math.max(1, (int) Math.floor(xp * MIDNIGHT_WORKSHOP_MULTIPLIER));
    }

    private static void tryGrantGreenCraftingXp(ServerPlayerEntity player, int craftXp) {
        if (!SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.CRAFT_VANILLA_XP
        )) {
            return;
        }

        if (craftXp <= 0) {
            return;
        }

        int vanillaXp = Math.max(
                1,
                (int) Math.floor(craftXp * GREEN_CRAFTING_VANILLA_XP_RATIO)
        );

        vanillaXp = Math.min(MAX_GREEN_CRAFTING_VANILLA_XP, vanillaXp);

        player.addExperience(vanillaXp);
    }

    private static int scoreToXp(int score) {
        if (score <= 0) {
            return 0;
        }

        int xp = (int) Math.floor(score * CRAFT_XP_MULTIPLIER);

        return Math.min(MAX_XP_PER_CRAFT_ACTION, Math.max(1, xp));
    }

    private static boolean isNight(ServerPlayerEntity player) {
        long time = player.getWorld().getTimeOfDay() % 24000L;
        return time >= 13000L && time <= 23000L;
    }
}