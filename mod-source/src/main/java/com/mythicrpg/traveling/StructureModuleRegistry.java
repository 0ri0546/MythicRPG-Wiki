package com.mythicrpg.traveling;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class StructureModuleRegistry {

    private static final Map<String, StructureModuleDefinition> DEFINITIONS = new LinkedHashMap<>();

    static {
        register("village", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.VILLAGE_PLAINS,
                StructureKeys.VILLAGE_DESERT,
                StructureKeys.VILLAGE_SAVANNA,
                StructureKeys.VILLAGE_SNOWY,
                StructureKeys.VILLAGE_TAIGA);
        register("mineshaft", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.MINESHAFT,
                StructureKeys.MINESHAFT_MESA);
        register("stronghold", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.STRONGHOLD);
        register("pillager_outpost", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.PILLAGER_OUTPOST);
        register("woodland_mansion", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.MANSION);
        register("ocean_monument", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.MONUMENT);
        register("ancient_city", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.ANCIENT_CITY);
        register("trial_chambers", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.TRIAL_CHAMBERS);
        register("desert_pyramid", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.DESERT_PYRAMID);
        register("jungle_temple", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.JUNGLE_PYRAMID);
        register("swamp_hut", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.SWAMP_HUT);
        register("igloo", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.IGLOO);
        register("shipwreck", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.SHIPWRECK,
                StructureKeys.SHIPWRECK_BEACHED);
        register("ocean_ruin", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.OCEAN_RUIN_COLD,
                StructureKeys.OCEAN_RUIN_WARM);
        register("buried_treasure", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.BURIED_TREASURE);
        register("trail_ruins", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.TRAIL_RUINS);
        register("ruined_portal", StructureModuleDefinition.Realm.OVERWORLD,
                StructureKeys.RUINED_PORTAL,
                StructureKeys.RUINED_PORTAL_DESERT,
                StructureKeys.RUINED_PORTAL_JUNGLE,
                StructureKeys.RUINED_PORTAL_SWAMP,
                StructureKeys.RUINED_PORTAL_MOUNTAIN,
                StructureKeys.RUINED_PORTAL_OCEAN);

        register("nether_fortress", StructureModuleDefinition.Realm.NETHER,
                StructureKeys.FORTRESS);
        register("bastion_remnant", StructureModuleDefinition.Realm.NETHER,
                StructureKeys.BASTION_REMNANT);
        register("nether_fossil", StructureModuleDefinition.Realm.NETHER,
                StructureKeys.NETHER_FOSSIL);
        register("nether_ruined_portal", StructureModuleDefinition.Realm.NETHER,
                StructureKeys.RUINED_PORTAL_NETHER);

        register("end_city", StructureModuleDefinition.Realm.END,
                StructureKeys.END_CITY);
    }

    private StructureModuleRegistry() {
    }

    @SafeVarargs
    private static void register(
            String id,
            StructureModuleDefinition.Realm realm,
            RegistryKey<Structure>... structures
    ) {
        DEFINITIONS.put(id, new StructureModuleDefinition(
                id,
                "item.mythicrpg.structure_module." + id,
                realm,
                List.of(structures)
        ));
    }

    public static Optional<StructureModuleDefinition> get(String id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static Collection<StructureModuleDefinition> values() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static RegistryEntryList<Structure> resolve(
            ServerWorld world,
            StructureModuleDefinition definition
    ) {
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        List<RegistryEntry<Structure>> entries = new ArrayList<>();

        for (RegistryKey<Structure> structureKey : definition.structures()) {
            registry.getEntry(structureKey).ifPresent(entries::add);
        }

        return RegistryEntryList.of(entries);
    }

    public static RegistryEntryList<Structure> resolveAll(ServerWorld world) {
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        List<RegistryEntry<Structure>> entries = new ArrayList<>();
        registry.streamEntries().forEach(entries::add);

        return RegistryEntryList.of(entries);
    }
}
