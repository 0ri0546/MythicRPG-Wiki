package com.mythicrpg.mining.archaeology;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class ExpeditionDossierItem extends Item {

    public ExpeditionDossierItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        ExpeditionDossierData.read(stack).ifPresentOrElse(dossier -> {
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.expedition_dossier.family",
                    dossier.family().displayName()
            ).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.expedition_dossier.rarity",
                    dossier.rarity().displayName()
            ).formatted(dossier.rarity().formatting()));
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.expedition_dossier.biome",
                    dossier.biomeId()
            ).formatted(Formatting.DARK_AQUA));
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.expedition_dossier.depth",
                    dossier.minY(),
                    dossier.maxY()
            ).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.expedition_dossier.x_range",
                    dossier.minX(),
                    dossier.maxX()
            ).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.expedition_dossier.z_range",
                    dossier.minZ(),
                    dossier.maxZ()
            ).formatted(Formatting.GRAY));
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.expedition_dossier.site",
                    dossier.siteId().toString().substring(0, 8)
            ).formatted(Formatting.DARK_GRAY));
        }, () -> tooltip.add(Text.translatable(
                "tooltip.mythicrpg.expedition_dossier.invalid"
        ).formatted(Formatting.RED)));
    }
}
