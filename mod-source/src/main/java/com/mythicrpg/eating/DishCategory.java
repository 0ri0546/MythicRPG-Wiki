package com.mythicrpg.eating;

import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Optional;

public enum DishCategory {
    STARTER,
    MAIN,
    DESSERT,
    DRINK;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Text displayName() {
        return Text.translatable("dish_category.mythicrpg." + id());
    }

    public static Optional<DishCategory> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(id.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
