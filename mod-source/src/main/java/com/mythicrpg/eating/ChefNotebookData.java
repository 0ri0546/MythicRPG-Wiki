package com.mythicrpg.eating;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/** Legacy notebook-local configuration kept only for one-time migration to the player profile. */
public final class ChefNotebookData {
    private static final String ROOT = "mythicrpg_signature_configuration";
    private static final String NAME = "name";
    private static final String INGREDIENTS = "ingredients";
    private static final String ICON = "icon";
    private static final String BONUS = "bonus";
    private static final String SEPARATOR = ";";

    private ChefNotebookData() {
    }

    public static void write(ItemStack notebook, Configuration configuration) {
        if (configuration == null || !configuration.isValid()) {
            clear(notebook);
            return;
        }
        NbtCompound data = new NbtCompound();
        data.putString(NAME, sanitizeName(configuration.name()));
        data.putString(INGREDIENTS, configuration.ingredients().stream()
                .map(SignatureIngredient::serialize).reduce((a, b) -> a + SEPARATOR + b).orElse(""));
        data.putString(ICON, configuration.icon().toString());
        data.putString(BONUS, configuration.bonus().id());
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, notebook, nbt -> nbt.put(ROOT, data));
    }

    public static Optional<Configuration> read(ItemStack notebook) {
        NbtCompound custom = notebook.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!custom.contains(ROOT)) {
            return Optional.empty();
        }
        NbtCompound data = custom.getCompound(ROOT);
        ArrayList<SignatureIngredient> ingredients = new ArrayList<>();
        String rawIngredients = data.getString(INGREDIENTS);
        if (!rawIngredients.isBlank()) {
            for (String raw : rawIngredients.split(SEPARATOR)) {
                SignatureIngredient descriptor = SignatureIngredient.parse(raw).orElse(null);
                if (descriptor == null) {
                    return Optional.empty();
                }
                ingredients.add(descriptor);
            }
        }
        Identifier icon = Identifier.tryParse(data.getString(ICON));
        SignatureBonus bonus = SignatureBonus.byId(data.getString(BONUS)).orElse(null);
        Configuration configuration = new Configuration(sanitizeName(data.getString(NAME)), ingredients, icon, bonus);
        return configuration.isValid() ? Optional.of(configuration) : Optional.empty();
    }

    public static void clear(ItemStack notebook) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, notebook, nbt -> nbt.remove(ROOT));
    }

    public static String sanitizeName(String value) {
        if (value == null) {
            return "Signature Dish";
        }
        String cleaned = value.strip().replaceAll("[\\p{Cntrl}]", "");
        if (cleaned.isBlank()) {
            return "Signature Dish";
        }
        return cleaned.length() > 32 ? cleaned.substring(0, 32) : cleaned;
    }

    public record Configuration(
            String name,
            List<SignatureIngredient> ingredients,
            Identifier icon,
            SignatureBonus bonus
    ) {
        public Configuration {
            name = sanitizeName(name);
            ingredients = List.copyOf(ingredients == null ? List.of() : ingredients);
            bonus = bonus == null ? SignatureBonus.DAMAGE : bonus;
        }

        public List<Identifier> ingredientIds() {
            return ingredients.stream().map(SignatureIngredient::itemId).toList();
        }

        public boolean isValid() {
            if (ingredients.size() < 2
                    || ingredients.size() > 5
                    || icon == null
                    || !ingredientIds().contains(icon)) {
                return false;
            }
            HashSet<Identifier> uniqueItems = new HashSet<>();
            for (SignatureIngredient ingredient : ingredients) {
                if (ingredient == null || !ingredient.isValid() || !uniqueItems.add(ingredient.itemId())) {
                    return false;
                }
            }
            return true;
        }
    }
}
