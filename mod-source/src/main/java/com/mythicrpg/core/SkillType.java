package com.mythicrpg.core;

import java.util.Locale;
import java.util.Optional;

import net.minecraft.text.Text;

public enum SkillType {
    MINING,
    FIGHTING,
    WOODCUTTING,
    FARMING,
    CRAFTING,
    TRAVELING,
    BUILDING,
    FISHING,
    EATING;


    public String translationKey() {
        return "skill.mythicrpg." + name().toLowerCase(Locale.ROOT);
    }

    public Text displayName() {
        return Text.translatable(translationKey());
    }

    /**
     * Parses a serialized/network skill identifier without letting stale or malformed values
     * break player data loading or packet handling.
     */
    public static Optional<SkillType> fromId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(valueOf(id.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
