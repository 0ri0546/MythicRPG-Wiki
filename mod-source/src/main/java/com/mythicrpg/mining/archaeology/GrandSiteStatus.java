package com.mythicrpg.mining.archaeology;

import java.util.Arrays;
import java.util.Optional;

import net.minecraft.text.Text;

public enum GrandSiteStatus {
    GENERATED("generated"),
    DISCOVERED("discovered"),
    PARTIALLY_EXCAVATED("partially_excavated"),
    DEPLETED("depleted");

    private final String id;

    GrandSiteStatus(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Text displayName() {
        return Text.translatable("status.mythicrpg.grand_site." + id);
    }

    public static Optional<GrandSiteStatus> byId(String id) {
        return Arrays.stream(values())
                .filter(value -> value.id.equalsIgnoreCase(id))
                .findFirst();
    }
}
