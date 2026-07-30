package com.mythicrpg.eating;

public record CookingResult(
        CookingRecipe recipe,
        DishRarity rarity,
        int portions,
        int score,
        boolean dubious
) {
}
