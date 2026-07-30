package com.mythicrpg.eating;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.mythicrpg.eating.CookingRecipe.ingredient;

public final class CookingRecipeRegistry {
    private static final Map<String, CookingRecipe> BY_ID = new LinkedHashMap<>();
    private static final Map<RecipeKey, CookingRecipe> FIXED_BY_KEY = new HashMap<>();
    private static final List<CookingRecipe> FIXED = new ArrayList<>();
    private static final List<CookingRecipe> GENERIC = new ArrayList<>();

    static {
        // Entrées
        fixed("country_soup", DishCategory.STARTER, DishRarity.COMMON, 2,
                ingredient(Items.CARROT, FoodCategory.VEGETABLE),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE));
        fixed("beetroot_potage", DishCategory.STARTER, DishRarity.COMMON, 2,
                ingredient(Items.BEETROOT, FoodCategory.VEGETABLE),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE),
                ingredient(Items.WHEAT, FoodCategory.GRAIN));
        fixed("forest_veloute", DishCategory.STARTER, DishRarity.RARE, 1,
                ingredient(Items.RED_MUSHROOM, FoodCategory.MUSHROOM),
                ingredient(Items.BROWN_MUSHROOM, FoodCategory.MUSHROOM),
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY));
        fixed("pumpkin_cream", DishCategory.STARTER, DishRarity.RARE, 2,
                ingredient(Items.PUMPKIN, FoodCategory.VEGETABLE),
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY),
                ingredient(Items.SUGAR, FoodCategory.SWEET));
        fixed("chicken_broth", DishCategory.STARTER, DishRarity.RARE, 1,
                ingredient(Items.COOKED_CHICKEN, FoodCategory.MEAT),
                ingredient(Items.CARROT, FoodCategory.VEGETABLE),
                ingredient(Items.POTION, FoodCategory.LIQUID));
        fixed("fisher_soup", DishCategory.STARTER, DishRarity.RARE, 1,
                ingredient(Items.COOKED_COD, FoodCategory.FISH),
                ingredient(Items.KELP, FoodCategory.OCEAN),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE));
        fixed("golden_potage", DishCategory.STARTER, DishRarity.EPIC, 2,
                ingredient(Items.GOLDEN_CARROT, FoodCategory.PRECIOUS),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE),
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY));
        fixed("infernal_broth", DishCategory.STARTER, DishRarity.EPIC, 2,
                ingredient(Items.NETHER_WART, FoodCategory.NETHER),
                ingredient(Items.BLAZE_POWDER, FoodCategory.SPICE),
                ingredient(Items.COOKED_PORKCHOP, FoodCategory.MEAT));

        // Plats
        fixed("traveler_stew", DishCategory.MAIN, DishRarity.RARE, 1,
                ingredient(Items.COOKED_BEEF, FoodCategory.MEAT),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE),
                ingredient(Items.CARROT, FoodCategory.VEGETABLE),
                ingredient(Items.BROWN_MUSHROOM, FoodCategory.MUSHROOM));
        fixed("forest_chicken", DishCategory.MAIN, DishRarity.RARE, 1,
                ingredient(Items.COOKED_CHICKEN, FoodCategory.MEAT),
                ingredient(Items.BROWN_MUSHROOM, FoodCategory.MUSHROOM),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE));
        fixed("caramelized_pork", DishCategory.MAIN, DishRarity.RARE, 2,
                ingredient(Items.COOKED_PORKCHOP, FoodCategory.MEAT),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET),
                ingredient(Items.APPLE, FoodCategory.FRUIT));
        fixed("simmered_rabbit", DishCategory.MAIN, DishRarity.EPIC, 1,
                ingredient(Items.COOKED_RABBIT, FoodCategory.MEAT),
                ingredient(Items.CARROT, FoodCategory.VEGETABLE),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE),
                ingredient(Items.RED_MUSHROOM, FoodCategory.MUSHROOM));
        fixed("fish_and_vegetables", DishCategory.MAIN, DishRarity.RARE, 1,
                ingredient(Items.COOKED_SALMON, FoodCategory.FISH),
                ingredient(Items.CARROT, FoodCategory.VEGETABLE),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE));
        fixed("farmer_feast", DishCategory.MAIN, DishRarity.RARE, 3,
                ingredient(Items.BREAD, FoodCategory.GRAIN),
                ingredient(Items.CARROT, FoodCategory.VEGETABLE),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE),
                ingredient(Items.BEETROOT, FoodCategory.VEGETABLE));
        fixed("deep_skillet", DishCategory.MAIN, DishRarity.EPIC, 2,
                ingredient(Items.GLOW_BERRIES, FoodCategory.UNDERGROUND),
                ingredient(Items.BROWN_MUSHROOM, FoodCategory.MUSHROOM),
                ingredient(Items.BAKED_POTATO, FoodCategory.VEGETABLE));
        fixed("monstrous_stew", DishCategory.MAIN, DishRarity.EPIC, 1,
                ingredient(Items.ROTTEN_FLESH, FoodCategory.MONSTROUS),
                ingredient(Items.SPIDER_EYE, FoodCategory.TOXIC),
                ingredient(Items.RED_MUSHROOM, FoodCategory.MUSHROOM),
                ingredient(Items.POTATO, FoodCategory.VEGETABLE));
        fixed("infernal_simmer", DishCategory.MAIN, DishRarity.LEGENDARY, 2,
                ingredient(Items.COOKED_PORKCHOP, FoodCategory.MEAT),
                ingredient(Items.NETHER_WART, FoodCategory.NETHER),
                ingredient(Items.WARPED_FUNGUS, FoodCategory.NETHER),
                ingredient(Items.BLAZE_POWDER, FoodCategory.SPICE));
        fixed("chorus_meal", DishCategory.MAIN, DishRarity.LEGENDARY, 2,
                ingredient(Items.CHORUS_FRUIT, FoodCategory.END),
                ingredient(Items.COOKED_RABBIT, FoodCategory.MEAT),
                ingredient(Items.BEETROOT, FoodCategory.VEGETABLE),
                ingredient(Items.BROWN_MUSHROOM, FoodCategory.MUSHROOM));
        fixed("golden_banquet", DishCategory.MAIN, DishRarity.LEGENDARY, 4,
                ingredient(Items.GOLDEN_CARROT, FoodCategory.PRECIOUS),
                ingredient(Items.GOLDEN_APPLE, FoodCategory.PRECIOUS),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET),
                ingredient(Items.COOKED_BEEF, FoodCategory.MEAT));
        fixed("mythic_feast", DishCategory.MAIN, DishRarity.MYTHIC, 7,
                ingredient(Items.ENCHANTED_GOLDEN_APPLE, FoodCategory.PRECIOUS),
                ingredient(Items.GOLDEN_CARROT, FoodCategory.PRECIOUS),
                ingredient(Items.CHORUS_FRUIT, FoodCategory.END),
                ingredient(Items.MAGMA_CREAM, FoodCategory.NETHER),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET));

        // Desserts
        fixed("apple_compote", DishCategory.DESSERT, DishRarity.COMMON, 4,
                ingredient(Items.APPLE, FoodCategory.FRUIT),
                ingredient(Items.SUGAR, FoodCategory.SWEET));
        fixed("honey_carrots", DishCategory.DESSERT, DishRarity.COMMON, 4,
                ingredient(Items.CARROT, FoodCategory.VEGETABLE),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET));
        fixed("cocoa_cream", DishCategory.DESSERT, DishRarity.RARE, 2,
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY),
                ingredient(Items.COCOA_BEANS, FoodCategory.SWEET),
                ingredient(Items.SUGAR, FoodCategory.SWEET));
        fixed("berry_pudding", DishCategory.DESSERT, DishRarity.RARE, 3,
                ingredient(Items.SWEET_BERRIES, FoodCategory.FRUIT),
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY),
                ingredient(Items.SUGAR, FoodCategory.SWEET),
                ingredient(Items.WHEAT, FoodCategory.GRAIN));
        fixed("melon_sweet", DishCategory.DESSERT, DishRarity.RARE, 5,
                ingredient(Items.MELON_SLICE, FoodCategory.FRUIT),
                ingredient(Items.SUGAR, FoodCategory.SWEET),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET));
        fixed("pumpkin_flan", DishCategory.DESSERT, DishRarity.EPIC, 3,
                ingredient(Items.PUMPKIN, FoodCategory.VEGETABLE),
                ingredient(Items.EGG, FoodCategory.EGG),
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY),
                ingredient(Items.SUGAR, FoodCategory.SWEET));
        fixed("chorus_cream", DishCategory.DESSERT, DishRarity.EPIC, 3,
                ingredient(Items.CHORUS_FRUIT, FoodCategory.END),
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY),
                ingredient(Items.SUGAR, FoodCategory.SWEET));
        fixed("luminous_delight", DishCategory.DESSERT, DishRarity.EPIC, 3,
                ingredient(Items.GLOW_BERRIES, FoodCategory.UNDERGROUND),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET),
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY));
        fixed("golden_sweet", DishCategory.DESSERT, DishRarity.LEGENDARY, 6,
                ingredient(Items.GOLDEN_APPLE, FoodCategory.PRECIOUS),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET),
                ingredient(Items.SUGAR, FoodCategory.SWEET));
        fixed("chef_cake", DishCategory.DESSERT, DishRarity.LEGENDARY, 5,
                ingredient(Items.WHEAT, FoodCategory.GRAIN),
                ingredient(Items.EGG, FoodCategory.EGG),
                ingredient(Items.MILK_BUCKET, FoodCategory.DAIRY),
                ingredient(Items.SUGAR, FoodCategory.SWEET),
                ingredient(Items.COCOA_BEANS, FoodCategory.SWEET));

        // Boissons. Le chocolat chaud utilise de l'eau plutôt que du lait afin de ne pas
        // entrer en conflit avec la combinaison exacte de la Crème au cacao.
        fixed("hot_chocolate", DishCategory.DRINK, DishRarity.RARE, 2,
                ingredient(Items.POTION, FoodCategory.LIQUID),
                ingredient(Items.COCOA_BEANS, FoodCategory.SWEET),
                ingredient(Items.SUGAR, FoodCategory.SWEET));
        fixed("honey_infusion", DishCategory.DRINK, DishRarity.COMMON, 4,
                ingredient(Items.POTION, FoodCategory.LIQUID),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET));
        fixed("luminous_infusion", DishCategory.DRINK, DishRarity.EPIC, 4,
                ingredient(Items.POTION, FoodCategory.LIQUID),
                ingredient(Items.GLOW_BERRIES, FoodCategory.UNDERGROUND),
                ingredient(Items.HONEY_BOTTLE, FoodCategory.SWEET));
        fixed("chorus_nectar", DishCategory.DRINK, DishRarity.EPIC, 4,
                ingredient(Items.POTION, FoodCategory.LIQUID),
                ingredient(Items.CHORUS_FRUIT, FoodCategory.END),
                ingredient(Items.SUGAR, FoodCategory.SWEET));
        fixed("infernal_tonic", DishCategory.DRINK, DishRarity.LEGENDARY, 5,
                ingredient(Items.POTION, FoodCategory.LIQUID),
                ingredient(Items.NETHER_WART, FoodCategory.NETHER),
                ingredient(Items.BLAZE_POWDER, FoodCategory.SPICE));

        generic("meat_stew", DishCategory.MAIN, List.of("meat", "vegetable"));
        generic("fish_soup", DishCategory.STARTER, List.of("fish", "vegetable"));
        generic("vegetable_potage", DishCategory.STARTER, List.of("vegetable", "vegetable"));
        generic("mushroom_veloute", DishCategory.STARTER, List.of("mushroom", "mushroom"));
        generic("fruit_compote", DishCategory.DESSERT, List.of("fruit", "sweet"));
        generic("sweet_cream", DishCategory.DESSERT, List.of("dairy", "sweet"));
        generic("grain_porridge", DishCategory.MAIN, List.of("grain", "dairy"));
        generic("infernal_preparation", DishCategory.MAIN, List.of("nether", "food"));
        generic("chorus_preparation", DishCategory.MAIN, List.of("end", "food"));
        generic("experimental_dish", DishCategory.MAIN, List.of("varied", "ingredients"));
        generic("signature_dish", DishCategory.MAIN, List.of("signature", "ingredients"));
        generic("dubious_dish", DishCategory.MAIN, List.of("incoherent", "ingredients"));
    }

    private CookingRecipeRegistry() {
    }

    public static Optional<CookingRecipe> byId(String id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static List<CookingRecipe> fixedRecipes() {
        return Collections.unmodifiableList(FIXED);
    }

    public static List<CookingRecipe> allCodexRecipes() {
        ArrayList<CookingRecipe> result = new ArrayList<>(FIXED.size() + GENERIC.size());
        result.addAll(FIXED);
        result.addAll(GENERIC);
        return List.copyOf(result);
    }

    public static Optional<CookingResult> resolve(List<ItemStack> rawStacks) {
        List<ItemStack> stacks = rawStacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        if (stacks.size() < 2 || stacks.size() > 5) {
            return Optional.empty();
        }
        for (ItemStack stack : stacks) {
            if (!CulinaryIngredientRegistry.isCulinaryIngredient(stack)) {
                return Optional.empty();
            }
        }

        CookingRecipe fixed = FIXED_BY_KEY.get(RecipeKey.of(stacks));
        if (fixed != null && matches(fixed, stacks)) {
            int score = stacks.stream()
                    .map(CulinaryIngredientRegistry::get)
                    .flatMap(Optional::stream)
                    .mapToInt(CulinaryIngredient::score)
                    .sum();
            return Optional.of(new CookingResult(fixed, fixed.baseRarity(), stacks.size(), score, false));
        }

        return Optional.of(resolveImprovised(stacks));
    }

    /**
     * Resolves a player's signature dish independently from fixed and improvised recipes.
     * Every 2-5 item combination accepted by the culinary registry is valid at creation time.
     */
    public static Optional<CookingResult> resolveSignature(List<ItemStack> rawStacks) {
        List<ItemStack> stacks = rawStacks.stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
        if (stacks.size() < 2 || stacks.size() > 5) {
            return Optional.empty();
        }

        EnumSet<FoodCategory> categories = EnumSet.noneOf(FoodCategory.class);
        int score = 0;
        int primaryCategories = 0;
        boolean seasoning = false;
        for (ItemStack stack : stacks) {
            Optional<CulinaryIngredient> optional = CulinaryIngredientRegistry.get(stack);
            if (optional.isEmpty()) {
                return Optional.empty();
            }
            CulinaryIngredient ingredient = optional.get();
            score += ingredient.score();
            categories.addAll(ingredient.categories());
            if (ingredient.has(FoodCategory.SPICE)) {
                seasoning = true;
            }
        }

        for (FoodCategory primary : List.of(
                FoodCategory.MEAT, FoodCategory.FISH, FoodCategory.VEGETABLE,
                FoodCategory.FRUIT, FoodCategory.GRAIN, FoodCategory.MUSHROOM,
                FoodCategory.DAIRY, FoodCategory.EGG
        )) {
            if (categories.contains(primary)) {
                primaryCategories++;
            }
        }
        if (primaryCategories >= 2) {
            score += Math.min(2, primaryCategories - 1);
        }
        if (seasoning) {
            score++;
        }
        score = Math.max(2, score);

        DishCategory category;
        boolean hasMeatOrFish = categories.contains(FoodCategory.MEAT)
                || categories.contains(FoodCategory.FISH);
        boolean dessertBase = categories.contains(FoodCategory.FRUIT)
                || categories.contains(FoodCategory.DAIRY)
                || categories.contains(FoodCategory.GRAIN);
        if (categories.contains(FoodCategory.LIQUID) && primaryCategories == 0) {
            category = DishCategory.DRINK;
        } else if (!hasMeatOrFish && categories.contains(FoodCategory.SWEET) && dessertBase) {
            category = DishCategory.DESSERT;
        } else if (!hasMeatOrFish && (categories.contains(FoodCategory.VEGETABLE)
                || categories.contains(FoodCategory.MUSHROOM))) {
            category = DishCategory.STARTER;
        } else {
            category = DishCategory.MAIN;
        }

        DishRarity rarity = DishRarity.fromScore(score);
        int shelfLife = shelfLifeFor(category, categories);
        CookingRecipe base = BY_ID.get("signature_dish");
        CookingRecipe recipe = new CookingRecipe(
                base.id(),
                category,
                rarity,
                shelfLife,
                base.ingredients(),
                true
        );
        return Optional.of(new CookingResult(recipe, rarity, stacks.size(), score, false));
    }

    private static CookingResult resolveImprovised(List<ItemStack> stacks) {
        EnumSet<FoodCategory> all = EnumSet.noneOf(FoodCategory.class);
        int score = 0;
        int meat = 0;
        int fish = 0;
        int vegetables = 0;
        int fruit = 0;
        int grain = 0;
        int mushrooms = 0;
        int dairy = 0;
        int sweet = 0;
        int toxic = 0;
        int baseIngredients = 0;
        boolean seasoning = false;

        for (ItemStack stack : stacks) {
            CulinaryIngredient ingredient = CulinaryIngredientRegistry.get(stack).orElseThrow();
            score += ingredient.score();
            all.addAll(ingredient.categories());
            if (ingredient.has(FoodCategory.MEAT)) meat++;
            if (ingredient.has(FoodCategory.FISH)) fish++;
            if (ingredient.has(FoodCategory.VEGETABLE)) vegetables++;
            if (ingredient.has(FoodCategory.FRUIT)) fruit++;
            if (ingredient.has(FoodCategory.GRAIN)) grain++;
            if (ingredient.has(FoodCategory.MUSHROOM)) mushrooms++;
            if (ingredient.has(FoodCategory.DAIRY)) dairy++;
            if (ingredient.has(FoodCategory.SWEET)) sweet++;
            if (ingredient.has(FoodCategory.TOXIC)) toxic++;
            if (ingredient.has(FoodCategory.SPICE)) seasoning = true;
            if (ingredient.has(FoodCategory.MEAT)
                    || ingredient.has(FoodCategory.FISH)
                    || ingredient.has(FoodCategory.VEGETABLE)
                    || ingredient.has(FoodCategory.FRUIT)
                    || ingredient.has(FoodCategory.GRAIN)
                    || ingredient.has(FoodCategory.MUSHROOM)
                    || ingredient.has(FoodCategory.DAIRY)
                    || ingredient.has(FoodCategory.EGG)) {
                baseIngredients++;
            }
        }

        String id;
        DishCategory category;
        boolean coherent = true;
        boolean dubious = toxic >= 2 || baseIngredients == 0 || (all.contains(FoodCategory.LIQUID) && baseIngredients == 0);

        if (dubious) {
            id = "dubious_dish";
            category = DishCategory.MAIN;
        } else if (all.contains(FoodCategory.NETHER)) {
            id = "infernal_preparation";
            category = DishCategory.MAIN;
        } else if (all.contains(FoodCategory.END)) {
            id = "chorus_preparation";
            category = DishCategory.MAIN;
        } else if (meat > 0 && vegetables > 0) {
            id = "meat_stew";
            category = DishCategory.MAIN;
        } else if (fish > 0 && vegetables > 0) {
            id = "fish_soup";
            category = DishCategory.STARTER;
        } else if (vegetables >= 2) {
            id = "vegetable_potage";
            category = DishCategory.STARTER;
        } else if (mushrooms >= 2 || (mushrooms > 0 && dairy > 0)) {
            id = "mushroom_veloute";
            category = DishCategory.STARTER;
        } else if (fruit > 0 && sweet > 0) {
            id = "fruit_compote";
            category = DishCategory.DESSERT;
        } else if (dairy > 0 && sweet > 0) {
            id = "sweet_cream";
            category = DishCategory.DESSERT;
        } else if (grain > 0 && dairy > 0) {
            id = "grain_porridge";
            category = DishCategory.MAIN;
        } else if (baseIngredients >= 2 && all.size() >= 2) {
            id = "experimental_dish";
            category = DishCategory.MAIN;
            coherent = false;
        } else {
            id = "dubious_dish";
            category = DishCategory.MAIN;
            dubious = true;
            coherent = false;
        }

        if (coherent) {
            score += 2;
        } else {
            score -= 2;
        }
        int primaryCategories = 0;
        for (FoodCategory primary : List.of(
                FoodCategory.MEAT, FoodCategory.FISH, FoodCategory.VEGETABLE,
                FoodCategory.FRUIT, FoodCategory.GRAIN, FoodCategory.MUSHROOM,
                FoodCategory.DAIRY, FoodCategory.EGG
        )) {
            if (all.contains(primary)) primaryCategories++;
        }
        if (primaryCategories >= 3) score += 1;
        if (seasoning && baseIngredients > 0) score += 1;
        score = Math.max(2, score);

        CookingRecipe base = BY_ID.get(id);
        DishRarity rarity = dubious ? DishRarity.COMMON : DishRarity.fromScore(score);
        int shelfLife = shelfLifeFor(category, all);
        CookingRecipe resolved = new CookingRecipe(
                base.id(),
                category,
                rarity,
                shelfLife,
                base.ingredients(),
                true
        );
        return new CookingResult(resolved, rarity, stacks.size(), score, dubious);
    }

    private static int shelfLifeFor(DishCategory category, Set<FoodCategory> categories) {
        int days = switch (category) {
            case STARTER, MAIN -> 2;
            case DESSERT -> 4;
            case DRINK -> 3;
        };
        if (categories.contains(FoodCategory.MEAT) || categories.contains(FoodCategory.FISH)) days--;
        if (categories.contains(FoodCategory.DAIRY) || categories.contains(FoodCategory.EGG)) days--;
        if (categories.contains(FoodCategory.SWEET)) days++;
        return Math.max(1, Math.min(7, days));
    }

    private static boolean matches(CookingRecipe recipe, List<ItemStack> stacks) {
        if (recipe.ingredients().size() != stacks.size()) {
            return false;
        }
        boolean[] used = new boolean[stacks.size()];
        for (CookingRecipe.IngredientRequirement requirement : recipe.ingredients()) {
            boolean found = false;
            for (int index = 0; index < stacks.size(); index++) {
                if (!used[index] && requirement.matches(stacks.get(index))) {
                    used[index] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static void fixed(
            String id,
            DishCategory category,
            DishRarity rarity,
            int shelfLifeDays,
            CookingRecipe.IngredientRequirement... ingredients
    ) {
        CookingRecipe recipe = new CookingRecipe(
                id, category, rarity, shelfLifeDays, List.of(ingredients), false
        );
        RecipeKey key = RecipeKey.ofItems(recipe.ingredients().stream().map(CookingRecipe.IngredientRequirement::item).toList());
        CookingRecipe conflict = FIXED_BY_KEY.putIfAbsent(key, recipe);
        if (conflict != null) {
            throw new IllegalStateException("Duplicate cooking recipe combination: " + conflict.id() + " / " + id);
        }
        BY_ID.put(id, recipe);
        FIXED.add(recipe);
    }

    private static void generic(String id, DishCategory category, List<String> hints) {
        List<CookingRecipe.IngredientRequirement> requirements = hints.stream()
                .map(hint -> new CookingRecipe.IngredientRequirement(Items.AIR, hint))
                .toList();
        CookingRecipe recipe = new CookingRecipe(id, category, DishRarity.COMMON, 2, requirements, true);
        BY_ID.put(id, recipe);
        GENERIC.add(recipe);
    }

    private record RecipeKey(List<String> itemIds) {
        static RecipeKey of(List<ItemStack> stacks) {
            return new RecipeKey(stacks.stream()
                    .map(stack -> net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString())
                    .sorted()
                    .toList());
        }

        static RecipeKey ofItems(List<Item> items) {
            return new RecipeKey(items.stream()
                    .map(item -> net.minecraft.registry.Registries.ITEM.getId(item).toString())
                    .sorted()
                    .toList());
        }
    }
}
