package com.mythicrpg.building;

import java.util.Locale;
import java.util.Optional;

/** Local structure axes used by the Building preview and rotation controls. */
public enum BuildingRotationAxis {
    X,
    Y,
    Z;

    public String networkId() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String translationKey() {
        return "screen.mythicrpg.building_ui.axis." + networkId();
    }

    public static Optional<BuildingRotationAxis> fromNetworkId(String id) {
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
