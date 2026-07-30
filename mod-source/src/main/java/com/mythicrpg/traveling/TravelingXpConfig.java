package com.mythicrpg.traveling;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Central balancing file for every base Traveling XP reward.
 *
 * Values are intentionally provisional for Traveling V1 private testing.
 */
public final class TravelingXpConfig {

    private static final int MOVEMENT_XP = 5;
    private static final int TREASURE_CHEST_XP = 50;
    private static final int TREASURE_VANILLA_XP = 250;
    private static final int BIOME_SPEED_DURATION_TICKS = 10 * 20;

    private static final int NETHER_FIRST_VISIT_XP = 75;
    private static final int END_FIRST_VISIT_XP = 150;
    private static final int OTHER_DIMENSION_FIRST_VISIT_XP = 75;

    private static final double MOVEMENT_DISTANCE_REQUIRED = 100.0;
    private static final double MOVEMENT_DIRECT_DISTANCE_REQUIRED = 70.0;
    private static final int MOVEMENT_CELL_SIZE = 128;

    private static final Map<RegistryKey<Structure>, Integer> STRUCTURE_XP;

    static {
        Map<RegistryKey<Structure>, Integer> values = new HashMap<>();

        put(values, 35, StructureKeys.PILLAGER_OUTPOST);
        put(values, 25, StructureKeys.MINESHAFT, StructureKeys.MINESHAFT_MESA);
        put(values, 100, StructureKeys.MANSION);
        put(values, 40, StructureKeys.JUNGLE_PYRAMID, StructureKeys.DESERT_PYRAMID);
        put(values, 20, StructureKeys.IGLOO, StructureKeys.SWAMP_HUT);
        put(values, 25, StructureKeys.SHIPWRECK, StructureKeys.SHIPWRECK_BEACHED);
        put(values, 100, StructureKeys.STRONGHOLD);
        put(values, 75, StructureKeys.MONUMENT);
        put(values, 20, StructureKeys.OCEAN_RUIN_COLD, StructureKeys.OCEAN_RUIN_WARM);
        put(values, 60, StructureKeys.FORTRESS);
        put(values, 25, StructureKeys.NETHER_FOSSIL);
        put(values, 90, StructureKeys.END_CITY);
        put(values, 15, StructureKeys.BURIED_TREASURE);
        put(values, 75, StructureKeys.BASTION_REMNANT);
        put(values, 15,
                StructureKeys.VILLAGE_PLAINS,
                StructureKeys.VILLAGE_DESERT,
                StructureKeys.VILLAGE_SAVANNA,
                StructureKeys.VILLAGE_SNOWY,
                StructureKeys.VILLAGE_TAIGA
        );
        put(values, 15,
                StructureKeys.RUINED_PORTAL,
                StructureKeys.RUINED_PORTAL_DESERT,
                StructureKeys.RUINED_PORTAL_JUNGLE,
                StructureKeys.RUINED_PORTAL_SWAMP,
                StructureKeys.RUINED_PORTAL_MOUNTAIN,
                StructureKeys.RUINED_PORTAL_OCEAN,
                StructureKeys.RUINED_PORTAL_NETHER
        );
        put(values, 100, StructureKeys.ANCIENT_CITY);
        put(values, 30, StructureKeys.TRAIL_RUINS);
        put(values, 60, StructureKeys.TRIAL_CHAMBERS);

        STRUCTURE_XP = Collections.unmodifiableMap(values);
    }

    private TravelingXpConfig() {
    }

    public static int getMovementXp() {
        return MOVEMENT_XP;
    }

    public static int getTreasureChestXp() {
        return TREASURE_CHEST_XP;
    }

    public static int getTreasureVanillaXp() {
        return TREASURE_VANILLA_XP;
    }

    public static int getBiomeSpeedDurationTicks() {
        return BIOME_SPEED_DURATION_TICKS;
    }

    public static int getFirstVisitDimensionXp(RegistryKey<World> dimension) {
        if (dimension == World.NETHER) {
            return NETHER_FIRST_VISIT_XP;
        }
        if (dimension == World.END) {
            return END_FIRST_VISIT_XP;
        }
        if (dimension == World.OVERWORLD) {
            return 0;
        }
        return OTHER_DIMENSION_FIRST_VISIT_XP;
    }

    public static int getStructureXp(RegistryKey<Structure> structureKey) {
        return STRUCTURE_XP.getOrDefault(structureKey, 0);
    }

    public static boolean rewardsStructure(RegistryKey<Structure> structureKey) {
        return STRUCTURE_XP.containsKey(structureKey);
    }

    public static Map<RegistryKey<Structure>, Integer> getStructureXpValues() {
        return STRUCTURE_XP;
    }

    public static double getMovementDistanceRequired() {
        return MOVEMENT_DISTANCE_REQUIRED;
    }

    public static double getMovementDirectDistanceRequired() {
        return MOVEMENT_DIRECT_DISTANCE_REQUIRED;
    }

    public static int getMovementCellSize() {
        return MOVEMENT_CELL_SIZE;
    }

    public static Identifier getOverworldId() {
        return World.OVERWORLD.getValue();
    }

    @SafeVarargs
    private static void put(
            Map<RegistryKey<Structure>, Integer> values,
            int xp,
            RegistryKey<Structure>... structures
    ) {
        for (RegistryKey<Structure> structure : structures) {
            values.put(structure, xp);
        }
    }
}
