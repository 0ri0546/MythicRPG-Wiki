package com.mythicrpg.core;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.building.ArchitectCompassItem;
import com.mythicrpg.building.BuilderWandItem;
import com.mythicrpg.building.BuildingPlan2DItem;
import com.mythicrpg.building.BuildingPlan3DItem;
import com.mythicrpg.building.BuildingMiniatureProjectItem;
import com.mythicrpg.crafting.CoinTossPotionItem;
import com.mythicrpg.crafting.RepairKitItem;
import com.mythicrpg.fighting.items.BaronsDollItem;
import com.mythicrpg.fighting.items.FireWandItem;
import com.mythicrpg.fighting.items.HeartOfTheBeamItem;
import com.mythicrpg.fighting.items.LegendaryShieldItem;
import com.mythicrpg.fighting.items.SpiderWandItem;
import com.mythicrpg.traveling.AdoptionSaddleItem;
import com.mythicrpg.traveling.DeathRecallTokenItem;
import com.mythicrpg.traveling.LandMountType;
import com.mythicrpg.traveling.MiniaturizationCharmItem;
import com.mythicrpg.traveling.GrapplingHookItem;
import com.mythicrpg.traveling.StructureModuleItem;
import com.mythicrpg.traveling.StructureModuleRegistry;
import com.mythicrpg.traveling.TravelerBoatItem;
import com.mythicrpg.traveling.TravelerMinecartItem;
import com.mythicrpg.traveling.TravelerVehicleDispenserBehaviors;
import com.mythicrpg.woodcutting.ChestModuleItem;
import com.mythicrpg.fishing.*;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import com.mythicrpg.mining.archaeology.FossilFamily;
import com.mythicrpg.mining.archaeology.FossilItem;
import com.mythicrpg.mining.archaeology.FossilRarity;
import com.mythicrpg.mining.archaeology.FossilSkeletonItem;
import com.mythicrpg.mining.archaeology.ExpeditionDossierItem;
import com.mythicrpg.mining.archaeology.relic.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ToolMaterials;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import com.mythicrpg.farming.FoodBackpackItem;
import com.mythicrpg.eating.PreparedDishItem;
import com.mythicrpg.eating.ServingPlateItem;
import com.mythicrpg.eating.DeliveryPhoneItem;
import com.mythicrpg.eating.ChefNotebookItem;
import net.minecraft.component.type.FoodComponent;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ModItems {

    public static final Item PREPARED_DISH = registerItem(
            "prepared_dish",
            new PreparedDishItem(
                    new Item.Settings()
                            .maxCount(1)
                            .food(new FoodComponent.Builder()
                                    .nutrition(3)
                                    .saturationModifier(0.2F)
                                    .build())
            )
    );

    public static final Item SIGNATURE_DISH = registerItem(
            "signature_dish",
            new PreparedDishItem(
                    new Item.Settings()
                            .maxCount(1)
                            .food(new FoodComponent.Builder()
                                    .nutrition(3)
                                    .saturationModifier(0.2F)
                                    .build())
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item DELIVERY_PHONE = registerItem(
            "delivery_phone",
            new DeliveryPhoneItem(mythicSettings("item.mythicrpg.delivery_phone").maxCount(1))
    );

    public static final Item CHEF_NOTEBOOK = registerItem(
            "chef_notebook",
            new ChefNotebookItem(mythicSettings("item.mythicrpg.chef_notebook").maxCount(1))
    );

    public static final Item SMALL_PLATE = registerItem(
            "small_plate",
            new ServingPlateItem(3, new Item.Settings().maxCount(1))
    );

    public static final Item MEDIUM_PLATE = registerItem(
            "medium_plate",
            new ServingPlateItem(4, new Item.Settings().maxCount(1))
    );

    public static final Item LARGE_PLATE = registerItem(
            "large_plate",
            new ServingPlateItem(5, new Item.Settings().maxCount(1))
    );

    public static final Item CHEST_MODULE_I = registerItem(
            "chest_module_1",
            new ChestModuleItem(
                    9,
                    mythicSettings("item.mythicrpg.chest_module_1")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item CHEST_MODULE_II = registerItem(
            "chest_module_2",
            new ChestModuleItem(
                    18,
                    mythicSettings("item.mythicrpg.chest_module_2")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item CHEST_MODULE_III = registerItem(
            "chest_module_3",
            new ChestModuleItem(
                    27,
                    mythicSettings("item.mythicrpg.chest_module_3")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item ENCHANTED_AXE = registerItem(
            "enchanted_axe",
            new AxeItem(
                    ToolMaterials.NETHERITE,
                    mythicSettings("item.mythicrpg.enchanted_axe")
                            .maxDamage(ToolMaterials.GOLD.getDurability())
                            .attributeModifiers(
                                    AxeItem.createAttributeModifiers(
                                            ToolMaterials.NETHERITE,
                                            8.0f,
                                            -2.8f
                                    )
                            )
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item ENCHANTED_SEED = registerItem(
            "enchanted_seed",
            new MythicTooltipItem(
                    mythicSettings("item.mythicrpg.enchanted_seed")
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true),
                    List.of(
                            MythicTooltipItem.line("tooltip.mythicrpg.enchanted_seed.description", Formatting.GRAY),
                            MythicTooltipItem.line("tooltip.mythicrpg.enchanted_seed.use", Formatting.GREEN)
                    )
            )
    );

    public static final Item FOOD_BACKPACK = registerItem(
            "food_backpack",
            new FoodBackpackItem(mythicSettings("item.mythicrpg.food_backpack")
                    .maxCount(1).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true))
    );

    public static final Item ENCHANTED_FLOWER = registerItem(
            "enchanted_flower",
            new MythicTooltipItem(
                    mythicSettings("item.mythicrpg.enchanted_flower")
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true),
                    List.of(
                            MythicTooltipItem.line("tooltip.mythicrpg.enchanted_flower.description", Formatting.GRAY),
                            MythicTooltipItem.line("tooltip.mythicrpg.enchanted_flower.offhand", Formatting.GREEN),
                            MythicTooltipItem.line("tooltip.mythicrpg.enchanted_flower.cost", Formatting.YELLOW),
                            MythicTooltipItem.line("tooltip.mythicrpg.enchanted_flower.backpack", Formatting.DARK_AQUA)
                    )
            )
    );

    public static final Item REPAIR_KIT = registerItem(
            "repair_kit",
            new RepairKitItem(mythicSettings("item.mythicrpg.repair_kit")
                    .maxCount(16))
    );

    public static final Item EXP_CHARM = registerItem(
            "exp_charm",
            new MythicTooltipItem(
                    mythicSettings("item.mythicrpg.exp_charm")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true),
                    List.of(
                            MythicTooltipItem.line("tooltip.mythicrpg.exp_charm.description", Formatting.GRAY),
                            MythicTooltipItem.line("tooltip.mythicrpg.exp_charm.offhand", Formatting.GREEN)
                    )
            )
    );

    public static final Item COIN_TOSS_BLESSED = registerItem(
            "coin_toss_blessed",
            new CoinTossPotionItem(true, mythicSettings("item.mythicrpg.coin_toss_blessed")
                    .maxCount(16))
    );

    public static final Item COIN_TOSS_CURSED = registerItem(
            "coin_toss_cursed",
            new CoinTossPotionItem(false, mythicSettings("item.mythicrpg.coin_toss_cursed")
                    .maxCount(16))
    );

    public static final Item FIRE_WAND = registerItem(
            "fire_wand",
            new FireWandItem(
                    mythicSettings("item.mythicrpg.fire_wand")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item WITHER_SHIELD = registerItem(
            "wither_shield",
            new LegendaryShieldItem(
                    mythicSettings("item.mythicrpg.wither_shield")
                            .maxCount(1)
                            .maxDamage(336)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true),
                    "tooltip.mythicrpg.wither_shield.flavor"
            )
    );

    public static final Item HEART_OF_THE_BEAM = registerItem(
            "heart_of_the_beam",
            new HeartOfTheBeamItem(
                    mythicSettings("item.mythicrpg.heart_of_the_beam")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item SPIDER_WAND = registerItem(
            "spider_wand",
            new SpiderWandItem(
                    mythicSettings("item.mythicrpg.spider_wand")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );


    public static final Item MINIATURIZATION_CHARM = registerItem(
            "miniaturization_charm",
            new MiniaturizationCharmItem(
                    mythicSettings("item.mythicrpg.miniaturization_charm")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item DEATH_RECALL_TOKEN = registerItem(
            "death_recall_token",
            new DeathRecallTokenItem(
                    mythicSettings("item.mythicrpg.death_recall_token")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item STRUCTURE_MODULE = registerItem(
            "structure_module",
            new StructureModuleItem(
                    mythicSettings("item.mythicrpg.structure_module")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );


    public static final Item TRAVELER_MINECART = registerItem(
            "traveler_minecart",
            new TravelerMinecartItem(
                    mythicSettings("item.mythicrpg.traveler_minecart")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item TRAVELER_BOAT = registerItem(
            "traveler_boat",
            new TravelerBoatItem(
                    mythicSettings("item.mythicrpg.traveler_boat")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item COW_SADDLE = registerItem(
            "cow_saddle",
            new AdoptionSaddleItem(
                    LandMountType.COW,
                    mythicSettings("item.mythicrpg.cow_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item GOAT_SADDLE = registerItem(
            "goat_saddle",
            new AdoptionSaddleItem(
                    LandMountType.GOAT,
                    mythicSettings("item.mythicrpg.goat_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item SPIDER_SADDLE = registerItem(
            "spider_saddle",
            new AdoptionSaddleItem(
                    LandMountType.SPIDER,
                    mythicSettings("item.mythicrpg.spider_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item CHICKEN_SADDLE = registerItem(
            "chicken_saddle",
            new AdoptionSaddleItem(
                    LandMountType.CHICKEN,
                    mythicSettings("item.mythicrpg.chicken_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item MOOSHROOM_SADDLE = registerItem(
            "mooshroom_saddle",
            new AdoptionSaddleItem(
                    LandMountType.MOOSHROOM,
                    mythicSettings("item.mythicrpg.mooshroom_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item SHEEP_SADDLE = registerItem(
            "sheep_saddle",
            new AdoptionSaddleItem(
                    LandMountType.SHEEP,
                    mythicSettings("item.mythicrpg.sheep_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item PIG_SADDLE = registerItem(
            "pig_saddle",
            new AdoptionSaddleItem(
                    LandMountType.PIG,
                    mythicSettings("item.mythicrpg.pig_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item LLAMA_SADDLE = registerItem(
            "llama_saddle",
            new AdoptionSaddleItem(
                    LandMountType.LLAMA,
                    mythicSettings("item.mythicrpg.llama_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item PANDA_SADDLE = registerItem(
            "panda_saddle",
            new AdoptionSaddleItem(
                    LandMountType.PANDA,
                    mythicSettings("item.mythicrpg.panda_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );


    public static final Item SNOW_GOLEM_SADDLE = registerItem(
            "snow_golem_saddle",
            new AdoptionSaddleItem(
                    LandMountType.SNOW_GOLEM,
                    mythicSettings("item.mythicrpg.snow_golem_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item TURTLE_SADDLE = registerItem(
            "turtle_saddle",
            new AdoptionSaddleItem(
                    LandMountType.TURTLE,
                    mythicSettings("item.mythicrpg.turtle_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item POLAR_BEAR_SADDLE = registerItem(
            "polar_bear_saddle",
            new AdoptionSaddleItem(
                    LandMountType.POLAR_BEAR,
                    mythicSettings("item.mythicrpg.polar_bear_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item HOGLIN_SADDLE = registerItem(
            "hoglin_saddle",
            new AdoptionSaddleItem(
                    LandMountType.HOGLIN,
                    mythicSettings("item.mythicrpg.hoglin_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item RAVAGER_SADDLE = registerItem(
            "ravager_saddle",
            new AdoptionSaddleItem(
                    LandMountType.RAVAGER,
                    mythicSettings("item.mythicrpg.ravager_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item ZOGLIN_SADDLE = registerItem(
            "zoglin_saddle",
            new AdoptionSaddleItem(
                    LandMountType.ZOGLIN,
                    mythicSettings("item.mythicrpg.zoglin_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item VILLAGER_SADDLE = registerItem(
            "villager_saddle",
            new AdoptionSaddleItem(
                    LandMountType.VILLAGER,
                    mythicSettings("item.mythicrpg.villager_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item ENDERMAN_SADDLE = registerItem(
            "enderman_saddle",
            new AdoptionSaddleItem(
                    LandMountType.ENDERMAN,
                    mythicSettings("item.mythicrpg.enderman_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item BLAZE_SADDLE = registerItem(
            "blaze_saddle",
            new AdoptionSaddleItem(
                    LandMountType.BLAZE,
                    mythicSettings("item.mythicrpg.blaze_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item BREEZE_SADDLE = registerItem(
            "breeze_saddle",
            new AdoptionSaddleItem(
                    LandMountType.BREEZE,
                    mythicSettings("item.mythicrpg.breeze_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item GHAST_SADDLE = registerItem(
            "ghast_saddle",
            new AdoptionSaddleItem(
                    LandMountType.GHAST,
                    mythicSettings("item.mythicrpg.ghast_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item BEE_SADDLE = registerItem(
            "bee_saddle",
            new AdoptionSaddleItem(
                    LandMountType.BEE,
                    mythicSettings("item.mythicrpg.bee_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item PHANTOM_SADDLE = registerItem(
            "phantom_saddle",
            new AdoptionSaddleItem(
                    LandMountType.PHANTOM,
                    mythicSettings("item.mythicrpg.phantom_saddle")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item GRAPPLING_HOOK = registerItem(
            "grappling_hook",
            new GrapplingHookItem(
                    mythicSettings("item.mythicrpg.grappling_hook")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item BUILDING_PLAN_2D = registerItem(
            "building_plan_2d",
            new BuildingPlan2DItem(
                    mythicSettings("item.mythicrpg.building_plan_2d")
                            .maxCount(1)
            )
    );

    public static final Item BUILDING_PLAN_3D = registerItem(
            "building_plan_3d",
            new BuildingPlan3DItem(
                    mythicSettings("item.mythicrpg.building_plan_3d")
                            .maxCount(1)
            )
    );

    public static final Item ARCHITECT_COMPASS = registerItem(
            "architect_compass",
            new ArchitectCompassItem(
                    mythicSettings("item.mythicrpg.architect_compass")
                            .maxCount(1)
            )
    );

    public static final Item BUILDER_WAND = registerItem(
            "builder_wand",
            new BuilderWandItem(
                    mythicSettings("item.mythicrpg.builder_wand")
                            .maxCount(1)
                            .maxDamage(256)
            )
    );

    public static final Item BUILDING_MINIATURE_PROJECT = registerItem(
            "building_miniature_project",
            new BuildingMiniatureProjectItem(
                    mythicSettings("item.mythicrpg.building_miniature_project")
                            .maxCount(1)
            )
    );

    public static final Item BARONS_DOLL = registerItem(
            "barons_doll",
            new BaronsDollItem(
                    mythicSettings("item.mythicrpg.barons_doll")
                            .maxCount(1)
                            .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            )
    );

    public static final Item COLOSSAL_AEGIS = registerItem(
            "colossal_aegis",
            new ColossalAegisItem(mythicSettings("item.mythicrpg.colossal_aegis")
                    .maxCount(1).maxDamage(672)
                    .component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true))
    );

    public static final Item GROWTH_TOTEM = registerItem(
            "growth_totem",
            new GrowthTotemItem(mythicSettings("item.mythicrpg.growth_totem")
                    .maxCount(1).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true))
    );

    public static final Item FOSSIL_DRILL = registerItem(
            "fossil_drill",
            new FossilDrillItem(mythicSettings("item.mythicrpg.fossil_drill")
                    .maxCount(1).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true))
    );

    public static final Item TEMPORAL_MACHINE = registerItem(
            "temporal_machine",
            new TemporalMachineItem(mythicSettings("item.mythicrpg.temporal_machine")
                    .maxCount(1).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true))
    );

    public static final Item FOSSIL_PALETTE = registerItem(
            "fossil_palette",
            new FossilPaletteItem(mythicSettings("item.mythicrpg.fossil_palette")
                    .maxCount(1).component(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true))
    );

    public static final Item EXPEDITION_DOSSIER = registerItem(
            "expedition_dossier",
            new ExpeditionDossierItem(
                    new Item.Settings()
                            .maxCount(1)
                            .component(
                                    DataComponentTypes.ITEM_NAME,
                                    Text.translatable("item.mythicrpg.expedition_dossier")
                                            .formatted(Formatting.GOLD)
                            )
            )
    );

    private static final EnumMap<FossilFamily, EnumMap<FossilRarity, Item>> FOSSIL_ITEMS =
            new EnumMap<>(FossilFamily.class);
    private static final List<Item> FOSSIL_ITEMS_IN_DISPLAY_ORDER = registerFossilItems();

    private static final EnumMap<FossilFamily, EnumMap<FossilRarity, Item>> FOSSIL_SKELETON_ITEMS =
            new EnumMap<>(FossilFamily.class);
    private static final List<Item> FOSSIL_SKELETON_ITEMS_IN_DISPLAY_ORDER = registerFossilSkeletonItems();

    // Compatibility aliases retained for existing code and worlds.
    public static final Item SMALL_LAND_COMMON_FOSSIL = fossilItem(
            FossilFamily.SMALL_LAND,
            FossilRarity.COMMON
    ).orElseThrow();

    public static final Item SMALL_LAND_COMMON_SKELETON = fossilSkeletonItem(
            FossilFamily.SMALL_LAND,
            FossilRarity.COMMON
    ).orElseThrow();


    // Fishing V1
    public static final Item FISHING_CATCH = registerItem("fishing_catch", new FishingCatchItem(new Item.Settings().maxCount(1)));
    public static final Item MYTHIC_FISHING_ROD = registerItem("mythic_fishing_rod", new MythicFishingRodItem(null, new Item.Settings().maxCount(1).component(DataComponentTypes.UNBREAKABLE, new net.minecraft.component.type.UnbreakableComponent(true))));
    public static final Item BASALT_FISHING_ROD = registerItem("basalt_fishing_rod", new MythicFishingRodItem(FishingFamily.INFERNAL, new Item.Settings().maxCount(1).fireproof().component(DataComponentTypes.UNBREAKABLE, new net.minecraft.component.type.UnbreakableComponent(true))));
    public static final Item VOID_FISHING_ROD = registerItem("void_fishing_rod", new MythicFishingRodItem(FishingFamily.VOID, new Item.Settings().maxCount(1).component(DataComponentTypes.UNBREAKABLE, new net.minecraft.component.type.UnbreakableComponent(true))));
    public static final Item WEATHER_WAND = registerItem("fishing_weather_wand", new WeatherWandItem(new Item.Settings().maxCount(1)));
    public static final Item MEGALODON_TOOTH = registerItem("megalodon_tooth", new Item(new Item.Settings().maxCount(16).rarity(net.minecraft.util.Rarity.RARE)));
    public static final Item NESSIE_SCALE = registerItem("nessie_scale", new Item(new Item.Settings().maxCount(16).rarity(net.minecraft.util.Rarity.RARE)));
    public static final Item WHALE_AMBERGRIS = registerItem("whale_ambergris", new Item(new Item.Settings().maxCount(16).rarity(net.minecraft.util.Rarity.RARE)));
    public static final Item MEGALODON_CHARM = registerItem("megalodon_charm", new FishingCharmItem(FishingCharmItem.Kind.MEGALODON, new Item.Settings().maxDamage(256).rarity(net.minecraft.util.Rarity.EPIC)));
    public static final Item NESSIE_CHARM = registerItem("nessie_charm", new FishingCharmItem(FishingCharmItem.Kind.NESSIE, new Item.Settings().maxDamage(256).rarity(net.minecraft.util.Rarity.EPIC)));
    public static final Item WHALE_CHARM = registerItem("whale_charm", new FishingCharmItem(FishingCharmItem.Kind.WHALE, new Item.Settings().maxDamage(128).rarity(net.minecraft.util.Rarity.EPIC)));
    public static final Item BAIT_I = registerItem("fishing_bait_i", new FishingUpgradeItem(FishingUpgradeItem.Kind.BAIT_I, new Item.Settings().maxCount(16)));
    public static final Item BAIT_II = registerItem("fishing_bait_ii", new FishingUpgradeItem(FishingUpgradeItem.Kind.BAIT_II, new Item.Settings().maxCount(16)));
    public static final Item BAIT_III = registerItem("fishing_bait_iii", new FishingUpgradeItem(FishingUpgradeItem.Kind.BAIT_III, new Item.Settings().maxCount(16)));
    public static final Item BAIT_LEGENDARY = registerItem("fishing_bait_legendary", new FishingUpgradeItem(FishingUpgradeItem.Kind.BAIT_LEGENDARY, new Item.Settings().maxCount(16)));
    public static final Item RUNE_RARITY = registerItem("fishing_rune_rarity", new FishingUpgradeItem(FishingUpgradeItem.Kind.RUNE_RARITY, new Item.Settings().maxCount(1)));
    public static final Item RUNE_SPEED = registerItem("fishing_rune_speed", new FishingUpgradeItem(FishingUpgradeItem.Kind.RUNE_SPEED, new Item.Settings().maxCount(1)));
    public static final Item RUNE_MASTERY = registerItem("fishing_rune_mastery", new FishingUpgradeItem(FishingUpgradeItem.Kind.RUNE_MASTERY, new Item.Settings().maxCount(1)));
    public static final EnumMap<FishingRarity, Item> FISHING_SCALES = registerFishingMaterials(false);
    public static final EnumMap<FishingRarity, Item> FISHING_SHELLS = registerFishingMaterials(true);
    public static final Item FISHING_BOAT = registerItem("fishing_boat", new FishingBoatItem(new Item.Settings().maxCount(1)));
    public static final EnumMap<FishingRarity, EnumMap<ArmorItem.Type, Item>> FISHING_ARMOR = registerFishingArmor();

    private ModItems() {
    }

    public static Optional<Item> fossilItem(FossilFamily family, FossilRarity rarity) {
        Map<FossilRarity, Item> byRarity = FOSSIL_ITEMS.get(family);
        return byRarity == null ? Optional.empty() : Optional.ofNullable(byRarity.get(rarity));
    }

    public static List<Item> fossilItems() {
        return FOSSIL_ITEMS_IN_DISPLAY_ORDER;
    }

    public static Optional<Item> fossilSkeletonItem(FossilFamily family, FossilRarity rarity) {
        Map<FossilRarity, Item> byRarity = FOSSIL_SKELETON_ITEMS.get(family);
        return byRarity == null ? Optional.empty() : Optional.ofNullable(byRarity.get(rarity));
    }

    public static List<Item> fossilSkeletonItems() {
        return FOSSIL_SKELETON_ITEMS_IN_DISPLAY_ORDER;
    }

    private static List<Item> registerFossilItems() {
        ArrayList<Item> displayOrder = new ArrayList<>(
                FossilFamily.values().length * FossilRarity.values().length
        );

        for (FossilFamily family : FossilFamily.values()) {
            EnumMap<FossilRarity, Item> byRarity = new EnumMap<>(FossilRarity.class);
            FOSSIL_ITEMS.put(family, byRarity);

            for (FossilRarity rarity : FossilRarity.values()) {
                String itemId = family.id() + "_" + rarity.id() + "_fossil";
                Item item = registerItem(
                        itemId,
                        new FossilItem(
                                family,
                                rarity,
                                fossilSettings(itemId, rarity)
                        )
                );
                byRarity.put(rarity, item);
                displayOrder.add(item);
            }
        }

        return List.copyOf(displayOrder);
    }

    private static List<Item> registerFossilSkeletonItems() {
        ArrayList<Item> displayOrder = new ArrayList<>(
                FossilFamily.values().length * FossilRarity.values().length
        );

        for (FossilFamily family : FossilFamily.values()) {
            EnumMap<FossilRarity, Item> byRarity = new EnumMap<>(FossilRarity.class);
            FOSSIL_SKELETON_ITEMS.put(family, byRarity);

            for (FossilRarity rarity : FossilRarity.values()) {
                String itemId = family.id() + "_" + rarity.id() + "_skeleton";
                Item item = registerItem(
                        itemId,
                        new FossilSkeletonItem(
                                family,
                                rarity,
                                fossilSkeletonSettings(itemId, rarity)
                        )
                );
                byRarity.put(rarity, item);
                displayOrder.add(item);
            }
        }

        return List.copyOf(displayOrder);
    }

    private static Item.Settings fossilSettings(String itemId, FossilRarity rarity) {
        return new Item.Settings().component(
                DataComponentTypes.ITEM_NAME,
                Text.translatable("item.mythicrpg." + itemId).formatted(rarity.formatting())
        );
    }

    private static Item.Settings fossilSkeletonSettings(String itemId, FossilRarity rarity) {
        return new Item.Settings()
                .maxCount(1)
                .component(
                        DataComponentTypes.ITEM_NAME,
                        Text.translatable("item.mythicrpg." + itemId).formatted(rarity.formatting())
                );
    }

    public static Item fishingMaterial(FishingRarity rarity, boolean shell) {
        return (shell ? FISHING_SHELLS : FISHING_SCALES).get(rarity);
    }

    private static EnumMap<FishingRarity, Item> registerFishingMaterials(boolean shell) {
        EnumMap<FishingRarity, Item> result = new EnumMap<>(FishingRarity.class);
        for (FishingRarity rarity : FishingRarity.values()) {
            String id = "fishing_" + rarity.id() + (shell ? "_shell" : "_scale");
            result.put(rarity, registerItem(id, new FishingMaterialItem(rarity, shell, new Item.Settings())));
        }
        return result;
    }

    private static EnumMap<FishingRarity, EnumMap<ArmorItem.Type, Item>> registerFishingArmor() {
        EnumMap<FishingRarity, EnumMap<ArmorItem.Type, Item>> all = new EnumMap<>(FishingRarity.class);
        for (FishingRarity rarity : FishingRarity.values()) {
            EnumMap<ArmorItem.Type, Item> set = new EnumMap<>(ArmorItem.Type.class);
            for (ArmorItem.Type type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
                String id = "fishing_armor_" + rarity.name().toLowerCase(java.util.Locale.ROOT) + "_" + type.getName();
                Item item = registerItem(id, new FishingScaleArmorItem(ArmorMaterials.DIAMOND, type, rarity, new Item.Settings().maxCount(1)));
                set.put(type, item);
            }
            all.put(rarity, set);
        }
        return all;
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(
                Registries.ITEM,
                Identifier.of(MythicRPG.MOD_ID, name),
                item
        );
    }

    public static void register() {
        MythicRPG.LOGGER.info("Registering MythicRPG items");
        TravelerVehicleDispenserBehaviors.register();

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(ENCHANTED_SEED);
            entries.add(ENCHANTED_FLOWER);
            entries.add(CHEST_MODULE_I);
            entries.add(CHEST_MODULE_II);
            entries.add(CHEST_MODULE_III);
            FOSSIL_ITEMS_IN_DISPLAY_ORDER.forEach(entries::add);
            FOSSIL_SKELETON_ITEMS_IN_DISPLAY_ORDER.forEach(entries::add);
            entries.add(EXPEDITION_DOSSIER);
            entries.add(COLOSSAL_AEGIS);
            entries.add(GROWTH_TOTEM);
            entries.add(FOSSIL_DRILL);
            entries.add(TEMPORAL_MACHINE);
            entries.add(FOSSIL_PALETTE);
            entries.add(FISHING_CATCH); entries.add(BAIT_I); entries.add(BAIT_II); entries.add(BAIT_III); entries.add(BAIT_LEGENDARY);
            entries.add(MEGALODON_TOOTH); entries.add(NESSIE_SCALE); entries.add(WHALE_AMBERGRIS);
            entries.add(RUNE_RARITY); entries.add(RUNE_SPEED); entries.add(RUNE_MASTERY); FISHING_SCALES.values().forEach(entries::add); FISHING_SHELLS.values().forEach(entries::add);

            StructureModuleRegistry.values().forEach(definition ->
                    entries.add(StructureModuleItem.create(definition.id()))
            );
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(FOOD_BACKPACK); entries.add(MYTHIC_FISHING_ROD); entries.add(BASALT_FISHING_ROD); entries.add(VOID_FISHING_ROD); entries.add(WEATHER_WAND); entries.add(FISHING_BOAT);
            entries.add(MEGALODON_CHARM); entries.add(NESSIE_CHARM); entries.add(WHALE_CHARM);
            FISHING_ARMOR.values().forEach(set -> set.values().forEach(entries::add));
            entries.add(ENCHANTED_AXE);
            entries.add(REPAIR_KIT);
            entries.add(EXP_CHARM);
            entries.add(FIRE_WAND);
            entries.add(WITHER_SHIELD);
            entries.add(HEART_OF_THE_BEAM);
            entries.add(SPIDER_WAND);
            entries.add(BARONS_DOLL);
            entries.add(MINIATURIZATION_CHARM);
            entries.add(TRAVELER_MINECART);
            entries.add(TRAVELER_BOAT);
            entries.add(COW_SADDLE);
            entries.add(GOAT_SADDLE);
            entries.add(SPIDER_SADDLE);
            entries.add(CHICKEN_SADDLE);
            entries.add(MOOSHROOM_SADDLE);
            entries.add(SHEEP_SADDLE);
            entries.add(PIG_SADDLE);
            entries.add(LLAMA_SADDLE);
            entries.add(PANDA_SADDLE);
            entries.add(SNOW_GOLEM_SADDLE);
            entries.add(TURTLE_SADDLE);
            entries.add(POLAR_BEAR_SADDLE);
            entries.add(HOGLIN_SADDLE);
            entries.add(RAVAGER_SADDLE);
            entries.add(ZOGLIN_SADDLE);
            entries.add(VILLAGER_SADDLE);
            entries.add(ENDERMAN_SADDLE);
            entries.add(PHANTOM_SADDLE);
            entries.add(BLAZE_SADDLE);
            entries.add(BREEZE_SADDLE);
            entries.add(GHAST_SADDLE);
            entries.add(BEE_SADDLE);
            entries.add(GRAPPLING_HOOK);
            entries.add(BUILDING_PLAN_2D);
            entries.add(BUILDING_PLAN_3D);
            entries.add(ARCHITECT_COMPASS);
            entries.add(BUILDER_WAND);
            entries.add(BUILDING_MINIATURE_PROJECT);
            entries.add(DELIVERY_PHONE);
            entries.add(CHEF_NOTEBOOK);
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FOOD_AND_DRINK).register(entries -> {
            entries.add(ModItems.SMALL_PLATE);
            entries.add(ModItems.MEDIUM_PLATE);
            entries.add(ModItems.LARGE_PLATE);
            entries.add(ModItems.COIN_TOSS_BLESSED);
            entries.add(ModItems.COIN_TOSS_CURSED);
        });
    }

    private static Item.Settings mythicSettings(String translationKey) {
        return new Item.Settings()
                .component(
                        DataComponentTypes.ITEM_NAME,
                        Text.translatable(translationKey).formatted(Formatting.LIGHT_PURPLE)
                );
    }
}