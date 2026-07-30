package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;

public final class TransformationSlotManager {

    private static final Map<Item, Item> TRANSFORMATIONS = createTransformations();

    public static int transformMainHand(ServerPlayerEntity player) {
        return transformMainHand(player, 1);
    }

    private TransformationSlotManager() {
    }

    public static int transformMainHand(ServerPlayerEntity player, int requestedAmount) {
        if (!hasTransformationSlot(player)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.perk_required", Text.translatable("skill_tree.mythicrpg.crafting.19.name"))
                            .formatted(Formatting.RED),
                    true
            );
            return 0;
        }

        ItemStack inputStack = player.getMainHandStack();
        int amount = Math.max(1, requestedAmount);

        if (inputStack.isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.transformation.hold_item")
                            .formatted(Formatting.RED),
                    true
            );
            return 0;
        }

        Item outputItem = getOutputItem(inputStack.getItem());

        if (outputItem == null) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.transformation.invalid_item")
                            .formatted(Formatting.RED),
                    true
            );
            return 0;
        }

        int transformAmount = Math.min(amount, inputStack.getCount());

        if (!PortableCraftingManager.tryConsumeCharges(player, transformAmount)) {
            return 0;
        }

        ItemStack outputStack = new ItemStack(outputItem, transformAmount);

        consumeAndGiveResult(player, inputStack, outputStack, transformAmount);

        PortableCraftingManager.sendDurability(player);

        player.sendMessage(
                Text.translatable("message.mythicrpg.transformation.complete", transformAmount)
                        .formatted(Formatting.LIGHT_PURPLE),
                true
        );

        return 1;
    }

    public static boolean isTransformable(ItemStack stack) {
        return !stack.isEmpty() && getOutputItem(stack.getItem()) != null;
    }

    private static void consumeAndGiveResult(
            ServerPlayerEntity player,
            ItemStack inputStack,
            ItemStack outputStack,
            int amount
    ) {
        if (inputStack.getCount() <= amount) {
            player.setStackInHand(Hand.MAIN_HAND, outputStack);
            return;
        }

        inputStack.decrement(amount);

        if (!player.getInventory().insertStack(outputStack)) {
            player.dropItem(outputStack, false);
        }
    }

    public static boolean hasTransformationSlot(ServerPlayerEntity player) {
        return SkillTreeManager.hasBonus(
                player,
                SkillType.CRAFTING,
                BonusType.TRANSFORMATION_SLOT
        );
    }

    public static Item getOutputItem(Item input) {
        return TRANSFORMATIONS.get(input);
    }

    private static Map<Item, Item> createTransformations() {
        Map<Item, Item> map = new HashMap<>();

        addLogTransformations(map);
        addCopperTransformations(map);
        addConcreteTransformations(map);

        map.put(Items.MUD, Items.CLAY);
        map.put(Items.ROTTEN_FLESH, Items.LEATHER);

        map.put(Items.SAND, Items.GLASS);
        map.put(Items.RED_SAND, Items.GLASS);

        map.put(Items.COBBLESTONE, Items.STONE);
        map.put(Items.STONE, Items.SMOOTH_STONE);

        return map;
    }

    private static void addLogTransformations(Map<Item, Item> map) {
        map.put(Items.OAK_LOG, Items.STRIPPED_OAK_LOG);
        map.put(Items.SPRUCE_LOG, Items.STRIPPED_SPRUCE_LOG);
        map.put(Items.BIRCH_LOG, Items.STRIPPED_BIRCH_LOG);
        map.put(Items.JUNGLE_LOG, Items.STRIPPED_JUNGLE_LOG);
        map.put(Items.ACACIA_LOG, Items.STRIPPED_ACACIA_LOG);
        map.put(Items.DARK_OAK_LOG, Items.STRIPPED_DARK_OAK_LOG);
        map.put(Items.MANGROVE_LOG, Items.STRIPPED_MANGROVE_LOG);
        map.put(Items.CHERRY_LOG, Items.STRIPPED_CHERRY_LOG);

        map.put(Items.OAK_WOOD, Items.STRIPPED_OAK_WOOD);
        map.put(Items.SPRUCE_WOOD, Items.STRIPPED_SPRUCE_WOOD);
        map.put(Items.BIRCH_WOOD, Items.STRIPPED_BIRCH_WOOD);
        map.put(Items.JUNGLE_WOOD, Items.STRIPPED_JUNGLE_WOOD);
        map.put(Items.ACACIA_WOOD, Items.STRIPPED_ACACIA_WOOD);
        map.put(Items.DARK_OAK_WOOD, Items.STRIPPED_DARK_OAK_WOOD);
        map.put(Items.MANGROVE_WOOD, Items.STRIPPED_MANGROVE_WOOD);
        map.put(Items.CHERRY_WOOD, Items.STRIPPED_CHERRY_WOOD);

        map.put(Items.CRIMSON_STEM, Items.STRIPPED_CRIMSON_STEM);
        map.put(Items.WARPED_STEM, Items.STRIPPED_WARPED_STEM);
        map.put(Items.CRIMSON_HYPHAE, Items.STRIPPED_CRIMSON_HYPHAE);
        map.put(Items.WARPED_HYPHAE, Items.STRIPPED_WARPED_HYPHAE);
    }

    private static void addCopperTransformations(Map<Item, Item> map) {
        map.put(Items.COPPER_BLOCK, Items.EXPOSED_COPPER);
        map.put(Items.EXPOSED_COPPER, Items.WEATHERED_COPPER);
        map.put(Items.WEATHERED_COPPER, Items.OXIDIZED_COPPER);

        map.put(Items.CUT_COPPER, Items.EXPOSED_CUT_COPPER);
        map.put(Items.EXPOSED_CUT_COPPER, Items.WEATHERED_CUT_COPPER);
        map.put(Items.WEATHERED_CUT_COPPER, Items.OXIDIZED_CUT_COPPER);
    }

    private static void addConcreteTransformations(Map<Item, Item> map) {
        map.put(Items.WHITE_CONCRETE_POWDER, Items.WHITE_CONCRETE);
        map.put(Items.LIGHT_GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_CONCRETE);
        map.put(Items.GRAY_CONCRETE_POWDER, Items.GRAY_CONCRETE);
        map.put(Items.BLACK_CONCRETE_POWDER, Items.BLACK_CONCRETE);
        map.put(Items.BROWN_CONCRETE_POWDER, Items.BROWN_CONCRETE);
        map.put(Items.RED_CONCRETE_POWDER, Items.RED_CONCRETE);
        map.put(Items.ORANGE_CONCRETE_POWDER, Items.ORANGE_CONCRETE);
        map.put(Items.YELLOW_CONCRETE_POWDER, Items.YELLOW_CONCRETE);
        map.put(Items.LIME_CONCRETE_POWDER, Items.LIME_CONCRETE);
        map.put(Items.GREEN_CONCRETE_POWDER, Items.GREEN_CONCRETE);
        map.put(Items.CYAN_CONCRETE_POWDER, Items.CYAN_CONCRETE);
        map.put(Items.LIGHT_BLUE_CONCRETE_POWDER, Items.LIGHT_BLUE_CONCRETE);
        map.put(Items.BLUE_CONCRETE_POWDER, Items.BLUE_CONCRETE);
        map.put(Items.PURPLE_CONCRETE_POWDER, Items.PURPLE_CONCRETE);
        map.put(Items.MAGENTA_CONCRETE_POWDER, Items.MAGENTA_CONCRETE);
        map.put(Items.PINK_CONCRETE_POWDER, Items.PINK_CONCRETE);
    }
}