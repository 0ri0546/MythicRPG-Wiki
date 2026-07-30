package com.mythicrpg.building;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Central whitelist and XP values shared by every Building subsystem. */
public final class BuildingBlockCatalog {
    private static final Map<Block, Integer> BASE_XP = new IdentityHashMap<>();

    static {
        register(3,
                Blocks.STONE,
                Blocks.COBBLESTONE,
                Blocks.ANDESITE,
                Blocks.DIORITE,
                Blocks.GRANITE,
                Blocks.DEEPSLATE,
                Blocks.COBBLED_DEEPSLATE,
                Blocks.TUFF,

                Blocks.OAK_PLANKS,
                Blocks.SPRUCE_PLANKS,
                Blocks.BIRCH_PLANKS,
                Blocks.JUNGLE_PLANKS,
                Blocks.ACACIA_PLANKS,
                Blocks.DARK_OAK_PLANKS,
                Blocks.MANGROVE_PLANKS,
                Blocks.CHERRY_PLANKS,
                Blocks.BAMBOO_PLANKS,
                Blocks.CRIMSON_PLANKS,
                Blocks.WARPED_PLANKS,

                Blocks.WHITE_WOOL,
                Blocks.ORANGE_WOOL,
                Blocks.MAGENTA_WOOL,
                Blocks.LIGHT_BLUE_WOOL,
                Blocks.YELLOW_WOOL,
                Blocks.LIME_WOOL,
                Blocks.PINK_WOOL,
                Blocks.GRAY_WOOL,
                Blocks.LIGHT_GRAY_WOOL,
                Blocks.CYAN_WOOL,
                Blocks.PURPLE_WOOL,
                Blocks.BLUE_WOOL,
                Blocks.BROWN_WOOL,
                Blocks.GREEN_WOOL,
                Blocks.RED_WOOL,
                Blocks.BLACK_WOOL
        );

        register(4,
                Blocks.SMOOTH_STONE,
                Blocks.STONE_BRICKS,
                Blocks.MOSSY_STONE_BRICKS,
                Blocks.CRACKED_STONE_BRICKS,
                Blocks.CHISELED_STONE_BRICKS,
                Blocks.MOSSY_COBBLESTONE,
                Blocks.BRICKS,
                Blocks.MUD_BRICKS,

                Blocks.POLISHED_ANDESITE,
                Blocks.POLISHED_DIORITE,
                Blocks.POLISHED_GRANITE,

                Blocks.POLISHED_DEEPSLATE,
                Blocks.DEEPSLATE_BRICKS,
                Blocks.CRACKED_DEEPSLATE_BRICKS,
                Blocks.DEEPSLATE_TILES,
                Blocks.CRACKED_DEEPSLATE_TILES,
                Blocks.CHISELED_DEEPSLATE,

                Blocks.POLISHED_TUFF,
                Blocks.TUFF_BRICKS,
                Blocks.CHISELED_TUFF,
                Blocks.CHISELED_TUFF_BRICKS,

                Blocks.SANDSTONE,
                Blocks.SMOOTH_SANDSTONE,
                Blocks.CUT_SANDSTONE,
                Blocks.CHISELED_SANDSTONE,

                Blocks.RED_SANDSTONE,
                Blocks.SMOOTH_RED_SANDSTONE,
                Blocks.CUT_RED_SANDSTONE,
                Blocks.CHISELED_RED_SANDSTONE,

                Blocks.PRISMARINE,
                Blocks.PRISMARINE_BRICKS,
                Blocks.DARK_PRISMARINE,

                Blocks.NETHER_BRICKS,
                Blocks.RED_NETHER_BRICKS,
                Blocks.CHISELED_NETHER_BRICKS,
                Blocks.CRACKED_NETHER_BRICKS,

                Blocks.BLACKSTONE,
                Blocks.POLISHED_BLACKSTONE,
                Blocks.POLISHED_BLACKSTONE_BRICKS,
                Blocks.CHISELED_POLISHED_BLACKSTONE,

                Blocks.END_STONE_BRICKS,

                Blocks.PURPUR_BLOCK,
                Blocks.PURPUR_PILLAR,

                Blocks.QUARTZ_BLOCK,
                Blocks.QUARTZ_BRICKS,
                Blocks.QUARTZ_PILLAR,
                Blocks.SMOOTH_QUARTZ,
                Blocks.CHISELED_QUARTZ_BLOCK,

                Blocks.CALCITE,
                Blocks.DRIPSTONE_BLOCK,
                Blocks.PACKED_MUD,
                Blocks.TERRACOTTA,

                Blocks.BAMBOO_MOSAIC,

                Blocks.BASALT,
                Blocks.POLISHED_BASALT,
                Blocks.SMOOTH_BASALT
        );

        register(5,
                Blocks.WHITE_CONCRETE,
                Blocks.ORANGE_CONCRETE,
                Blocks.MAGENTA_CONCRETE,
                Blocks.LIGHT_BLUE_CONCRETE,
                Blocks.YELLOW_CONCRETE,
                Blocks.LIME_CONCRETE,
                Blocks.PINK_CONCRETE,
                Blocks.GRAY_CONCRETE,
                Blocks.LIGHT_GRAY_CONCRETE,
                Blocks.CYAN_CONCRETE,
                Blocks.PURPLE_CONCRETE,
                Blocks.BLUE_CONCRETE,
                Blocks.BROWN_CONCRETE,
                Blocks.GREEN_CONCRETE,
                Blocks.RED_CONCRETE,
                Blocks.BLACK_CONCRETE,

                Blocks.WHITE_TERRACOTTA,
                Blocks.ORANGE_TERRACOTTA,
                Blocks.MAGENTA_TERRACOTTA,
                Blocks.LIGHT_BLUE_TERRACOTTA,
                Blocks.YELLOW_TERRACOTTA,
                Blocks.LIME_TERRACOTTA,
                Blocks.PINK_TERRACOTTA,
                Blocks.GRAY_TERRACOTTA,
                Blocks.LIGHT_GRAY_TERRACOTTA,
                Blocks.CYAN_TERRACOTTA,
                Blocks.PURPLE_TERRACOTTA,
                Blocks.BLUE_TERRACOTTA,
                Blocks.BROWN_TERRACOTTA,
                Blocks.GREEN_TERRACOTTA,
                Blocks.RED_TERRACOTTA,
                Blocks.BLACK_TERRACOTTA,

                Blocks.WHITE_GLAZED_TERRACOTTA,
                Blocks.ORANGE_GLAZED_TERRACOTTA,
                Blocks.MAGENTA_GLAZED_TERRACOTTA,
                Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA,
                Blocks.YELLOW_GLAZED_TERRACOTTA,
                Blocks.LIME_GLAZED_TERRACOTTA,
                Blocks.PINK_GLAZED_TERRACOTTA,
                Blocks.GRAY_GLAZED_TERRACOTTA,
                Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA,
                Blocks.CYAN_GLAZED_TERRACOTTA,
                Blocks.PURPLE_GLAZED_TERRACOTTA,
                Blocks.BLUE_GLAZED_TERRACOTTA,
                Blocks.BROWN_GLAZED_TERRACOTTA,
                Blocks.GREEN_GLAZED_TERRACOTTA,
                Blocks.RED_GLAZED_TERRACOTTA,
                Blocks.BLACK_GLAZED_TERRACOTTA,

                Blocks.GLASS,
                Blocks.TINTED_GLASS,

                Blocks.SEA_LANTERN,
                Blocks.GLOWSTONE,
                Blocks.SHROOMLIGHT,
                Blocks.OCHRE_FROGLIGHT,
                Blocks.VERDANT_FROGLIGHT,
                Blocks.PEARLESCENT_FROGLIGHT,

                Blocks.AMETHYST_BLOCK,

                Blocks.COPPER_BLOCK,
                Blocks.EXPOSED_COPPER,
                Blocks.WEATHERED_COPPER,
                Blocks.OXIDIZED_COPPER,

                Blocks.CUT_COPPER,
                Blocks.EXPOSED_CUT_COPPER,
                Blocks.WEATHERED_CUT_COPPER,
                Blocks.OXIDIZED_CUT_COPPER
        );
    }

    private BuildingBlockCatalog() {}

    private static void register(int xp, Block... blocks) {
        for (Block block : blocks) BASE_XP.put(block, xp);
    }

    /** Registers a MythicRPG Building block after its block registry entry exists. */
    public static void registerCustom(Block block, int xp) {
        if (block == null || xp <= 0) {
            throw new IllegalArgumentException("Building blocks require a non-null block and positive XP");
        }
        BASE_XP.put(block, xp);
    }

    public static boolean isEligible(Block block) { return BASE_XP.containsKey(block); }
    public static int baseXp(Block block) { return BASE_XP.getOrDefault(block, 0); }
    public static Set<Block> eligibleBlocks() { return Collections.unmodifiableSet(BASE_XP.keySet()); }
}
