package com.mythicrpg.core;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.building.VerticalSlabRegistry;
import com.mythicrpg.building.BuildingReserveChestBlock;
import com.mythicrpg.building.BlankBlock;
import com.mythicrpg.building.BlankBlockItem;
import com.mythicrpg.building.BuildingBlockCatalog;
import com.mythicrpg.building.StaticDecorationBlock;
import com.mythicrpg.building.StaticDecorationItem;
import com.mythicrpg.crafting.InfiniteCraftingTableBlock;
import com.mythicrpg.crafting.LuckyBlock;
import com.mythicrpg.crafting.LuckyBlockItem;
import com.mythicrpg.eating.CookingPotBlock;
import com.mythicrpg.eating.FridgeBlock;
import com.mythicrpg.fishing.FishNetBlock;
import com.mythicrpg.fishing.FisheryTableBlock;
import com.mythicrpg.mining.archaeology.FossilBlock;
import com.mythicrpg.mining.archaeology.FossilIncubatorBlock;
import com.mythicrpg.mining.archaeology.relic.GrowthTotemBlock;
import com.mythicrpg.mining.archaeology.relic.FossilDrillBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public final class ModBlocks {

    public static final Block ENCHANTED_WOOD = registerBlock(
            "enchanted_wood",
            new PillarBlock(
                    AbstractBlock.Settings.copy(Blocks.OAK_LOG)
                            .strength(3.0f)
                            .sounds(BlockSoundGroup.WOOD)
                            .luminance(state -> 6)
            ),
            List.of(
                    MythicTooltipItem.line("tooltip.mythicrpg.enchanted_wood.description", Formatting.GRAY),
                    MythicTooltipItem.line("tooltip.mythicrpg.enchanted_wood.use", Formatting.GREEN)
            )
    );

    public static final Block INFINITE_CRAFTING_TABLE = registerBlock(
            "infinite_crafting_table",
            new InfiniteCraftingTableBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE)
                    .strength(2.5f, 6.0f)),
            List.of(
                    MythicTooltipItem.line("tooltip.mythicrpg.infinite_crafting_table.description", Formatting.GRAY),
                    MythicTooltipItem.line("tooltip.mythicrpg.infinite_crafting_table.use", Formatting.GREEN),
                    MythicTooltipItem.line("tooltip.mythicrpg.infinite_crafting_table.durability", Formatting.YELLOW)
            )
    );

    public static final Block LUCKY_BLOCK = registerLuckyBlock(
            "lucky_block",
            new LuckyBlock(AbstractBlock.Settings.copy(Blocks.GOLD_BLOCK)
                    .strength(1.5f, 6.0f))
    );

    public static final Block COOKING_POT = registerBlock(
            "cooking_pot",
            new CookingPotBlock(AbstractBlock.Settings.copy(Blocks.CAULDRON)
                    .strength(2.0F, 6.0F)
                    .sounds(BlockSoundGroup.METAL)),
            List.of(
                    MythicTooltipItem.line("tooltip.mythicrpg.cooking_pot.description", Formatting.GRAY),
                    MythicTooltipItem.line("tooltip.mythicrpg.cooking_pot.heat", Formatting.GOLD)
            )
    );

    public static final Block FRIDGE = registerBlock(
            "fridge",
            new FridgeBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
                    .strength(3.5F, 8.0F)
                    .sounds(BlockSoundGroup.METAL)),
            List.of(
                    MythicTooltipItem.line("tooltip.mythicrpg.fridge.description", Formatting.GRAY),
                    MythicTooltipItem.line("tooltip.mythicrpg.fridge.redstone", Formatting.RED),
                    MythicTooltipItem.line("tooltip.mythicrpg.fridge.food_only", Formatting.AQUA)
            )
    );

    public static final Block FOSSIL_BLOCK = registerBlock(
            "fossil_block",
            new FossilBlock(AbstractBlock.Settings.copy(Blocks.STONE)
                    .strength(50.0f, 1200.0f)
                    .requiresTool()),
            List.of(
                    MythicTooltipItem.line("tooltip.mythicrpg.fossil_block.description", Formatting.GRAY),
                    MythicTooltipItem.line("tooltip.mythicrpg.fossil_block.brush", Formatting.AQUA)
            )
    );

    public static final Block FOSSIL_INCUBATOR = registerBlock(
            "fossil_incubator",
            new FossilIncubatorBlock(AbstractBlock.Settings.copy(Blocks.COPPER_BLOCK)
                    .strength(3.5f, 1200.0f)),
            List.of(
                    MythicTooltipItem.line("tooltip.mythicrpg.fossil_incubator.description", Formatting.GRAY),
                    MythicTooltipItem.line("tooltip.mythicrpg.fossil_incubator.use", Formatting.GREEN)
            )
    );

    public static final Block BUILDING_RESERVE_CHEST = registerBlock(
            "building_reserve_chest",
            new BuildingReserveChestBlock(AbstractBlock.Settings.copy(Blocks.BARREL)
                    .strength(2.5f, 6.0f)),
            List.of(
                    MythicTooltipItem.line("tooltip.mythicrpg.building_reserve_chest.description", Formatting.GRAY),
                    MythicTooltipItem.line("tooltip.mythicrpg.building_reserve_chest.use", Formatting.GREEN),
                    MythicTooltipItem.line("tooltip.mythicrpg.building_reserve_chest.owner", Formatting.YELLOW)
            )
    );

    public static final Block BLANK_BLOCK = registerBlankBlock(
            "blank_block",
            new BlankBlock(AbstractBlock.Settings.copy(Blocks.WHITE_CONCRETE)
                    .strength(1.8f, 6.0f))
    );

    public static final Block STATIC_DECORATION = registerStaticDecoration(
            "static_decoration_generator",
            new StaticDecorationBlock(AbstractBlock.Settings.create()
                    .noCollision()
                    .nonOpaque()
                    .strength(0.1f)
                    .sounds(BlockSoundGroup.GLASS))
    );


    public static final Block GROWTH_TOTEM_BLOCK = registerBareBlock("growth_totem_block", new GrowthTotemBlock(AbstractBlock.Settings.copy(Blocks.BONE_BLOCK).strength(3.0f)));
    public static final Block FOSSIL_DRILL_BLOCK = registerBareBlock("fossil_drill_block", new FossilDrillBlock(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK).strength(5.0f)));


    public static final Block FISH_NET = registerBlock("fish_net", new FishNetBlock(AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR).strength(1.5F).nonOpaque()), List.of());
    public static final Block FISHERY_TABLE = registerBlock("fishery_table", new FisheryTableBlock(AbstractBlock.Settings.copy(Blocks.CRAFTING_TABLE).strength(2.5F)), List.of());

    private ModBlocks() {
    }

    private static Block registerBlock(
            String name,
            Block block,
            List<MythicTooltipItem.TooltipLine> tooltipLines
    ) {
        Identifier id = Identifier.of(MythicRPG.MOD_ID, name);

        Registry.register(Registries.BLOCK, id, block);

        Item.Settings itemSettings = new Item.Settings()
                .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);

        BlockItem blockItem = tooltipLines.isEmpty()
                ? new BlockItem(block, itemSettings)
                : new MythicTooltipBlockItem(block, itemSettings, tooltipLines);

        Registry.register(Registries.ITEM, id, blockItem);

        return block;
    }

    private static Block registerBlankBlock(String name, Block block) {
        Identifier id = Identifier.of(MythicRPG.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlankBlockItem(block, new Item.Settings()));
        BuildingBlockCatalog.registerCustom(block, 5);
        return block;
    }

    private static Block registerStaticDecoration(String name, Block block) {
        Identifier id = Identifier.of(MythicRPG.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new StaticDecorationItem(block, new Item.Settings().maxCount(1)));
        return block;
    }

    private static Block registerBareBlock(String name, Block block) { return Registry.register(Registries.BLOCK, Identifier.of(MythicRPG.MOD_ID, name), block); }

    private static Block registerLuckyBlock(String name, Block block) {
        Identifier id = Identifier.of(MythicRPG.MOD_ID, name);

        Registry.register(
                Registries.ITEM,
                id,
                new LuckyBlockItem(
                        block,
                        new Item.Settings()
                                .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
                )
        );

        return Registry.register(
                Registries.BLOCK,
                id,
                block
        );
    }

    public static void register() {
        MythicRPG.LOGGER.info("Registering MythicRPG blocks");
        VerticalSlabRegistry.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries ->
                entries.add(ENCHANTED_WOOD.asItem())
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries ->
                entries.add(BLANK_BLOCK.asItem())
        );

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(INFINITE_CRAFTING_TABLE.asItem());
            entries.add(COOKING_POT.asItem());
            entries.add(FRIDGE.asItem());
            entries.add(LUCKY_BLOCK.asItem());
            entries.add(FOSSIL_INCUBATOR.asItem());
            entries.add(FOSSIL_BLOCK.asItem());
            entries.add(BUILDING_RESERVE_CHEST.asItem());
            entries.add(STATIC_DECORATION.asItem());
            entries.add(FISH_NET.asItem());
            entries.add(FISHERY_TABLE.asItem());
        });
    }
}
