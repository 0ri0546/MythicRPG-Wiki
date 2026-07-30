package com.mythicrpg.crafting;

import com.mythicrpg.crafting.station.CraftingStationType;
import net.minecraft.block.BlockState;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class InfiniteCraftingTableBlock extends CraftingTableBlock {

    private static final Text TITLE = Text.translatable("block.mythicrpg.infinite_crafting_table");

    public InfiniteCraftingTableBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected NamedScreenHandlerFactory createScreenHandlerFactory(
            BlockState state,
            World world,
            BlockPos pos
    ) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, inventory, player) -> new MythicCraftingScreenHandler(
                        syncId,
                        inventory,
                        ScreenHandlerContext.create(world, pos),
                        CraftingStationType.INFINITE_TABLE,
                        pos
                ),
                TITLE
        );
    }
}
