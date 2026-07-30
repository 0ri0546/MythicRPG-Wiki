package com.mythicrpg.mining;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Copie serveur de la préférence cliente.
 * Le Vein Mining est activé par défaut.
 */
public final class VeinMiningToggleState {
    private static final Map<UUID, Boolean> ENABLED =
            new HashMap<>();

    private VeinMiningToggleState() {
    }

    public static boolean isEnabled(UUID playerId) {
        return ENABLED.getOrDefault(playerId, true);
    }

    public static void setEnabled(
            UUID playerId,
            boolean enabled
    ) {
        ENABLED.put(playerId, enabled);
    }

    public static void clear(UUID playerId) {
        ENABLED.remove(playerId);
    }
}