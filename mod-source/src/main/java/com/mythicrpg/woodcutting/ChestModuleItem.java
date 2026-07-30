package com.mythicrpg.woodcutting;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Capacity upgrade installed in a dedicated vanilla chest module slot. */
public final class ChestModuleItem extends Item {

    private final int extraSlots;

    public ChestModuleItem(int extraSlots, Settings settings) {
        super(settings);
        if (extraSlots <= 0 || extraSlots % 9 != 0) {
            throw new IllegalArgumentException("Chest module capacity must be a positive multiple of 9");
        }
        this.extraSlots = extraSlots;
    }

    public int extraSlots() {
        return extraSlots;
    }

    public static boolean isModule(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ChestModuleItem;
    }

    public static int extraSlots(ItemStack stack) {
        return stack.getItem() instanceof ChestModuleItem module ? module.extraSlots() : 0;
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.chest_module.capacity", extraSlots)
                .formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.chest_module.use")
                .formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.mythicrpg.chest_module.remove")
                .formatted(Formatting.GRAY));
    }
}
