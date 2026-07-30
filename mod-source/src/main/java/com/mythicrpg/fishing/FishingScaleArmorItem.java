package com.mythicrpg.fishing;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.registry.entry.RegistryEntry;

public final class FishingScaleArmorItem extends ArmorItem {
    private final FishingRarity rarity;

    public FishingScaleArmorItem(
            RegistryEntry<ArmorMaterial> material,
            Type type,
            FishingRarity rarity,
            Settings settings
    ) {
        super(material, type, settings);
        this.rarity = rarity;
    }

    public FishingRarity rarity() {
        return rarity;
    }
}
