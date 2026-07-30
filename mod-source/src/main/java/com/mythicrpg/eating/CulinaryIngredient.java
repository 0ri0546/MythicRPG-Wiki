package com.mythicrpg.eating;

import java.util.Set;

public record CulinaryIngredient(int score, Set<FoodCategory> categories) {
    public CulinaryIngredient {
        score = Math.max(1, Math.min(5, score));
        categories = Set.copyOf(categories);
    }

    public boolean has(FoodCategory category) {
        return categories.contains(category);
    }
}
