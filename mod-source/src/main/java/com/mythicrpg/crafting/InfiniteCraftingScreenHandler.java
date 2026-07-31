package com.mythicrpg.crafting;

import com.mythicrpg.core.ModBlocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;

public class InfiniteCraftingScreenHandler extends CraftingScreenHandler {

    private final ScreenHandlerContext context;

    public InfiniteCraftingScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            ScreenHandlerContext context
    ) {
        super(syncId, playerInventory, context);
        this.context = context;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return canUse(context, player, ModBlocks.INFINITE_CRAFTING_TABLE);
    }
}