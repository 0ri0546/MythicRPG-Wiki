package com.mythicrpg.core;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class MythicTooltipItem extends Item {
    private final List<TooltipLine> tooltipLines;

    public MythicTooltipItem(Settings settings, List<TooltipLine> tooltipLines) {
        super(settings);
        this.tooltipLines = tooltipLines;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        for (TooltipLine line : tooltipLines) {
            tooltip.add(Text.translatable(line.translationKey()).formatted(line.formatting()));
        }
    }

    public static TooltipLine line(String translationKey, Formatting formatting) {
        return new TooltipLine(translationKey, formatting);
    }

    public record TooltipLine(String translationKey, Formatting formatting) {
    }
}