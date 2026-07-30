package com.mythicrpg.building;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.List;

/** Block item retaining an optional six-face configuration copied with pick-block. */
public final class BlankBlockItem extends BlockItem {
    public BlankBlockItem(Block block, Item.Settings settings) {
        super(block, settings);
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        super.appendTooltip(stack, context, tooltip, type);
        BlankBlockAppearance appearance = BlankBlockItemData.read(stack);
        tooltip.add(Text.translatable("tooltip.mythicrpg.blank_block.description")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.blank_block.apply")
                .formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.mythicrpg.blank_block.sneak")
                .formatted(Formatting.YELLOW));

        if (appearance.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.mythicrpg.blank_block.empty")
                    .formatted(Formatting.DARK_GRAY));
            return;
        }

        tooltip.add(Text.translatable(
                "tooltip.mythicrpg.blank_block.configured",
                appearance.configuredFaceCount()
        ).formatted(Formatting.AQUA));

        for (Direction face : Direction.values()) {
            Identifier id = appearance.material(face);
            if (id == null) {
                continue;
            }
            BlankBlockMaterialRegistry.resolve(id).ifPresent(block ->
                    tooltip.add(Text.translatable(
                            "tooltip.mythicrpg.blank_block.face",
                            Text.translatable("direction.mythicrpg." + face.asString()),
                            new ItemStack(block).getName()
                    ).formatted(Formatting.DARK_GRAY))
            );
        }
    }
}
