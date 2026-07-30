package com.mythicrpg.crafting.station;

public enum CraftingStationType {
    PORTABLE(0, true),
    VANILLA_TABLE(1, true),
    INFINITE_TABLE(2, false);

    private final int id;
    private final boolean finiteDurability;

    CraftingStationType(int id, boolean finiteDurability) {
        this.id = id;
        this.finiteDurability = finiteDurability;
    }

    public int getId() {
        return id;
    }

    public boolean hasFiniteDurability() {
        return finiteDurability;
    }

    public static CraftingStationType byId(int id) {
        for (CraftingStationType type : values()) {
            if (type.id == id) {
                return type;
            }
        }

        return PORTABLE;
    }
}
