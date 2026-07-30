package com.mythicrpg.eating;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Optional;

/** Component-aware, bounded ingredient descriptor for a player's signature recipe. */
public record SignatureIngredient(Identifier itemId, String variant) {
    private static final String SEPARATOR = "#";
    private static final String WATER = "water";

    public SignatureIngredient {
        variant = variant == null ? "" : variant;
    }

    public static Optional<SignatureIngredient> fromStack(ItemStack stack) {
        if (stack.isEmpty() || !CulinaryIngredientRegistry.isCulinaryIngredient(stack)) {
            return Optional.empty();
        }
        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        String variant = stack.isOf(Items.POTION) ? WATER : "";
        return Optional.of(new SignatureIngredient(itemId, variant));
    }

    public static Optional<SignatureIngredient> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        int separator = raw.indexOf(SEPARATOR);
        String rawId = separator < 0 ? raw : raw.substring(0, separator);
        String variant = separator < 0 ? "" : raw.substring(separator + 1);
        Identifier itemId = Identifier.tryParse(rawId);
        if (itemId == null || !Registries.ITEM.containsId(itemId)) {
            return Optional.empty();
        }
        if (Registries.ITEM.get(itemId) == Items.POTION) {
            // Legacy potion descriptors are migrated to the only culinary potion variant: water.
            variant = WATER;
        } else if (!variant.isBlank()) {
            return Optional.empty();
        }
        SignatureIngredient descriptor = new SignatureIngredient(itemId, variant);
        return descriptor.isValid() ? Optional.of(descriptor) : Optional.empty();
    }

    public boolean isValid() {
        if (!Registries.ITEM.containsId(itemId)) {
            return false;
        }
        if (!CulinaryIngredientRegistry.isRegisteredItem(Registries.ITEM.get(itemId))) {
            return false;
        }
        return Registries.ITEM.get(itemId) != Items.POTION || WATER.equals(variant);
    }

    public boolean matches(ItemStack stack) {
        return isValid()
                && !stack.isEmpty()
                && Registries.ITEM.getId(stack.getItem()).equals(itemId)
                && CulinaryIngredientRegistry.isCulinaryIngredient(stack);
    }

    public String serialize() {
        return variant.isBlank() ? itemId.toString() : itemId + SEPARATOR + variant;
    }
}
