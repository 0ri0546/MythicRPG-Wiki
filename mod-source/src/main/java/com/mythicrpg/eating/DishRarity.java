package com.mythicrpg.eating;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.Optional;

public enum DishRarity {
    COMMON(0, 14.4F, Formatting.WHITE),
    RARE(1, 15.8F, Formatting.AQUA),
    EPIC(2, 17.2F, Formatting.LIGHT_PURPLE),
    LEGENDARY(3, 18.6F, Formatting.GOLD),
    MYTHIC(4, 20.0F, Formatting.RED);

    private final int rank;
    private final float saturation;
    private final Formatting formatting;

    DishRarity(int rank, float saturation, Formatting formatting) {
        this.rank = rank;
        this.saturation = saturation;
        this.formatting = formatting;
    }

    public int rank() {
        return rank;
    }

    public float saturation() {
        return saturation;
    }

    public Formatting formatting() {
        return formatting;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public Text displayName() {
        return Text.translatable("dish_rarity.mythicrpg." + id()).formatted(formatting);
    }

    public static DishRarity fromScore(int score) {
        if (score >= 18) return MYTHIC;
        if (score >= 14) return LEGENDARY;
        if (score >= 10) return EPIC;
        if (score >= 6) return RARE;
        return COMMON;
    }

    public static DishRarity byRank(int rank) {
        for (DishRarity rarity : values()) {
            if (rarity.rank == rank) {
                return rarity;
            }
        }
        return COMMON;
    }

    public static Optional<DishRarity> byId(String id) {
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
