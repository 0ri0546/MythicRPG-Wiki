package com.mythicrpg.fighting.items;

import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public class LegendaryShieldItem extends ShieldItem {
    private final String flavorTooltipKey;

    public LegendaryShieldItem(Settings settings, String flavorTooltipKey) {
        super(settings);
        this.flavorTooltipKey = flavorTooltipKey;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        LegendaryTooltipItem.appendLegendaryTooltip(tooltip, flavorTooltipKey);
    }
}
