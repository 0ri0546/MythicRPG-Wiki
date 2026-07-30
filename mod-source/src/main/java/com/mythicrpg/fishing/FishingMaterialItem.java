package com.mythicrpg.fishing;

import net.minecraft.item.Item;

public final class FishingMaterialItem extends Item {
    private final FishingRarity rarity;
    private final boolean shell;

    public FishingMaterialItem(FishingRarity rarity, boolean shell, Settings settings) {
        super(settings);
        this.rarity = rarity;
        this.shell = shell;
    }

    public FishingRarity rarity() {
        return rarity;
    }

    public boolean shell() {
        return shell;
    }
}
