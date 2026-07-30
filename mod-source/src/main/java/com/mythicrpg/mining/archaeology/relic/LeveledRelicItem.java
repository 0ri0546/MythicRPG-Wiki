package com.mythicrpg.mining.archaeology.relic;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class LeveledRelicItem extends Item {
    private final String descriptionKey;

    public LeveledRelicItem(Settings settings, String descriptionKey) {
        super(settings);
        this.descriptionKey = descriptionKey;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        RelicLevel level = RelicItemData.getLevel(stack);
        tooltip.add(Text.translatable("tooltip.mythicrpg.archaeology_relic.level", level.displayName())
                .formatted(level.formatting()));
        tooltip.add(Text.translatable(descriptionKey).formatted(Formatting.GRAY));
    }
}
