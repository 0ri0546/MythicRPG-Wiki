package com.mythicrpg.building;

import java.util.Locale;
import java.util.Optional;

/** Identifies the four Building tools that use the shared UI and selection foundations. */
public enum BuildingUiTool {
    PLAN_2D,
    PLAN_3D,
    MINIATURE,
    ARCHITECT_COMPASS;

    public String networkId() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "screen.mythicrpg.building_ui.tool." + networkId();
    }

    public static Optional<BuildingUiTool> fromNetworkId(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(id.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
