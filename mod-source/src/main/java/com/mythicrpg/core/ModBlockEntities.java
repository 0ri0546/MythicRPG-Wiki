package com.mythicrpg.core;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.mining.archaeology.FossilBlockEntity;
import com.mythicrpg.building.BuildingReserveChestBlockEntity;
import com.mythicrpg.building.BlankBlockEntity;
import com.mythicrpg.building.StaticDecorationBlockEntity;
import com.mythicrpg.mining.archaeology.FossilIncubatorBlockEntity;
import com.mythicrpg.mining.archaeology.relic.GrowthTotemBlockEntity;
import com.mythicrpg.mining.archaeology.relic.FossilDrillBlockEntity;
import com.mythicrpg.eating.CookingPotBlockEntity;
import com.mythicrpg.eating.FridgeBlockEntity;
import com.mythicrpg.fishing.FishNetBlockEntity;
import com.mythicrpg.fishing.FisheryTableBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {

    public static final BlockEntityType<CookingPotBlockEntity> COOKING_POT = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MythicRPG.MOD_ID, "cooking_pot"),
            FabricBlockEntityTypeBuilder.create(
                    CookingPotBlockEntity::new,
                    ModBlocks.COOKING_POT
            ).build()
    );

    public static final BlockEntityType<FridgeBlockEntity> FRIDGE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MythicRPG.MOD_ID, "fridge"),
            FabricBlockEntityTypeBuilder.create(
                    FridgeBlockEntity::new,
                    ModBlocks.FRIDGE
            ).build()
    );

    public static final BlockEntityType<FossilBlockEntity> FOSSIL = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MythicRPG.MOD_ID, "fossil"),
            FabricBlockEntityTypeBuilder.create(FossilBlockEntity::new, ModBlocks.FOSSIL_BLOCK).build()
    );

    public static final BlockEntityType<FossilIncubatorBlockEntity> FOSSIL_INCUBATOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MythicRPG.MOD_ID, "fossil_incubator"),
            FabricBlockEntityTypeBuilder.create(
                    FossilIncubatorBlockEntity::new,
                    ModBlocks.FOSSIL_INCUBATOR
            ).build()
    );

    public static final BlockEntityType<BuildingReserveChestBlockEntity> BUILDING_RESERVE_CHEST = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MythicRPG.MOD_ID, "building_reserve_chest"),
            FabricBlockEntityTypeBuilder.create(
                    BuildingReserveChestBlockEntity::new,
                    ModBlocks.BUILDING_RESERVE_CHEST
            ).build()
    );

    public static final BlockEntityType<BlankBlockEntity> BLANK_BLOCK = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MythicRPG.MOD_ID, "blank_block"),
            FabricBlockEntityTypeBuilder.create(
                    BlankBlockEntity::new,
                    ModBlocks.BLANK_BLOCK
            ).build()
    );

    public static final BlockEntityType<StaticDecorationBlockEntity> STATIC_DECORATION = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(MythicRPG.MOD_ID, "static_decoration_generator"),
            FabricBlockEntityTypeBuilder.create(
                    StaticDecorationBlockEntity::new,
                    ModBlocks.STATIC_DECORATION
            ).build()
    );

    public static final BlockEntityType<GrowthTotemBlockEntity> GROWTH_TOTEM = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MythicRPG.MOD_ID, "growth_totem"), FabricBlockEntityTypeBuilder.create(GrowthTotemBlockEntity::new, ModBlocks.GROWTH_TOTEM_BLOCK).build());
    public static final BlockEntityType<FossilDrillBlockEntity> FOSSIL_DRILL = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MythicRPG.MOD_ID, "fossil_drill"), FabricBlockEntityTypeBuilder.create(FossilDrillBlockEntity::new, ModBlocks.FOSSIL_DRILL_BLOCK).build());


    public static final BlockEntityType<FishNetBlockEntity> FISH_NET = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MythicRPG.MOD_ID, "fish_net"), FabricBlockEntityTypeBuilder.create(FishNetBlockEntity::new, ModBlocks.FISH_NET).build());
    public static final BlockEntityType<FisheryTableBlockEntity> FISHERY_TABLE = Registry.register(Registries.BLOCK_ENTITY_TYPE, Identifier.of(MythicRPG.MOD_ID, "fishery_table"), FabricBlockEntityTypeBuilder.create(FisheryTableBlockEntity::new, ModBlocks.FISHERY_TABLE).build());

    private ModBlockEntities() {
    }

    public static void register() {
        MythicRPG.LOGGER.info("Registering MythicRPG block entities");
    }
}
