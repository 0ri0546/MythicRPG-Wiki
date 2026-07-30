package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.ModItems;
import net.minecraft.item.Item;

import java.util.Optional;

/** Central family/rarity lookup used by extraction and incubation. */
public final class FossilContentRegistry {

    private FossilContentRegistry() {
    }

    public static Optional<Item> fossilItem(FossilFamily family, FossilRarity rarity) {
        return ModItems.fossilItem(family, rarity);
    }

    public static Optional<Item> skeletonItem(FossilFamily family, FossilRarity rarity) {
        return ModItems.fossilSkeletonItem(family, rarity);
    }
}
