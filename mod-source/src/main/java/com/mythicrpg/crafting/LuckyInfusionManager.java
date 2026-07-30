package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.core.PlayerCooldownManager;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class LuckyInfusionManager {

    private static final String LOCKED_LUCKY_INFUSION_FEEDBACK_COOLDOWN = "locked_lucky_infusion";

    private LuckyInfusionManager() {
    }

    public static boolean isLuckyInfusionRecipe(RecipeInputInventory input, ItemStack result) {
        if (result.isEmpty() || !result.isOf(ModBlocks.LUCKY_BLOCK.asItem())) {
            return false;
        }

        return getInfusionDelta(input) != 0;
    }

    public static boolean canTakeInfusionResult(ServerPlayerEntity player, RecipeInputInventory input, ItemStack result) {
        if (!isLuckyInfusionRecipe(input, result)) {
            return true;
        }

        if (SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.LUCKY_INFUSION
        )) {
            return true;
        }

        sendLockedFeedback(player);
        return false;
    }

    public static void applyToResult(ServerPlayerEntity player, RecipeInputInventory input, ItemStack result) {
        if (!isLuckyInfusionRecipe(input, result)) {
            return;
        }

        ItemStack luckyBlockInput = getLuckyBlockInput(input);

        if (luckyBlockInput.isEmpty()) {
            return;
        }

        int oldLuck = LuckyBlockLuckManager.getLuck(luckyBlockInput);
        int delta = getInfusionDelta(input);
        int newLuck = LuckyBlockLuckManager.clamp(oldLuck + delta);

        LuckyBlockLuckManager.setLuck(result, newLuck);

        player.sendMessage(
                Text.translatable("message.mythicrpg.lucky_infusion.luck", newLuck)
                        .formatted(newLuck >= 0 ? Formatting.GOLD : Formatting.DARK_PURPLE),
                true
        );
    }

    public static int getInfusionDelta(RecipeInputInventory input) {
        Item infusionItem = null;
        int luckyBlockSlots = 0;
        int infusionSlots = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            Item item = stack.getItem();

            if (item == ModBlocks.LUCKY_BLOCK.asItem()) {
                luckyBlockSlots++;
                continue;
            }

            if (infusionItem == null) {
                infusionItem = item;
            } else if (infusionItem != item) {
                return 0;
            }

            infusionSlots++;
        }

        if (luckyBlockSlots != 1 || infusionSlots != 8 || infusionItem == null) {
            return 0;
        }

        return getDeltaForItem(infusionItem);
    }

    private static ItemStack getLuckyBlockInput(RecipeInputInventory input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);

            if (stack.isOf(ModBlocks.LUCKY_BLOCK.asItem())) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static int getDeltaForItem(Item item) {
        if (item == Items.COAL_BLOCK || item == Items.COPPER_BLOCK) {
            return 1;
        }

        if (item == Items.IRON_BLOCK
                || item == Items.GOLD_BLOCK
                || item == Items.REDSTONE_BLOCK
                || item == Items.LAPIS_BLOCK) {
            return 2;
        }

        if (item == Items.DIAMOND_BLOCK || item == Items.EMERALD_BLOCK) {
            return 3;
        }

        if (item == Items.DIRT
                || item == Items.COBBLESTONE
                || item == Items.GRAVEL
                || item == Items.SAND
                || item == Items.NETHERRACK
                || item == Items.ROTTEN_FLESH
                || item == Items.POISONOUS_POTATO) {
            return -1;
        }

        if (item == Items.SPIDER_EYE
                || item == Items.FERMENTED_SPIDER_EYE
                || item == Items.GUNPOWDER
                || item == Items.MAGMA_BLOCK
                || item == Items.SOUL_SAND
                || item == Items.SOUL_SOIL
                || item == Items.PHANTOM_MEMBRANE) {
            return -2;
        }

        if (item == Items.WITHER_ROSE || item == Items.WITHER_SKELETON_SKULL) {
            return -3;
        }

        return 0;
    }

    private static void sendLockedFeedback(ServerPlayerEntity player) {
        if (!PlayerCooldownManager.tryUse(player, LOCKED_LUCKY_INFUSION_FEEDBACK_COOLDOWN, 20)) {
            return;
        }

        player.sendMessage(
                Text.translatable("message.mythicrpg.perk_required", Text.translatable("skill_tree.mythicrpg.crafting.18.name"))
                        .formatted(Formatting.RED),
                true
        );
    }
}