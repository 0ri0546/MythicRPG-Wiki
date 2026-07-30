package com.mythicrpg.eating;

import java.util.Locale;

public enum FoodCategory {
    MEAT,
    FISH,
    VEGETABLE,
    FRUIT,
    GRAIN,
    MUSHROOM,
    DAIRY,
    EGG,
    SWEET,
    SALTY,
    SPICE,
    LIQUID,
    THICKENER,
    NETHER,
    END,
    OCEAN,
    UNDERGROUND,
    MAGICAL,
    MONSTROUS,
    PRECIOUS,
    TOXIC;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
