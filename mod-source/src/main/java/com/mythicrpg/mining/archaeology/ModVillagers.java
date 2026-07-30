package com.mythicrpg.mining.archaeology;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.core.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.villager.VillagerProfessionBuilder;
import net.fabricmc.fabric.api.object.builder.v1.world.poi.PointOfInterestHelper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.village.VillagerProfession;
import net.minecraft.world.poi.PointOfInterestType;

public final class ModVillagers {

    public static final Identifier ARCHAEOLOGIST_ID = Identifier.of(MythicRPG.MOD_ID, "archaeologist");
    public static final RegistryKey<PointOfInterestType> ARCHAEOLOGIST_POI_KEY = RegistryKey.of(
            RegistryKeys.POINT_OF_INTEREST_TYPE,
            Identifier.of(MythicRPG.MOD_ID, "archaeologist_incubator")
    );

    public static final PointOfInterestType ARCHAEOLOGIST_POI = PointOfInterestHelper.register(
            ARCHAEOLOGIST_POI_KEY.getValue(),
            1,
            1,
            ModBlocks.FOSSIL_INCUBATOR
    );

    @SuppressWarnings("deprecation")
    public static final VillagerProfession ARCHAEOLOGIST = Registry.register(
            Registries.VILLAGER_PROFESSION,
            ARCHAEOLOGIST_ID,
            VillagerProfessionBuilder.create()
                    .id(ARCHAEOLOGIST_ID)
                    .workstation(ARCHAEOLOGIST_POI_KEY)
                    .workSound(SoundEvents.ENTITY_VILLAGER_WORK_LIBRARIAN)
                    .build()
    );

    private ModVillagers() {
    }

    public static void register() {
        MythicRPG.LOGGER.info("Registering MythicRPG archaeologist profession");
        // Static fields perform registry insertion.
    }
}
