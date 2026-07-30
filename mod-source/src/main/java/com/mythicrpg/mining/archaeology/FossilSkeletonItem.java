package com.mythicrpg.mining.archaeology;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class FossilSkeletonItem extends Item {

    private final FossilFamily family;
    private final FossilRarity rarity;

    public FossilSkeletonItem(FossilFamily family, FossilRarity rarity, Settings settings) {
        super(settings);
        this.family = family;
        this.rarity = rarity;
    }

    public FossilFamily family() {
        return family;
    }

    public FossilRarity rarity() {
        return rarity;
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.fossil.family", family.displayName())
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.fossil.rarity", rarity.displayName())
                .formatted(Formatting.GRAY));

        FossilSpecimenData.read(stack).ifPresentOrElse(specimen -> {
            tooltip.add(Text.translatable(
                            "tooltip.mythicrpg.skeleton.specimen",
                            specimen.specimenId().toString().substring(0, 8)
                    )
                    .formatted(Formatting.DARK_GRAY));
            tooltip.add(Text.translatable(
                            specimen.analyzed()
                                    ? "tooltip.mythicrpg.skeleton.analyzed"
                                    : "tooltip.mythicrpg.skeleton.unanalyzed"
                    )
                    .formatted(specimen.analyzed() ? Formatting.GREEN : Formatting.YELLOW));
        }, () -> tooltip.add(Text.translatable("tooltip.mythicrpg.skeleton.unclaimed")
                .formatted(Formatting.RED)));
    }
}
