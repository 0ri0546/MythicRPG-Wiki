package com.mythicrpg.fishing;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class FishingCatchItem extends Item {
    public FishingCatchItem(Settings settings) { super(settings); }

    @Override
    public Text getName(ItemStack stack) {
        return FishingCatchData.read(stack)
                .<Text>map(data -> Text.translatable("item.mythicrpg.fishing_catch.named", data.family().displayName(), data.rarity().displayName())
                        .formatted(data.rarity().formatting()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        FishingCatchData.read(stack).ifPresent(data -> {
            tooltip.add(Text.translatable("tooltip.mythicrpg.fishing.family", data.family().displayName()).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.mythicrpg.fishing.rarity", data.rarity().displayName()).formatted(data.rarity().formatting()));
            tooltip.add(Text.translatable("tooltip.mythicrpg.fishing.not_direct_food").formatted(Formatting.DARK_GRAY));
        });
    }
}
