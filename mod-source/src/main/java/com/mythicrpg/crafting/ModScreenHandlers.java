package com.mythicrpg.crafting;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.traveling.TravelingCompassScreenHandler;
import com.mythicrpg.mining.archaeology.FossilIncubatorScreenHandler;
import com.mythicrpg.mining.archaeology.ArchaeologistScreenHandler;
import com.mythicrpg.eating.CookingPotScreenHandler;
import com.mythicrpg.fishing.FishNetScreenHandler;
import com.mythicrpg.fishing.FisheryTableScreenHandler;
import com.mythicrpg.fishing.FishingBoatScreenHandler;
import com.mythicrpg.fishing.FishingRodScreenHandler;
import com.mythicrpg.woodcutting.chest.ModularChestScreenHandler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {

    public static final ScreenHandlerType<ModularChestScreenHandler> MODULAR_CHEST_SINGLE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "modular_chest_single"),
            new ScreenHandlerType<>(ModularChestScreenHandler::createSingleClient, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<ModularChestScreenHandler> MODULAR_CHEST_DOUBLE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "modular_chest_double"),
            new ScreenHandlerType<>(ModularChestScreenHandler::createDoubleClient, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<CookingPotScreenHandler> COOKING_POT = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "cooking_pot"),
            new ScreenHandlerType<>(CookingPotScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<MythicCraftingScreenHandler> MYTHIC_CRAFTING = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "mythic_crafting"),
            new ScreenHandlerType<>(MythicCraftingScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<TravelingCompassScreenHandler> TRAVELING_COMPASS = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "traveling_compass"),
            new ScreenHandlerType<>(TravelingCompassScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<FossilIncubatorScreenHandler> FOSSIL_INCUBATOR = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "fossil_incubator"),
            new ScreenHandlerType<>(FossilIncubatorScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<ArchaeologistScreenHandler> ARCHAEOLOGIST = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "archaeologist"),
            new ScreenHandlerType<>(ArchaeologistScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    private ModScreenHandlers() {
    }


    public static final ScreenHandlerType<FishingRodScreenHandler> FISHING_ROD = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "fishing_rod"),
            new ScreenHandlerType<>(FishingRodScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<FishNetScreenHandler> FISH_NET = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "fish_net"),
            new ScreenHandlerType<>(FishNetScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<FisheryTableScreenHandler> FISHERY_TABLE = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "fishery_table"),
            new ScreenHandlerType<>(FisheryTableScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static final ScreenHandlerType<FishingBoatScreenHandler> FISHING_BOAT = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(MythicRPG.MOD_ID, "fishing_boat"),
            new ScreenHandlerType<>(FishingBoatScreenHandler::new, FeatureFlags.VANILLA_FEATURES)
    );

    public static void register() {
        MythicRPG.LOGGER.info("Registering MythicRPG screen handlers");
    }
}
