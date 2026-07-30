package com.mythicrpg.building;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Finite V1 whitelist of opaque, static, full-cube materials usable on Blank Block faces. */
public final class BlankBlockMaterialRegistry {
    private static final Map<Identifier, Block> BY_ID = new LinkedHashMap<>();
    private static final Set<Block> BLOCKS = Collections.newSetFromMap(new IdentityHashMap<>());

    static {
        register(
                Blocks.STONE,
                Blocks.COBBLESTONE,
                Blocks.MOSSY_COBBLESTONE,
                Blocks.SMOOTH_STONE,
                Blocks.STONE_BRICKS,
                Blocks.BRICKS,
                Blocks.MUD_BRICKS,

                Blocks.GRANITE,
                Blocks.POLISHED_GRANITE,
                Blocks.DIORITE,
                Blocks.POLISHED_DIORITE,
                Blocks.ANDESITE,
                Blocks.POLISHED_ANDESITE,

                Blocks.DEEPSLATE,
                Blocks.COBBLED_DEEPSLATE,
                Blocks.POLISHED_DEEPSLATE,
                Blocks.DEEPSLATE_BRICKS,
                Blocks.CRACKED_DEEPSLATE_BRICKS,
                Blocks.DEEPSLATE_TILES,
                Blocks.CRACKED_DEEPSLATE_TILES,
                Blocks.CHISELED_DEEPSLATE,

                Blocks.TUFF,
                Blocks.POLISHED_TUFF,
                Blocks.TUFF_BRICKS,
                Blocks.CHISELED_TUFF_BRICKS,

                Blocks.SANDSTONE,
                Blocks.CHISELED_SANDSTONE,
                Blocks.RED_SANDSTONE,
                Blocks.CHISELED_RED_SANDSTONE,

                Blocks.NETHER_BRICKS,
                Blocks.CHISELED_NETHER_BRICKS,
                Blocks.CRACKED_NETHER_BRICKS,

                Blocks.BLACKSTONE,
                Blocks.CHISELED_POLISHED_BLACKSTONE,

                Blocks.QUARTZ_BLOCK,
                Blocks.CHISELED_QUARTZ_BLOCK,

                Blocks.BASALT,
                Blocks.POLISHED_BASALT,
                Blocks.SMOOTH_BASALT,

                Blocks.OAK_PLANKS,
                Blocks.SPRUCE_PLANKS,
                Blocks.BIRCH_PLANKS,
                Blocks.JUNGLE_PLANKS,
                Blocks.ACACIA_PLANKS,
                Blocks.DARK_OAK_PLANKS,
                Blocks.MANGROVE_PLANKS,
                Blocks.CHERRY_PLANKS,
                Blocks.BAMBOO_PLANKS,
                Blocks.BAMBOO_MOSAIC,
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
                Blocks.BLACK_WOOL,

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

                Blocks.TERRACOTTA,
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

                Blocks.PRISMARINE,
                Blocks.PRISMARINE_BRICKS,
                Blocks.DARK_PRISMARINE,

                Blocks.END_STONE_BRICKS,
                Blocks.PURPUR_BLOCK,
                Blocks.SMOOTH_QUARTZ,
                Blocks.CALCITE,
                Blocks.PACKED_MUD,

                Blocks.COPPER_BLOCK,
                Blocks.EXPOSED_COPPER,
                Blocks.WEATHERED_COPPER,
                Blocks.OXIDIZED_COPPER,

                Blocks.AMETHYST_BLOCK
        );
    }

    private BlankBlockMaterialRegistry() {
    }

    private static void register(Block... blocks) {
        for (Block block : blocks) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id == null || block.asItem() == Items.AIR) {
                throw new IllegalStateException("Invalid Blank Block material: " + block);
            }
            BY_ID.put(id, block);
            BLOCKS.add(block);
        }
    }

    public static boolean isAllowed(Block block) {
        return block != null && BLOCKS.contains(block);
    }

    public static boolean isAllowed(ItemStack stack) {
        return stack != null
                && stack.getItem() instanceof BlockItem blockItem
                && isAllowed(blockItem.getBlock());
    }

    public static Optional<Block> resolve(Identifier id) {
        return Optional.ofNullable(id == null ? null : BY_ID.get(id));
    }

    public static Identifier id(Block block) {
        return isAllowed(block) ? Registries.BLOCK.getId(block) : null;
    }

    public static Item item(Identifier id) {
        Block block = BY_ID.get(id);
        return block == null ? Items.AIR : block.asItem();
    }

    public static boolean isValid(BlankBlockAppearance appearance) {
        if (appearance == null) {
            return false;
        }
        for (Identifier id : appearance.configuredMaterials()) {
            if (!BY_ID.containsKey(id)) {
                return false;
            }
        }
        return true;
    }

    public static Map<Identifier, Block> materials() {
        return Collections.unmodifiableMap(BY_ID);
    }
}
