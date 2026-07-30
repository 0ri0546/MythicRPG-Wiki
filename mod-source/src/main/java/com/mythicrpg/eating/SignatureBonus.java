package com.mythicrpg.eating;

import java.util.Locale;
import java.util.Optional;

public enum SignatureBonus {
    DAMAGE,
    RESISTANCE,
    SPEED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static SignatureBonus byOrdinal(int ordinal) {
        SignatureBonus[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    public static Optional<SignatureBonus> byId(String id) {
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
