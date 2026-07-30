package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ModItems;
import com.mythicrpg.mining.archaeology.FossilFamily;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;

public final class ArchaeologyRelicRewards {
    private ArchaeologyRelicRewards() {}

    public static ItemStack create(FossilFamily family, Random random) {
        Item item = switch (family) {
            case LARGE_LAND -> ModItems.COLOSSAL_AEGIS;
            case SMALL_LAND -> ModItems.GROWTH_TOTEM;
            case MARINE -> ModItems.FOSSIL_DRILL;
            case FLYING -> ModItems.TEMPORAL_MACHINE;
            case INSECT -> ModItems.FOSSIL_PALETTE;
        };
        ItemStack stack = new ItemStack(item);
        RelicItemData.setLevel(stack, RelicLevel.roll(random));
        return stack;
    }
}
