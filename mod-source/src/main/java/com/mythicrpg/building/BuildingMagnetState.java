package com.mythicrpg.building;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-side mirror of the player's client preference. Defaults to enabled. */
public final class BuildingMagnetState {
    private static final Map<UUID, Boolean> ENABLED = new HashMap<>();

    private BuildingMagnetState() {
    }

    public static boolean isEnabled(UUID playerId) {
        return ENABLED.getOrDefault(playerId, true);
    }

    public static void setEnabled(UUID playerId, boolean enabled) {
        ENABLED.put(playerId, enabled);
    }

    public static void clear(UUID playerId) {
        ENABLED.remove(playerId);
    }
}
