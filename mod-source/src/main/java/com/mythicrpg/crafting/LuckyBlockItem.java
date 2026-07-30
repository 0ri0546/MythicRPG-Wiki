package com.mythicrpg.crafting;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class LuckyBlockItem extends BlockItem {

    public LuckyBlockItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        int luck = LuckyBlockLuckManager.getLuck(stack);

        tooltip.add(Text.translatable("tooltip.mythicrpg.lucky_block.description")
                .formatted(Formatting.GRAY));

        tooltip.add(Text.translatable("tooltip.mythicrpg.lucky_block.luck", formatLuck(luck))
                .formatted(getLuckFormatting(luck)));

        tooltip.add(Text.translatable("tooltip.mythicrpg.lucky_block.use")
                .formatted(Formatting.GREEN));
    }

    private static String formatLuck(int luck) {
        if (luck > 0) {
            return "+" + luck;
        }

        return String.valueOf(luck);
    }

    private static Formatting getLuckFormatting(int luck) {
        if (luck > 0) {
            return Formatting.GOLD;
        }

        if (luck < 0) {
            return Formatting.DARK_PURPLE;
        }

        return Formatting.YELLOW;
    }
}