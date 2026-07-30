package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
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

public final class RecycleCraftManager {

    private static final String LOCKED_RECYCLE_FEEDBACK_COOLDOWN = "locked_recycle_craft";

    private RecycleCraftManager() {
    }

    public static boolean isRecycleRecipe(RecipeInputInventory input, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }

        ItemStack onlyInput = getOnlyInput(input);

        if (onlyInput.isEmpty()) {
            return false;
        }

        Item inputItem = onlyInput.getItem();
        Item resultItem = result.getItem();

        return isWoodenTool(inputItem, resultItem)
                || isStoneTool(inputItem, resultItem)
                || isIronToolOrArmor(inputItem, resultItem)
                || isGoldToolOrArmor(inputItem, resultItem);
    }

    public static boolean canTakeRecycleResult(ServerPlayerEntity player, RecipeInputInventory input, ItemStack result) {
        if (!isRecycleRecipe(input, result)) {
            return true;
        }

        if (SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.RECYCLE_CRAFTS
        )) {
            return true;
        }

        sendLockedFeedback(player);
        return false;
    }

    private static ItemStack getOnlyInput(RecipeInputInventory input) {
        ItemStack found = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (!found.isEmpty()) {
                return ItemStack.EMPTY;
            }

            found = stack;
        }

        return found;
    }

    private static boolean isWoodenTool(Item inputItem, Item resultItem) {
        return resultItem == Items.STICK && (
                inputItem == Items.WOODEN_SWORD
                        || inputItem == Items.WOODEN_PICKAXE
                        || inputItem == Items.WOODEN_AXE
                        || inputItem == Items.WOODEN_SHOVEL
                        || inputItem == Items.WOODEN_HOE
        );
    }

    private static boolean isStoneTool(Item inputItem, Item resultItem) {
        return resultItem == Items.COBBLESTONE && (
                inputItem == Items.STONE_SWORD
                        || inputItem == Items.STONE_PICKAXE
                        || inputItem == Items.STONE_AXE
                        || inputItem == Items.STONE_SHOVEL
                        || inputItem == Items.STONE_HOE
        );
    }

    private static boolean isIronToolOrArmor(Item inputItem, Item resultItem) {
        return resultItem == Items.IRON_NUGGET && (
                inputItem == Items.IRON_SWORD
                        || inputItem == Items.IRON_PICKAXE
                        || inputItem == Items.IRON_AXE
                        || inputItem == Items.IRON_SHOVEL
                        || inputItem == Items.IRON_HOE
                        || inputItem == Items.IRON_HELMET
                        || inputItem == Items.IRON_CHESTPLATE
                        || inputItem == Items.IRON_LEGGINGS
                        || inputItem == Items.IRON_BOOTS
        );
    }

    private static boolean isGoldToolOrArmor(Item inputItem, Item resultItem) {
        return resultItem == Items.GOLD_NUGGET && (
                inputItem == Items.GOLDEN_SWORD
                        || inputItem == Items.GOLDEN_PICKAXE
                        || inputItem == Items.GOLDEN_AXE
                        || inputItem == Items.GOLDEN_SHOVEL
                        || inputItem == Items.GOLDEN_HOE
                        || inputItem == Items.GOLDEN_HELMET
                        || inputItem == Items.GOLDEN_CHESTPLATE
                        || inputItem == Items.GOLDEN_LEGGINGS
                        || inputItem == Items.GOLDEN_BOOTS
        );
    }

    private static void sendLockedFeedback(ServerPlayerEntity player) {
        if (!PlayerCooldownManager.tryUse(player, LOCKED_RECYCLE_FEEDBACK_COOLDOWN, 20)) {
            return;
        }

        player.sendMessage(
                Text.translatable("message.mythicrpg.perk_required", Text.translatable("skill_tree.mythicrpg.crafting.15.name"))
                        .formatted(Formatting.RED),
                true
        );
    }
}