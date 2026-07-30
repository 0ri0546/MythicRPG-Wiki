package com.mythicrpg.eating;

import com.mythicrpg.core.ModItems;
import com.mythicrpg.fishing.FishingCatchData;
import com.mythicrpg.fishing.FishingFamily;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;

import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CulinaryIngredientRegistry {
    private static final Map<Item, CulinaryIngredient> INGREDIENTS = new IdentityHashMap<>();

    static {
        register(Items.CARROT, 1, VEGETABLE());
        register(Items.POTATO, 1, VEGETABLE());
        register(Items.BAKED_POTATO, 2, VEGETABLE());
        register(Items.POISONOUS_POTATO, 2, VEGETABLE(), TOXIC());
        register(Items.BEETROOT, 1, VEGETABLE());
        register(Items.PUMPKIN, 1, VEGETABLE());
        register(Items.GOLDEN_CARROT, 3, VEGETABLE(), PRECIOUS(), MAGICAL());

        register(Items.APPLE, 1, FRUIT(), SWEET());
        register(Items.GOLDEN_APPLE, 4, FRUIT(), SWEET(), PRECIOUS(), MAGICAL());
        register(Items.ENCHANTED_GOLDEN_APPLE, 5, FRUIT(), SWEET(), PRECIOUS(), MAGICAL());
        register(Items.MELON_SLICE, 1, FRUIT(), SWEET());
        register(Items.SWEET_BERRIES, 1, FRUIT(), SWEET());
        register(Items.GLOW_BERRIES, 3, FRUIT(), SWEET(), UNDERGROUND(), MAGICAL());
        register(Items.CHORUS_FRUIT, 3, FRUIT(), END(), MAGICAL());

        register(Items.WHEAT, 1, GRAIN(), THICKENER());
        register(Items.BREAD, 2, GRAIN());
        register(Items.COOKIE, 2, GRAIN(), SWEET());
        register(Items.SUGAR, 1, SWEET(), SPICE());
        register(Items.COCOA_BEANS, 2, SWEET(), SPICE());
        register(Items.HONEY_BOTTLE, 2, SWEET(), LIQUID());
        register(Items.EGG, 1, EGG(), THICKENER());
        register(Items.MILK_BUCKET, 2, DAIRY(), LIQUID());

        register(Items.RED_MUSHROOM, 1, MUSHROOM());
        register(Items.BROWN_MUSHROOM, 1, MUSHROOM());
        register(Items.WARPED_FUNGUS, 3, MUSHROOM(), NETHER(), MAGICAL());
        register(Items.NETHER_WART, 3, SPICE(), NETHER(), MAGICAL());
        register(Items.BLAZE_POWDER, 4, SPICE(), NETHER(), MAGICAL());
        register(Items.MAGMA_CREAM, 4, NETHER(), MAGICAL(), MONSTROUS());

        register(Items.BEEF, 1, MEAT());
        register(Items.COOKED_BEEF, 2, MEAT(), SALTY());
        register(Items.CHICKEN, 1, MEAT());
        register(Items.COOKED_CHICKEN, 2, MEAT(), SALTY());
        register(Items.PORKCHOP, 1, MEAT());
        register(Items.COOKED_PORKCHOP, 2, MEAT(), SALTY());
        register(Items.RABBIT, 2, MEAT());
        register(Items.COOKED_RABBIT, 3, MEAT(), SALTY());
        register(Items.MUTTON, 1, MEAT());
        register(Items.COOKED_MUTTON, 2, MEAT(), SALTY());
        register(Items.ROTTEN_FLESH, 1, MEAT(), MONSTROUS(), TOXIC());
        register(Items.SPIDER_EYE, 2, MONSTROUS(), TOXIC());

        register(Items.COD, 1, FISH(), OCEAN());
        register(Items.COOKED_COD, 2, FISH(), OCEAN(), SALTY());
        register(Items.SALMON, 1, FISH(), OCEAN());
        register(Items.COOKED_SALMON, 2, FISH(), OCEAN(), SALTY());
        register(Items.TROPICAL_FISH, 2, FISH(), OCEAN());
        register(Items.PUFFERFISH, 3, FISH(), OCEAN(), TOXIC());
        register(Items.KELP, 1, VEGETABLE(), OCEAN());
        register(Items.DRIED_KELP, 1, VEGETABLE(), OCEAN(), SALTY());

        // POTION is accepted only when its contents are exactly vanilla water.
        register(Items.POTION, 1, LIQUID());
    }

    private CulinaryIngredientRegistry() {
    }

    public static Optional<CulinaryIngredient> get(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        if (stack.isOf(Items.POTION)) {
            PotionContentsComponent contents = stack.getOrDefault(
                    DataComponentTypes.POTION_CONTENTS,
                    PotionContentsComponent.DEFAULT
            );
            if (!contents.matches(Potions.WATER)) {
                return Optional.empty();
            }
        }
        if (stack.isOf(ModItems.FISHING_CATCH)) {
            return FishingCatchData.read(stack).map(catchData -> {
                EnumSet<FoodCategory> categories = EnumSet.of(FoodCategory.FISH);
                switch (catchData.family()) {
                    case INFERNAL -> categories.addAll(Set.of(FoodCategory.NETHER, FoodCategory.MAGICAL));
                    case VOID -> categories.addAll(Set.of(FoodCategory.END, FoodCategory.MAGICAL));
                    default -> categories.add(FoodCategory.OCEAN);
                }
                return new CulinaryIngredient(catchData.rarity().rank() + 1, categories);
            });
        }
        return Optional.ofNullable(INGREDIENTS.get(stack.getItem()));
    }

    public static boolean isCulinaryIngredient(ItemStack stack) {
        return get(stack).isPresent();
    }

    public static boolean isRegisteredItem(Item item) {
        return INGREDIENTS.containsKey(item);
    }

    private static void register(Item item, int score, Set<FoodCategory>... categorySets) {
        EnumSet<FoodCategory> categories = EnumSet.noneOf(FoodCategory.class);
        for (Set<FoodCategory> set : categorySets) {
            categories.addAll(set);
        }
        INGREDIENTS.put(item, new CulinaryIngredient(score, categories));
    }

    private static Set<FoodCategory> MEAT() { return Set.of(FoodCategory.MEAT); }
    private static Set<FoodCategory> FISH() { return Set.of(FoodCategory.FISH); }
    private static Set<FoodCategory> VEGETABLE() { return Set.of(FoodCategory.VEGETABLE); }
    private static Set<FoodCategory> FRUIT() { return Set.of(FoodCategory.FRUIT); }
    private static Set<FoodCategory> GRAIN() { return Set.of(FoodCategory.GRAIN); }
    private static Set<FoodCategory> MUSHROOM() { return Set.of(FoodCategory.MUSHROOM); }
    private static Set<FoodCategory> DAIRY() { return Set.of(FoodCategory.DAIRY); }
    private static Set<FoodCategory> EGG() { return Set.of(FoodCategory.EGG); }
    private static Set<FoodCategory> SWEET() { return Set.of(FoodCategory.SWEET); }
    private static Set<FoodCategory> SALTY() { return Set.of(FoodCategory.SALTY); }
    private static Set<FoodCategory> SPICE() { return Set.of(FoodCategory.SPICE); }
    private static Set<FoodCategory> LIQUID() { return Set.of(FoodCategory.LIQUID); }
    private static Set<FoodCategory> THICKENER() { return Set.of(FoodCategory.THICKENER); }
    private static Set<FoodCategory> NETHER() { return Set.of(FoodCategory.NETHER); }
    private static Set<FoodCategory> END() { return Set.of(FoodCategory.END); }
    private static Set<FoodCategory> OCEAN() { return Set.of(FoodCategory.OCEAN); }
    private static Set<FoodCategory> UNDERGROUND() { return Set.of(FoodCategory.UNDERGROUND); }
    private static Set<FoodCategory> MAGICAL() { return Set.of(FoodCategory.MAGICAL); }
    private static Set<FoodCategory> MONSTROUS() { return Set.of(FoodCategory.MONSTROUS); }
    private static Set<FoodCategory> PRECIOUS() { return Set.of(FoodCategory.PRECIOUS); }
    private static Set<FoodCategory> TOXIC() { return Set.of(FoodCategory.TOXIC); }
}
