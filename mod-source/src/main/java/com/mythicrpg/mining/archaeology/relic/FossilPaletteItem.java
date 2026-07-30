package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ItemContainerUtils;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Relique de construction : son contenu est exposé directement par les slots
 * supplémentaires du PlayerScreenHandler lorsque la Palette est en offhand.
 */
public final class FossilPaletteItem extends LeveledRelicItem {
    public static final int MAX_SLOTS = 8;

    public FossilPaletteItem(Settings settings) {
        super(settings, "tooltip.mythicrpg.fossil_palette.description");
    }

    public static int slots(ItemStack stack) {
        return RelicItemData.getLevel(stack).value() + 3;
    }

    public static DefaultedList<ItemStack> read(ItemStack stack) {
        return ItemContainerUtils.read(stack, MAX_SLOTS);
    }

    public static void write(ItemStack stack, DefaultedList<ItemStack> contents) {
        ItemContainerUtils.write(stack, contents);
    }

    public static boolean accepted(ItemStack stack) {
        return stack.isEmpty() || stack.getItem() instanceof BlockItem;
    }
}
