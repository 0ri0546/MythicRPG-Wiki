package com.mythicrpg.building;

import com.mythicrpg.MythicRPG;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Central registry for the finite Building V1 vertical-slab catalogue. */
public final class VerticalSlabRegistry {
    private static final int BUILDING_XP = 2;
    private static final Map<String, Block> BY_ID = new LinkedHashMap<>();
    private static final Map<Block, Block> SOURCE_BY_SLAB = new IdentityHashMap<>();
    private static final Set<Block> SLABS = Collections.newSetFromMap(new IdentityHashMap<>());

    public static final Block STONE = register("stone_vertical_slab", Blocks.STONE);
    public static final Block COBBLESTONE = register("cobblestone_vertical_slab", Blocks.COBBLESTONE);
    public static final Block STONE_BRICKS = register("stone_brick_vertical_slab", Blocks.STONE_BRICKS);
    public static final Block BRICKS = register("brick_vertical_slab", Blocks.BRICKS);
    public static final Block POLISHED_DEEPSLATE = register("polished_deepslate_vertical_slab", Blocks.POLISHED_DEEPSLATE);
    public static final Block QUARTZ = register("quartz_vertical_slab", Blocks.QUARTZ_BLOCK);
    public static final Block SANDSTONE = register("sandstone_vertical_slab", Blocks.SANDSTONE);
    public static final Block RED_SANDSTONE = register("red_sandstone_vertical_slab", Blocks.RED_SANDSTONE);
    public static final Block NETHER_BRICKS = register("nether_brick_vertical_slab", Blocks.NETHER_BRICKS);

    public static final Block OAK = register("oak_vertical_slab", Blocks.OAK_PLANKS);
    public static final Block SPRUCE = register("spruce_vertical_slab", Blocks.SPRUCE_PLANKS);
    public static final Block BIRCH = register("birch_vertical_slab", Blocks.BIRCH_PLANKS);
    public static final Block JUNGLE = register("jungle_vertical_slab", Blocks.JUNGLE_PLANKS);
    public static final Block ACACIA = register("acacia_vertical_slab", Blocks.ACACIA_PLANKS);
    public static final Block DARK_OAK = register("dark_oak_vertical_slab", Blocks.DARK_OAK_PLANKS);
    public static final Block MANGROVE = register("mangrove_vertical_slab", Blocks.MANGROVE_PLANKS);
    public static final Block CHERRY = register("cherry_vertical_slab", Blocks.CHERRY_PLANKS);
    public static final Block BAMBOO = register("bamboo_vertical_slab", Blocks.BAMBOO_PLANKS);
    public static final Block CRIMSON = register("crimson_vertical_slab", Blocks.CRIMSON_PLANKS);
    public static final Block WARPED = register("warped_vertical_slab", Blocks.WARPED_PLANKS);

    private static boolean initialized;

    private VerticalSlabRegistry() {
    }

    private static Block register(String name, Block source) {
        Identifier id = Identifier.of(MythicRPG.MOD_ID, name);
        VerticalSlabBlock block = new VerticalSlabBlock(
                AbstractBlock.Settings.copy(source).nonOpaque()
        );

        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));

        BY_ID.put(name, block);
        SOURCE_BY_SLAB.put(block, source);
        SLABS.add(block);
        BuildingBlockCatalog.registerCustom(block, BUILDING_XP);
        return block;
    }

    /** Forces registration and installs the creative-tab entries once. */
    public static void register() {
        if (initialized) {
            return;
        }
        initialized = true;

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            for (Block block : BY_ID.values()) {
                entries.add(block.asItem());
            }
        });

        MythicRPG.LOGGER.info("Registered {} Building vertical slabs", BY_ID.size());
    }

    public static boolean isVerticalSlab(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem && isVerticalSlab(blockItem.getBlock());
    }

    public static boolean isVerticalSlab(Block block) {
        return SLABS.contains(block);
    }

    public static Block sourceBlock(Block slab) {
        return SOURCE_BY_SLAB.get(slab);
    }

    public static Map<String, Block> blocksById() {
        return Collections.unmodifiableMap(BY_ID);
    }
}
