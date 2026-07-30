package com.mythicrpg.eating;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;

public final class CookingHeatRegistry {
    private CookingHeatRegistry() {
    }

    public static boolean isHeatSource(BlockState state) {
        if (state.isIn(BlockTags.CAMPFIRES)) {
            return state.getOrEmpty(Properties.LIT).orElse(false);
        }
        return state.isOf(Blocks.FIRE)
                || state.isOf(Blocks.SOUL_FIRE)
                || state.isOf(Blocks.MAGMA_BLOCK)
                || state.isOf(Blocks.LAVA);
    }
}
