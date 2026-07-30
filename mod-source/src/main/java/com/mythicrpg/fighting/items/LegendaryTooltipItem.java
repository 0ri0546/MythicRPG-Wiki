package com.mythicrpg.fighting.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class LegendaryTooltipItem extends Item {
    private final String flavorTooltipKey;

    public LegendaryTooltipItem(Settings settings, String flavorTooltipKey) {
        super(settings);
        this.flavorTooltipKey = flavorTooltipKey;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        appendLegendaryTooltip(tooltip, flavorTooltipKey);
    }

    public static void appendLegendaryTooltip(List<Text> tooltip, String flavorTooltipKey) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.legendary").formatted(Formatting.GOLD));
        tooltip.add(Text.translatable(flavorTooltipKey).formatted(Formatting.GRAY));
    }
}
