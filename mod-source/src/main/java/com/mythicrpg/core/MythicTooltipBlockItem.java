package com.mythicrpg.core;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;

import java.util.List;

public class MythicTooltipBlockItem extends BlockItem {

    private final List<MythicTooltipItem.TooltipLine> tooltipLines;

    public MythicTooltipBlockItem(
            Block block,
            Settings settings,
            List<MythicTooltipItem.TooltipLine> tooltipLines
    ) {
        super(block, settings);
        this.tooltipLines = tooltipLines;
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        for (MythicTooltipItem.TooltipLine line : tooltipLines) {
            tooltip.add(Text.translatable(line.translationKey()).formatted(line.formatting()));
        }
    }
}
