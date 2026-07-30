package com.mythicrpg.eating;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public record CookingRecipe(
        String id,
        DishCategory category,
        DishRarity baseRarity,
        int shelfLifeDays,
        List<IngredientRequirement> ingredients,
        boolean improvised
) {
    public CookingRecipe {
        ingredients = List.copyOf(ingredients);
        shelfLifeDays = Math.max(1, shelfLifeDays);
    }

    public String translationKey() {
        return "dish.mythicrpg." + id;
    }

    public Text displayName() {
        return Text.translatable(translationKey());
    }

    public List<String> categoryHints() {
        return ingredients.stream().map(IngredientRequirement::hintId).toList();
    }

    public record IngredientRequirement(Item item, String hintId) {
        public boolean matches(ItemStack stack) {
            if (item == net.minecraft.item.Items.POTION) {
                return CulinaryIngredientRegistry.isCulinaryIngredient(stack) && stack.isOf(item);
            }
            return stack.isOf(item);
        }
    }

    public static IngredientRequirement ingredient(Item item, FoodCategory hint) {
        return new IngredientRequirement(item, hint.id());
    }

    public static IngredientRequirement ingredient(Item item, String hintId) {
        return new IngredientRequirement(item, hintId);
    }
}
