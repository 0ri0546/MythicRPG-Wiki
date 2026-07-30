package com.mythicrpg.mining.archaeology;

import com.mythicrpg.MythicRPG;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;

public final class FossilWorldGeneration {

    private static final Identifier FEATURE_ID = Identifier.of(MythicRPG.MOD_ID, "fossil_site");
    private static final RegistryKey<PlacedFeature> PLACED_FEATURE_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE,
            FEATURE_ID
    );

    public static final Feature<?> FOSSIL_SITE_FEATURE = Registry.register(
            Registries.FEATURE,
            FEATURE_ID,
            new FossilSiteFeature()
    );

    private FossilWorldGeneration() {
    }

    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Feature.UNDERGROUND_ORES,
                PLACED_FEATURE_KEY
        );
        MythicRPG.LOGGER.info("Registering MythicRPG fossil site world generation");
    }
}
