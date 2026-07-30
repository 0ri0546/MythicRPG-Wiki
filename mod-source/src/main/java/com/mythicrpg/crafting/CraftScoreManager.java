package com.mythicrpg.crafting;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public final class CraftScoreManager {

    private static final Map<Item, Integer> ITEM_POINTS = new HashMap<>();

    static {
        // Common resources
        value(Items.COBBLESTONE, 1);
        value(Items.STONE, 1);
        value(Items.DEEPSLATE, 1);
        value(Items.DIRT, 1);
        value(Items.SAND, 1);
        value(Items.GRAVEL, 1);
        value(Items.GLASS, 2);

        // Wood / basic crafting
        value(Items.OAK_LOG, 2);
        value(Items.SPRUCE_LOG, 2);
        value(Items.BIRCH_LOG, 2);
        value(Items.JUNGLE_LOG, 2);
        value(Items.ACACIA_LOG, 2);
        value(Items.DARK_OAK_LOG, 2);
        value(Items.MANGROVE_LOG, 2);
        value(Items.CHERRY_LOG, 2);
        value(Items.CRIMSON_STEM, 3);
        value(Items.WARPED_STEM, 3);

        value(Items.STICK, 0);
        value(Items.OAK_PLANKS, 1);
        value(Items.SPRUCE_PLANKS, 1);
        value(Items.BIRCH_PLANKS, 1);
        value(Items.JUNGLE_PLANKS, 1);
        value(Items.ACACIA_PLANKS, 1);
        value(Items.DARK_OAK_PLANKS, 1);
        value(Items.MANGROVE_PLANKS, 1);
        value(Items.CHERRY_PLANKS, 1);
        value(Items.CRIMSON_PLANKS, 1);
        value(Items.WARPED_PLANKS, 1);

        // Early materials
        value(Items.COAL, 3);
        value(Items.CHARCOAL, 2);
        value(Items.COPPER_INGOT, 4);
        value(Items.IRON_INGOT, 8);
        value(Items.GOLD_INGOT, 8);
        value(Items.REDSTONE, 4);
        value(Items.LAPIS_LAZULI, 5);
        value(Items.QUARTZ, 5);

        // Better materials
        value(Items.AMETHYST_SHARD, 8);
        value(Items.ENDER_PEARL, 15);
        value(Items.BLAZE_ROD, 18);
        value(Items.DIAMOND, 35);
        value(Items.EMERALD, 35);

        // Late game
        value(Items.NETHERITE_SCRAP, 80);
        value(Items.NETHERITE_INGOT, 250);
        value(Items.NETHER_STAR, 500);
        value(Items.DRAGON_BREATH, 150);
        value(Items.ECHO_SHARD, 120);

        // Blocks / expensive resources
        value(Items.COAL_BLOCK, 27);
        value(Items.COPPER_BLOCK, 36);
        value(Items.IRON_BLOCK, 72);
        value(Items.GOLD_BLOCK, 72);
        value(Items.REDSTONE_BLOCK, 36);
        value(Items.LAPIS_BLOCK, 45);
        value(Items.DIAMOND_BLOCK, 315);
        value(Items.EMERALD_BLOCK, 315);
        value(Items.NETHERITE_BLOCK, 2250);
    }

    private CraftScoreManager() {
    }

    public static int getCraftScore(RecipeInputInventory input, ItemStack result) {
        if (result.isEmpty()) {
            return 0;
        }

        if (RecycleCraftManager.isRecycleRecipe(input, result)) {
            return 0;
        }

        if (LuckyInfusionManager.isLuckyInfusionRecipe(input, result)) {
            return 0;
        }

        if (isBlockedCraftLoop(input, result)) {
            return 0;
        }

        int score = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            score += getItemPoints(stack);
        }

        return Math.max(0, score);
    }

    public static boolean isEligibleForCraftXp(RecipeInputInventory input, ItemStack result) {
        return getCraftScore(input, result) > 0;
    }

    public static int getItemPoints(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }

        Item item = stack.getItem();

        Integer customValue = ITEM_POINTS.get(item);
        if (customValue != null) {
            return customValue;
        }

        Identifier id = Registries.ITEM.getId(item);

        if ("mythicrpg".equals(id.getNamespace())) {
            return 100;
        }

        return 1;
    }

    private static boolean isBlockedCraftLoop(RecipeInputInventory input, ItemStack result) {
        Item resultItem = result.getItem();

        if (isKnownLoopResult(resultItem)
                || isDecorativeSpamResult(resultItem)
                || isDoubtfulStorageOrBuildingResult(resultItem)) {
            return true;
        }

        if (isHoneyBottleLoop(input, resultItem)) {
            return true;
        }

        if (isBambooPlanksFromBambooBlock(input, resultItem)) {
            return true;
        }

        Item onlyInputItem = getOnlyInputItem(input);

        if (onlyInputItem == null) {
            return false;
        }

        return isCompressionPair(onlyInputItem, resultItem)
                || isCompressionPair(resultItem, onlyInputItem)
                || isKnownLoopInput(onlyInputItem);
    }

    private static Item getOnlyInputItem(RecipeInputInventory input) {
        Item onlyInputItem = null;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);

            if (stack.isEmpty()) {
                continue;
            }

            if (onlyInputItem == null) {
                onlyInputItem = stack.getItem();
            } else if (onlyInputItem != stack.getItem()) {
                return null;
            }
        }

        return onlyInputItem;
    }

    private static boolean isCompressionPair(Item a, Item b) {
        return matches(a, b, Items.COAL, Items.COAL_BLOCK)
                || matches(a, b, Items.RAW_IRON, Items.RAW_IRON_BLOCK)
                || matches(a, b, Items.RAW_COPPER, Items.RAW_COPPER_BLOCK)
                || matches(a, b, Items.RAW_GOLD, Items.RAW_GOLD_BLOCK)
                || matches(a, b, Items.COPPER_INGOT, Items.COPPER_BLOCK)
                || matches(a, b, Items.IRON_INGOT, Items.IRON_BLOCK)
                || matches(a, b, Items.GOLD_INGOT, Items.GOLD_BLOCK)
                || matches(a, b, Items.IRON_NUGGET, Items.IRON_INGOT)
                || matches(a, b, Items.GOLD_NUGGET, Items.GOLD_INGOT)
                || matches(a, b, Items.REDSTONE, Items.REDSTONE_BLOCK)
                || matches(a, b, Items.LAPIS_LAZULI, Items.LAPIS_BLOCK)
                || matches(a, b, Items.DIAMOND, Items.DIAMOND_BLOCK)
                || matches(a, b, Items.EMERALD, Items.EMERALD_BLOCK)
                || matches(a, b, Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK)
                || matches(a, b, Items.WHEAT, Items.HAY_BLOCK)
                || matches(a, b, Items.SLIME_BALL, Items.SLIME_BLOCK)
                || matches(a, b, Items.HONEY_BOTTLE, Items.HONEY_BLOCK)
                || matches(a, b, Items.DRIED_KELP, Items.DRIED_KELP_BLOCK)
                || matches(a, b, Items.BONE, Items.BONE_BLOCK)
                || matches(a, b, Items.BONE_MEAL, Items.BONE_BLOCK)
                || matches(a, b, Items.QUARTZ, Items.QUARTZ_BLOCK)
                || matches(a, b, Items.GLOWSTONE_DUST, Items.GLOWSTONE)
                || matches(a, b, Items.AMETHYST_SHARD, Items.AMETHYST_BLOCK)
                || matches(a, b, Items.SNOWBALL, Items.SNOW_BLOCK)
                || matches(a, b, Items.CLAY_BALL, Items.CLAY)
                || matches(a, b, Items.BAMBOO, Items.BAMBOO_BLOCK)
                || matches(a, b, Items.NETHER_WART, Items.NETHER_WART_BLOCK);
    }

    private static boolean matches(Item a, Item b, Item x, Item y) {
        return a == x && b == y;
    }

    private static boolean isHoneyBottleLoop(RecipeInputInventory input, Item resultItem) {
        if (resultItem != Items.HONEY_BOTTLE) {
            return false;
        }

        return containsInput(input, Items.HONEY_BLOCK) && containsInput(input, Items.GLASS_BOTTLE);
    }

    private static boolean isBambooPlanksFromBambooBlock(RecipeInputInventory input, Item resultItem) {
        return resultItem == Items.BAMBOO_PLANKS && containsInput(input, Items.BAMBOO_BLOCK);
    }

    private static boolean containsInput(RecipeInputInventory input, Item item) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);

            if (stack.isOf(item)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isKnownLoopResult(Item resultItem) {
        return resultItem == Items.STICK
                || resultItem == Items.BONE_MEAL
                || resultItem == Items.SNOW
                || resultItem == Items.SNOW_BLOCK;
    }

    private static boolean isKnownLoopInput(Item inputItem) {
        return inputItem == Items.STICK
                || inputItem == Items.BONE_MEAL;
    }

    private static boolean isDoubtfulStorageOrBuildingResult(Item resultItem) {
        return resultItem == Items.GLOWSTONE
                || resultItem == Items.AMETHYST_BLOCK
                || resultItem == Items.PACKED_MUD
                || resultItem == Items.QUARTZ_BLOCK
                || resultItem == Items.QUARTZ_BRICKS
                || resultItem == Items.QUARTZ_PILLAR
                || resultItem == Items.CHISELED_QUARTZ_BLOCK
                || resultItem == Items.SMOOTH_QUARTZ;
    }

    private static boolean isDecorativeSpamResult(Item resultItem) {
        Identifier id = Registries.ITEM.getId(resultItem);

        if (!"minecraft".equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();

        return path.endsWith("_button")
                || path.endsWith("_pressure_plate")
                || path.endsWith("_trapdoor")
                || path.endsWith("_sign")
                || path.endsWith("_hanging_sign")
                || path.endsWith("_fence")
                || path.endsWith("_fence_gate")
                || path.endsWith("_slab")
                || path.endsWith("_stairs");
    }

    private static void value(Item item, int points) {
        ITEM_POINTS.put(item, points);
    }
}
