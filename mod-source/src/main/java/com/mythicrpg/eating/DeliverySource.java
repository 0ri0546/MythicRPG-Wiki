package com.mythicrpg.eating;

public enum DeliverySource {
    COOKING_POT,
    FRIDGE;

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static DeliverySource byOrdinal(int ordinal) {
        DeliverySource[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
