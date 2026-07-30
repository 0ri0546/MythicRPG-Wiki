package com.mythicrpg.core;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.traveling.TravelerBoatEntity;
import com.mythicrpg.fishing.FishingBoatEntity;
import com.mythicrpg.traveling.TravelerMinecartEntity;
import com.mythicrpg.building.BuildingMiniatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModEntities {

    public static final EntityType<TravelerMinecartEntity> TRAVELER_MINECART =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(MythicRPG.MOD_ID, "traveler_minecart"),
                    EntityType.Builder
                            .<TravelerMinecartEntity>create(
                                    TravelerMinecartEntity::new,
                                    SpawnGroup.MISC
                            )
                            .dimensions(0.98F, 0.7F)
                            .passengerAttachments(0.1875F)
                            .maxTrackingRange(8)
                            // Le véhicule sérialise le même NBT qu'un minecart vanilla :
                            // on réutilise son schéma DFU au lieu de demander un schéma
                            // inexistant pour l'identifiant custom.
                            .build("minecart")
            );

    public static final EntityType<TravelerBoatEntity> TRAVELER_BOAT =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(MythicRPG.MOD_ID, "traveler_boat"),
                    EntityType.Builder
                            .<TravelerBoatEntity>create(
                                    TravelerBoatEntity::new,
                                    SpawnGroup.MISC
                            )
                            .dimensions(1.375F, 0.5625F)
                            .eyeHeight(0.5625F)
                            .maxTrackingRange(10)
                            // Même principe pour le bateau : schéma DFU vanilla,
                            // identifiant de registre MythicRPG conservé par Registry.register.
                            .build("boat")
            );

    public static final EntityType<BuildingMiniatureEntity> BUILDING_MINIATURE =
            Registry.register(
                    Registries.ENTITY_TYPE,
                    Identifier.of(MythicRPG.MOD_ID, "building_miniature"),
                    EntityType.Builder
                            .<BuildingMiniatureEntity>create(
                                    BuildingMiniatureEntity::new,
                                    SpawnGroup.MISC
                            )
                            .dimensions(1.0F, 0.9F)
                            .maxTrackingRange(3)
                            .trackingTickInterval(20)
                            .build("marker")
            );


    public static final EntityType<FishingBoatEntity> FISHING_BOAT = Registry.register(Registries.ENTITY_TYPE, Identifier.of(MythicRPG.MOD_ID, "fishing_boat"), EntityType.Builder.<FishingBoatEntity>create(FishingBoatEntity::new, SpawnGroup.MISC).dimensions(1.375F,0.5625F).eyeHeight(0.5625F).maxTrackingRange(10).build("boat"));

    private ModEntities() {
    }

    public static void register() {
        MythicRPG.LOGGER.info("Registering MythicRPG entities");
    }
}
