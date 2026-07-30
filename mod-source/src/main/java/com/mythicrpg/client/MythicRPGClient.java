package com.mythicrpg.client;

import com.mythicrpg.client.crafting.MythicCraftingScreen;
import com.mythicrpg.client.eating.ClientEatingCodexState;
import com.mythicrpg.client.eating.CookingPotScreen;
import com.mythicrpg.client.woodcutting.ModularChestScreen;
import com.mythicrpg.client.eating.FoodSaturationTooltip;
import com.mythicrpg.client.ui.MythicInventoryTabs;
import com.mythicrpg.client.eating.DeliveryPhoneScreen;
import com.mythicrpg.client.eating.SignatureDishScreen;
import com.mythicrpg.client.building.ArchitectCompassClient;
import com.mythicrpg.client.building.BuildingPlan2DClient;
import com.mythicrpg.client.building.BuildingPlan3DClient;
import com.mythicrpg.client.building.BuildingSelectionBoxClient;
import com.mythicrpg.client.building.ui.BuildingPlanUiClient;
import com.mythicrpg.client.building.ui.ArchitectCompassUiClient;
import com.mythicrpg.client.building.ui.StaticDecorationUiClient;
import com.mythicrpg.client.building.BlankBlockEntityRenderer;
import com.mythicrpg.building.BuildingPlan2DPreviewPayload;
import com.mythicrpg.building.BuildingPlan3DPreviewPayload;
import com.mythicrpg.building.BuildingSelectionBoxPayload;
import com.mythicrpg.building.BuildingPlanUiStatePayload;
import com.mythicrpg.building.BuildingReserveRequestPayload;
import com.mythicrpg.building.ArchitectCompassUiStatePayload;
import com.mythicrpg.building.StaticDecorationUiStatePayload;
import com.mythicrpg.client.titles.ClientTitleState;
import com.mythicrpg.client.titles.TitleSelectionScreen;
import com.mythicrpg.client.mining.OreHighlightRenderer;
import com.mythicrpg.client.mining.FossilCleaningHud;
import com.mythicrpg.client.mining.relic.PaletteClientManager;
import com.mythicrpg.client.mining.relic.ColossalAegisRenderer;
import com.mythicrpg.client.mining.FossilIncubatorScreen;
import com.mythicrpg.client.mining.ArchaeologistScreen;
import com.mythicrpg.client.mining.ClientFossilCodexState;
import com.mythicrpg.network.FossilCodexStatePayload;
import com.mythicrpg.network.EatingCodexStatePayload;
import com.mythicrpg.eating.DeliveryPhoneOpenPayload;
import com.mythicrpg.eating.SignatureDishOpenPayload;
import com.mythicrpg.eating.SignatureDishIconRegistry;
import com.mythicrpg.network.TitleStatePayload;
import com.mythicrpg.crafting.ModScreenHandlers;
import com.mythicrpg.mining.MiningTogglePayload;
import com.mythicrpg.mining.MiningToggleStatePayload;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.ModEntities;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.ModBlockEntities;
import com.mythicrpg.client.building.StaticDecorationBlockEntityRenderer;
import com.mythicrpg.client.building.StaticDecorationRenderBudget;
import com.mythicrpg.client.building.BuildingMiniatureEntityRenderer;
import com.mythicrpg.client.building.BuildingMiniatureRenderBudget;
import com.mythicrpg.network.TreeStatePayload;
import com.mythicrpg.network.XpGainPayload;
import com.mythicrpg.client.traveling.TravelingDoubleJumpClient;
import com.mythicrpg.client.traveling.GrapplingHookClient;
import com.mythicrpg.client.traveling.TravelingCompassScreen;
import com.mythicrpg.traveling.GrapplingHookVisualPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.BoatEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.client.render.entity.MinecartEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import com.mythicrpg.network.LevelUpPayload;
import com.mythicrpg.network.FishingCodexStatePayload;
import com.mythicrpg.network.FishingMiniGameOpenPayload;
import com.mythicrpg.network.SeaMonsterStatePayload;
import com.mythicrpg.client.fishing.*;
import com.mythicrpg.fishing.FishingRodScreenHandler;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class MythicRPGClient implements ClientModInitializer {
    private static KeyBinding openTreeKey;
    private static KeyBinding toggle3x3Key;
    private static KeyBinding toggleHighlightKey;
    private static KeyBinding requestBuildingReserveKey;
    private static KeyBinding openEatingCodexKey;
    private static boolean area3x3Enabled = false;

    @Override
    public void onInitializeClient() {
        MythicClientPreferences.initialize();

        FoodSaturationTooltip.register();

        HandledScreens.register(ModScreenHandlers.MODULAR_CHEST_SINGLE, ModularChestScreen::new);
        HandledScreens.register(ModScreenHandlers.MODULAR_CHEST_DOUBLE, ModularChestScreen::new);
        HandledScreens.register(ModScreenHandlers.MYTHIC_CRAFTING, MythicCraftingScreen::new);
        HandledScreens.register(ModScreenHandlers.TRAVELING_COMPASS, TravelingCompassScreen::new);
        HandledScreens.register(ModScreenHandlers.FOSSIL_INCUBATOR, FossilIncubatorScreen::new);
        HandledScreens.register(ModScreenHandlers.ARCHAEOLOGIST, ArchaeologistScreen::new);
        HandledScreens.register(ModScreenHandlers.COOKING_POT, CookingPotScreen::new);
        HandledScreens.register(ModScreenHandlers.FISHING_ROD, FishingRodScreen::new);
        HandledScreens.register(ModScreenHandlers.FISH_NET, FishNetScreen::new);
        HandledScreens.register(ModScreenHandlers.FISHERY_TABLE, FisheryTableScreen::new);
        HandledScreens.register(ModScreenHandlers.FISHING_BOAT, FishingBoatScreen::new);

        FabricModelPredicateProviderRegistry.register(
                ModItems.COLOSSAL_AEGIS,
                Identifier.ofVanilla("blocking"),
                (stack, world, entity, seed) -> entity != null
                        && entity.isUsingItem()
                        && entity.getActiveItem() == stack
                        ? 1.0F
                        : 0.0F
        );

        FabricModelPredicateProviderRegistry.register(
                ModItems.SIGNATURE_DISH,
                Identifier.of("mythicrpg", "signature_icon"),
                (stack, world, entity, seed) -> SignatureDishIconRegistry.predicateValue(stack)
        );

        registerFishingRodCastPredicate(ModItems.MYTHIC_FISHING_ROD);
        registerFishingRodCastPredicate(ModItems.BASALT_FISHING_ROD);
        registerFishingRodCastPredicate(ModItems.VOID_FISHING_ROD);
        FabricModelPredicateProviderRegistry.register(
                ModItems.WEATHER_WAND,
                Identifier.of("mythicrpg", "weather_mode"),
                (stack, world, entity, seed) -> com.mythicrpg.fishing.WeatherWandItem.modelPredicate(stack)
        );

        BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.COLOSSAL_AEGIS,
                new ColossalAegisRenderer()
        );

        BlockEntityRendererFactories.register(
                ModBlockEntities.BLANK_BLOCK,
                BlankBlockEntityRenderer::new
        );
        BlankBlockEntityRenderer.registerReloadListener();
        BlockEntityRendererFactories.register(
                ModBlockEntities.STATIC_DECORATION,
                StaticDecorationBlockEntityRenderer::new
        );


        EntityRendererRegistry.register(
                ModEntities.TRAVELER_MINECART,
                context -> new MinecartEntityRenderer<>(context, EntityModelLayers.MINECART)
        );
        EntityRendererRegistry.register(ModEntities.FISHING_BOAT, context -> new BoatEntityRenderer(context, false));

        EntityRendererRegistry.register(
                ModEntities.TRAVELER_BOAT,
                context -> new BoatEntityRenderer(context, false)
        );
        EntityRendererRegistry.register(
                ModEntities.BUILDING_MINIATURE,
                BuildingMiniatureEntityRenderer::new
        );

        ClientPlayNetworking.registerGlobalReceiver(FossilCodexStatePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientFossilCodexState.update(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(FishingCodexStatePayload.ID, (payload, context) -> context.client().execute(() -> ClientFishingCodexState.update(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SeaMonsterStatePayload.ID, (payload, context) -> context.client().execute(() -> ClientSeaMonsterState.update(payload)));
        ClientPlayNetworking.registerGlobalReceiver(FishingMiniGameOpenPayload.ID, (payload, context) -> context.client().execute(() -> context.client().setScreen(new FishingMiniGameScreen(payload))));
        ClientPlayNetworking.registerGlobalReceiver(EatingCodexStatePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientEatingCodexState.update(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(MiningToggleStatePayload.ID, (payload, context) ->
                context.client().execute(() -> area3x3Enabled = payload.enabled())
        );
        ClientPlayNetworking.registerGlobalReceiver(DeliveryPhoneOpenPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new DeliveryPhoneScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(SignatureDishOpenPayload.ID, (payload, context) ->
                context.client().execute(() -> context.client().setScreen(new SignatureDishScreen(payload)))
        );
        ClientPlayNetworking.registerGlobalReceiver(BuildingPlan2DPreviewPayload.ID, (payload, context) ->
                context.client().execute(() -> BuildingPlan2DClient.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(BuildingPlan3DPreviewPayload.ID, (payload, context) ->
                context.client().execute(() -> BuildingPlan3DClient.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(BuildingSelectionBoxPayload.ID, (payload, context) ->
                context.client().execute(() -> BuildingSelectionBoxClient.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(BuildingPlanUiStatePayload.ID, (payload, context) ->
                context.client().execute(() -> BuildingPlanUiClient.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(ArchitectCompassUiStatePayload.ID, (payload, context) ->
                context.client().execute(() -> ArchitectCompassUiClient.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(StaticDecorationUiStatePayload.ID, (payload, context) ->
                context.client().execute(() -> StaticDecorationUiClient.handle(payload))
        );
        ClientPlayNetworking.registerGlobalReceiver(TitleStatePayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    if (context.client().currentScreen instanceof TitleSelectionScreen screen) {
                        screen.acceptState(payload);
                    } else {
                        ClientTitleState.update(payload);
                    }
                })
        );
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) ->
                        client.execute(() -> {
                            MythicClientPreferences
                                    .syncBuildingMagnetPreference();

                            MythicClientPreferences
                                    .syncVeinMiningPreference();
                        })
        );
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientFossilCodexState.clear();
            com.mythicrpg.core.ClientSkillUnlockSnapshot.clear();
            ClientEatingCodexState.clear();
            ClientFishingCodexState.clear();
            ClientSeaMonsterState.clear();
            ClientTitleState.clear();
            area3x3Enabled = false;
            BuildingPlan2DClient.clear();
            BuildingPlan3DClient.clear();
            BuildingSelectionBoxClient.clear();
            ArchitectCompassClient.clear();
            BuildingMiniatureEntityRenderer.clearCache();
        });

        ClientPlayNetworking.registerGlobalReceiver(TreeStatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                SkillType type = SkillType.valueOf(payload.skillId());
                ClientSkillTreeState.update(type, payload.unlockedIds(), payload.skillPoints(),
                        payload.level(), payload.currentXp(), payload.xpForNext());

                if (context.client().currentScreen instanceof SkillTreeScreen screen) {
                    screen.refresh();
                }
            });
        });

        openTreeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mythicrpg.open_mining_tree", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.mythicrpg.keys"
        ));
        toggle3x3Key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mythicrpg.toggle_3x3", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "category.mythicrpg.keys"
        ));
        toggleHighlightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mythicrpg.toggle_highlight", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.mythicrpg.keys"
        ));
        requestBuildingReserveKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mythicrpg.building_reserve_request", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.mythicrpg.keys"
        ));
        openEatingCodexKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mythicrpg.open_eating_codex", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.mythicrpg.keys"
        ));

        WorldRenderEvents.START.register(context -> {
            StaticDecorationRenderBudget.beginFrame();
            BuildingMiniatureRenderBudget.beginFrame();
        });
        WorldRenderEvents.AFTER_TRANSLUCENT.register(OreHighlightRenderer::onRenderWorld);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BuildingPlan2DClient::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BuildingPlan3DClient::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BuildingSelectionBoxClient::render);
        WorldRenderEvents.AFTER_TRANSLUCENT.register(ArchitectCompassClient::render);
        WorldRenderEvents.AFTER_ENTITIES.register(GrapplingHookClient::render);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            OreHighlightRenderer.onClientTick(client);
            TravelingDoubleJumpClient.tick(client);
            GrapplingHookClient.tick(client);
            PaletteClientManager.tick(client);
            BuildingPlan2DClient.tick(client);
            BuildingPlan3DClient.tick(client);
            BuildingSelectionBoxClient.tick(client);
            ArchitectCompassClient.tick(client);
            while (openTreeKey.wasPressed()) {
                if (client.player != null) {
                    client.setScreen(new SkillTreeScreen());
                }
            }

            while (openEatingCodexKey.wasPressed()) {
                if (client.player != null) {
                    MythicInventoryTabs.requestOpenEatingCodex();
                }
            }

            while (toggle3x3Key.wasPressed()) {
                if (client.player != null) {
                    area3x3Enabled = !area3x3Enabled;
                    ClientPlayNetworking.send(new MiningTogglePayload(area3x3Enabled));
                    client.player.sendMessage(
                            Text.translatable(area3x3Enabled ? "message.mythicrpg.mining_3x3.enabled" : "message.mythicrpg.mining_3x3.disabled"), true
                    );
                }
            }

            while (toggleHighlightKey.wasPressed()) {
                if (client.player != null) {
                    OreHighlightRenderer.toggle();
                    client.player.sendMessage(
                            Text.translatable(OreHighlightRenderer.isEnabled() ? "message.mythicrpg.ore_highlight.enabled" : "message.mythicrpg.ore_highlight.disabled"), true
                    );
                }
            }

            while (requestBuildingReserveKey.wasPressed()) {
                if (client.player != null) {
                    ClientPlayNetworking.send(new BuildingReserveRequestPayload());
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(GrapplingHookVisualPayload.ID, (payload, context) -> {
            context.client().execute(() -> GrapplingHookClient.handle(payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(LevelUpPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                SkillType type = SkillType.valueOf(payload.skillId());
                LevelUpHud.show(type, payload.level(), payload.currentXp(), payload.xpForNext());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(XpGainPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                SkillType type = SkillType.valueOf(payload.skillId());
                XpHud.update(type, payload.level(), payload.currentXp(), payload.xpForNext());
                ClientSkillTreeState.updateXp(type, payload.level(), payload.currentXp(), payload.xpForNext());
            });
        });

        HudRenderCallback.EVENT.register(XpHud::render);

        HudRenderCallback.EVENT.register(LevelUpHud::render);
        HudRenderCallback.EVENT.register(PaletteClientManager::render);
        HudRenderCallback.EVENT.register(FossilCleaningHud::render);
    }
    private static void registerFishingRodCastPredicate(net.minecraft.item.Item rod) {
        FabricModelPredicateProviderRegistry.register(
                rod,
                Identifier.ofVanilla("cast"),
                (stack, world, entity, seed) -> {
                    if (!(entity instanceof net.minecraft.entity.player.PlayerEntity player)
                            || player.fishHook == null) {
                        return 0.0F;
                    }
                    return player.getMainHandStack() == stack || player.getOffHandStack() == stack
                            ? 1.0F
                            : 0.0F;
                }
        );
    }

}