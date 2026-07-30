package com.mythicrpg.core;

public enum BonusType {
    // Mining
    DROP_MULTIPLIER(BonusAggregation.SUM),
    XP_MULTIPLIER(BonusAggregation.SUM),
    ORE_HIGHLIGHT_RADIUS(BonusAggregation.MAX),
    VEIN_MINING(BonusAggregation.MAX),
    MINING_3X3(BonusAggregation.MAX),
    NO_FALL_DAMAGE(BonusAggregation.MAX),
    NO_DURABILITY_LOSS(BonusAggregation.MAX),
    FOSSIL_EXCAVATION(BonusAggregation.MAX),
    FOSSIL_INCUBATION(BonusAggregation.MAX),
    FOSSIL_ARCHAEOLOGIST(BonusAggregation.MAX),

    // Fighting
    HIT_GLOWING(BonusAggregation.MAX),
    DOUBLE_LOOT_CHANCE(BonusAggregation.SUM),
    SWORD_REACH(BonusAggregation.SUM),
    ATTACK_COOLDOWN_MULTIPLIER(BonusAggregation.MIN),
    UNDEAD_DAMAGE(BonusAggregation.SUM),
    SPIDER_DAMAGE(BonusAggregation.SUM),
    MOB_XP_MULTIPLIER(BonusAggregation.MAX),

    // Woodcutting
    WOOD_DOUBLE_DROP_CHANCE(BonusAggregation.SUM),
    ENCHANTED_WOOD_CHANCE(BonusAggregation.SUM),
    WOOD_VANILLA_XP(BonusAggregation.MAX),
    RANDOM_SAPLING_DROP_CHANCE(BonusAggregation.SUM),
    LEAF_APPLE_DROP(BonusAggregation.MAX),
    LEAF_GOLDEN_APPLE_CHANCE(BonusAggregation.SUM),
    TIMBER(BonusAggregation.MAX),
    AXE_NO_DURABILITY(BonusAggregation.MAX),
    ENCHANTED_AXE_CRAFT(BonusAggregation.MAX),
    WOOD_EATER(BonusAggregation.MAX),
    GROWTH_CRAFT(BonusAggregation.MAX),
    TREE_GROWTH(BonusAggregation.MAX),
    CHEST_MODULE_I_CRAFT(BonusAggregation.MAX),
    CHEST_MODULE_II_CRAFT(BonusAggregation.MAX),
    CHEST_MODULE_III_CRAFT(BonusAggregation.MAX),

    // Farming
    ENCHANTED_SEED_CHANCE(BonusAggregation.SUM),
    AUTO_REPLANT(BonusAggregation.MAX),
    FARMING_DOUBLE_DROP_CHANCE(BonusAggregation.SUM),
    COMPOST_RARE_DROP_CHANCE(BonusAggregation.MAX),
    FOOD_BACKPACK_CRAFT(BonusAggregation.MAX),
    FARMING_VANILLA_XP(BonusAggregation.MAX),
    BONE_MEAL_REGEN(BonusAggregation.MAX),
    IRRIGATED_STEP(BonusAggregation.MAX),
    CULTIVATED_SHIELD(BonusAggregation.MAX),
    FARMER_REACH_RADIUS(BonusAggregation.MAX),
    ENCHANTED_FLOWER_CRAFT(BonusAggregation.MAX),
    PRESERVED_FARMER(BonusAggregation.MAX),
    LIVING_FIELD(BonusAggregation.MAX),

    // Crafting
    CRAFT_PORTABLE_TABLE(BonusAggregation.MAX),
    CRAFT_RESOURCE_SAVE_CHANCE(BonusAggregation.SUM),
    REPAIR_KIT_CRAFT(BonusAggregation.MAX),
    REPAIR_KIT_POWER(BonusAggregation.SUM),
    LUCKY_BLOCK_CRAFT(BonusAggregation.MAX),
    INFINITE_CRAFTING_TABLE_CRAFT(BonusAggregation.MAX),
    EXP_CHARM_CRAFT(BonusAggregation.MAX),
    CRAFT_CHARGE(BonusAggregation.MAX),
    CRAFT_VANILLA_XP(BonusAggregation.MAX),
    MYTHIC_INSPIRATION(BonusAggregation.MAX),
    FIRST_CRAFT_BONUS(BonusAggregation.MAX),
    RECYCLE_CRAFTS(BonusAggregation.MAX),
    MIDNIGHT_CRAFTING(BonusAggregation.MAX),
    REINFORCED_CRAFT_CHANCE(BonusAggregation.MAX),
    LUCKY_INFUSION(BonusAggregation.MAX),
    TRANSFORMATION_SLOT(BonusAggregation.MAX),
    CRAFT_MASTERY(BonusAggregation.MAX),

    // Traveling
    TRAVEL_DOUBLE_JUMP(BonusAggregation.MAX),
    TRAVEL_SOUL_WALKER(BonusAggregation.MAX),
    TRAVEL_DOLPHINS_GRACE(BonusAggregation.MAX),
    TRAVEL_POWDER_WALKER(BonusAggregation.MAX),
    TRAVEL_XP_MULTIPLIER(BonusAggregation.SUM),
    TRAVEL_DISCOVERY_XP_MULTIPLIER(BonusAggregation.SUM),
    TRAVEL_MINIATURIZATION(BonusAggregation.MAX),
    MONUMENTAL_COMPASS_CRAFT(BonusAggregation.MAX),
    STRUCTURE_MODULES_OVERWORLD(BonusAggregation.MAX),
    STRUCTURE_MODULES_NETHER_END(BonusAggregation.MAX),
    TRAVEL_DEATH_RECALL(BonusAggregation.MAX),
    TRAVEL_BOOTS_NO_DURABILITY(BonusAggregation.MAX),
    TRAVEL_BIOME_SPEED(BonusAggregation.MAX),
    FAST_MINECART_CRAFT(BonusAggregation.MAX),
    FAST_BOAT_CRAFT(BonusAggregation.MAX),
    LAND_MOUNTS(BonusAggregation.MAX),
    TREASURE_VANILLA_XP(BonusAggregation.MAX),
    FLYING_MOUNTS(BonusAggregation.MAX),

    // Building
    BUILD_QUICK_REPLACE(BonusAggregation.MAX),
    BUILD_PLAN_2D_8(BonusAggregation.MAX),
    BUILD_PLAN_2D_12(BonusAggregation.MAX),
    BUILD_PLAN_3D(BonusAggregation.MAX),
    BUILD_AUTO_RESTOCK(BonusAggregation.MAX),
    BUILD_DECORATIVE_MAGNET(BonusAggregation.MAX),
    BUILD_NO_TOOL_DURABILITY(BonusAggregation.MAX),
    BUILD_VERTICAL_SLABS(BonusAggregation.MAX),
    BUILD_REACH(BonusAggregation.MAX),
    BUILD_SCAFFOLDING_RANGE(BonusAggregation.MAX),
    BUILD_BLANK_BLOCK(BonusAggregation.MAX),
    BUILD_ARCHITECT_COMPASS(BonusAggregation.MAX),
    BUILD_RESERVE_RANGE(BonusAggregation.MAX),
    BUILD_MINIATURE(BonusAggregation.MAX),
    BUILD_STATIC_DECORATION(BonusAggregation.MAX),
    BUILD_WAND(BonusAggregation.MAX),

    // Fishing
    FISHING_CUSTOM_ROD(BonusAggregation.MAX),
    FISHING_WEATHER_RAIN(BonusAggregation.MAX),
    FISHING_WEATHER_SUN(BonusAggregation.MAX),
    FISHING_WEATHER_STORM(BonusAggregation.MAX),
    FISHING_BAIT_I(BonusAggregation.MAX),
    FISHING_BAIT_II(BonusAggregation.MAX),
    FISHING_BAIT_III(BonusAggregation.MAX),
    FISHING_RUNE_SLOTS(BonusAggregation.MAX),
    FISHING_RUNE_RARITY(BonusAggregation.MAX),
    FISHING_RUNE_SPEED(BonusAggregation.MAX),
    FISHING_RUNE_MASTERY(BonusAggregation.MAX),
    FISHING_NET_3(BonusAggregation.MAX),
    FISHING_NET_4(BonusAggregation.MAX),
    FISHING_NET_5(BonusAggregation.MAX),
    FISHING_FISHERY_TABLE(BonusAggregation.MAX),
    FISHING_SCALE_ARMOR(BonusAggregation.MAX),
    FISHING_BOAT(BonusAggregation.MAX),
    FISHING_LEGENDARY_BAIT(BonusAggregation.MAX),
    FISHING_BASALT_ROD(BonusAggregation.MAX),
    FISHING_VOID_ROD(BonusAggregation.MAX),

    // Eating
    EATING_COOKING(BonusAggregation.MAX),
    EATING_POT_SLOTS(BonusAggregation.SUM),
    EATING_SMALL_PLATE(BonusAggregation.MAX),
    EATING_MEDIUM_PLATE(BonusAggregation.MAX),
    EATING_LARGE_PLATE(BonusAggregation.MAX),
    EATING_FRIDGE(BonusAggregation.MAX),
    EATING_WHEN_FULL(BonusAggregation.MAX),
    EATING_CHEF_AURA(BonusAggregation.MAX),
    EATING_COMPLETE_MEAL(BonusAggregation.MAX),
    EATING_RISK_TASTE(BonusAggregation.MAX),
    EATING_DELIVERY(BonusAggregation.MAX),
    EATING_INTERNATIONAL_GASTRONOMY(BonusAggregation.MAX),
    EATING_DUBIOUS_COMPOST(BonusAggregation.MAX),
    EATING_CHEF_RENOWN(BonusAggregation.MAX),
    EATING_RARITY_UP(BonusAggregation.MAX),
    EATING_SIGNATURE_DISH(BonusAggregation.MAX),
    EATING_PORTABLE_FRIDGE(BonusAggregation.MAX),
    EATING_AUTO_FEED(BonusAggregation.MAX),

    ELYTRA_UPDRAFT(BonusAggregation.MAX), // Legacy value kept for old saves/source compatibility.
    GRAPPLING_HOOK_CRAFT(BonusAggregation.MAX),
    ;

    private final BonusAggregation aggregation;

    BonusType(BonusAggregation aggregation) {
        this.aggregation = aggregation;
    }

    public BonusAggregation aggregation() {
        return aggregation;
    }
}
