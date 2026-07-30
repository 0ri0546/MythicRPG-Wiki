package com.mythicrpg;

import com.mythicrpg.core.*;
import com.mythicrpg.building.*;
import com.mythicrpg.crafting.CraftDebugCommands;
import com.mythicrpg.crafting.ModScreenHandlers;
import com.mythicrpg.crafting.MythicCraftingScreenHandler;
import com.mythicrpg.crafting.OpenMythicCraftingPayload;
import com.mythicrpg.crafting.station.CraftingStationDurabilityManager;
import com.mythicrpg.crafting.LuckyBlockDelayedEventManager;
import com.mythicrpg.crafting.LuckyBlockChoiceManager;
import com.mythicrpg.crafting.MythicInspirationManager;
import com.mythicrpg.farming.FarmingBreedingXpManager;
import com.mythicrpg.farming.FarmingDeathManager;
import com.mythicrpg.farming.FarmingEvents;
import com.mythicrpg.farming.FarmingGrowthManager;
import com.mythicrpg.eating.*;
import com.mythicrpg.fighting.BaronBehaviorManager;
import com.mythicrpg.fighting.barons.BaronDeathMessageRegistry;
import com.mythicrpg.fighting.barons.BaronRewardRegistry;
import com.mythicrpg.fighting.items.BaronLegendaryItemEffects;
import com.mythicrpg.fighting.FightingEvents;
import com.mythicrpg.fighting.PoisonParticleEffects;
import com.mythicrpg.fishing.*;
import com.mythicrpg.network.FishingCodexStatePayload;
import com.mythicrpg.network.FishingMiniGameOpenPayload;
import com.mythicrpg.network.FishingMiniGameActionPayload;
import com.mythicrpg.network.SeaMonsterStatePayload;
import com.mythicrpg.mining.MiningDamageEvents;
import com.mythicrpg.mining.MiningEvents;
import com.mythicrpg.mining.MiningTogglePayload;
import com.mythicrpg.mining.MiningToggleState;
import com.mythicrpg.mining.MiningToggleStatePayload;
import com.mythicrpg.mining.archaeology.FossilCodexManager;
import com.mythicrpg.mining.archaeology.FossilWorldGeneration;
import com.mythicrpg.mining.archaeology.FossilDebugCommands;
import com.mythicrpg.mining.archaeology.FossilIncubatorBlock;
import com.mythicrpg.mining.archaeology.FossilHintManager;
import com.mythicrpg.mining.archaeology.ModVillagers;
import com.mythicrpg.mining.archaeology.ArchaeologistInteractionManager;
import com.mythicrpg.mining.archaeology.GrandSiteHighlightManager;
import com.mythicrpg.mining.archaeology.relic.TemporalReturnManager;
import com.mythicrpg.mining.archaeology.relic.FossilDrillManager;
import com.mythicrpg.mining.archaeology.relic.ColossalAegisManager;
import com.mythicrpg.mining.archaeology.relic.PaletteSelectionManager;
import com.mythicrpg.mining.archaeology.relic.PaletteSelectionPayload;
import com.mythicrpg.network.*;
import com.mythicrpg.woodcutting.TreeGrowthSneakManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import com.mythicrpg.core.PassiveProcSoundManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import com.mythicrpg.woodcutting.WoodcuttingEvents;
import com.mythicrpg.woodcutting.WoodEatingEvents;
import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.woodcutting.EnchantedAxeProjectileManager;
import com.mythicrpg.traveling.TravelingBonusCache;
import com.mythicrpg.traveling.TravelingDebugCommands;
import com.mythicrpg.traveling.TravelingXpManager;
import com.mythicrpg.traveling.TravelingPerkManager;
import com.mythicrpg.traveling.TravelingDoubleJumpManager;
import com.mythicrpg.traveling.TravelingDoubleJumpPayload;
import com.mythicrpg.traveling.TravelingMiniaturizationManager;
import com.mythicrpg.traveling.GrapplingHookManager;
import com.mythicrpg.traveling.GrapplingHookVisualPayload;
import com.mythicrpg.traveling.TravelingDeathRecallManager;
import com.mythicrpg.traveling.TravelingCompassManager;
import com.mythicrpg.traveling.TravelingCompassScreenHandler;
import com.mythicrpg.traveling.LandMountManager;
import com.mythicrpg.traveling.OpenTravelingCompassPayload;
import com.mythicrpg.titles.TitleManager;
import com.mythicrpg.crafting.ExpCharmBonusManager;
import net.minecraft.text.Text;
import com.mythicrpg.mining.VeinMiningTogglePayload;
import com.mythicrpg.mining.VeinMiningToggleState;

public class MythicRPG implements ModInitializer {
	public static final String MOD_ID = "mythicrpg";
	public static final org.slf4j.Logger LOGGER =
			org.slf4j.LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Force le chargement de la classe ModAttachments pour que
		// les attachments s'enregistrent bien dès le démarrage du mod
		var ignoredProgress = ModAttachments.SKILL_PROGRESS;
		var ignoredUnlocks = ModAttachments.SKILL_UNLOCKS;
		var ignoredFossilCodex = ModAttachments.FOSSIL_CODEX;
		var ignoredEatingCodex = ModAttachments.EATING_CODEX;
		var ignoredFishingCodex = ModAttachments.FISHING_CODEX;
		var ignoredSeaMonsterProgress = ModAttachments.SEA_MONSTER_PROGRESS;
		var ignoredEatingRuntime = ModAttachments.EATING_RUNTIME;
		var ignoredTitleProfile = ModAttachments.TITLE_PROFILE;
		var ignoredFoodBackpackLink = ModAttachments.ACTIVE_FOOD_BACKPACK_ID;
		var ignoredMiningToggle = ModAttachments.MINING_AREA_3X3_ENABLED;

		MiningEvents.register();
		PassiveEffectManager.register();
		MiningDamageEvents.register();
		FightingEvents.register();
		AttributeBonusManager.register();
		PoisonParticleEffects.register();
		PayloadTypeRegistry.playS2C().register(LevelUpPayload.ID, LevelUpPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(XpGainPayload.ID, XpGainPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(GrapplingHookVisualPayload.ID, GrapplingHookVisualPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FossilCodexStatePayload.ID, FossilCodexStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EatingCodexStatePayload.ID, EatingCodexStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FishingCodexStatePayload.ID, FishingCodexStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FishingMiniGameOpenPayload.ID, FishingMiniGameOpenPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SeaMonsterStatePayload.ID, SeaMonsterStatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(FishingMiniGameActionPayload.ID, FishingMiniGameActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MiningToggleStatePayload.ID, MiningToggleStatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(VeinMiningTogglePayload.ID, VeinMiningTogglePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(DeliveryPhoneOpenPayload.ID, DeliveryPhoneOpenPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(DeliveryPhoneActionPayload.ID, DeliveryPhoneActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SignatureDishOpenPayload.ID, SignatureDishOpenPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(SignatureDishCreatePayload.ID, SignatureDishCreatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BuildingPlan2DPreviewPayload.ID, BuildingPlan2DPreviewPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BuildingPlan3DPreviewPayload.ID, BuildingPlan3DPreviewPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BuildingSelectionBoxPayload.ID, BuildingSelectionBoxPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BuildingPlanUiStatePayload.ID, BuildingPlanUiStatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(BuildingPlanUiActionPayload.ID, BuildingPlanUiActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ArchitectCompassUiStatePayload.ID, ArchitectCompassUiStatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ArchitectCompassUiActionPayload.ID, ArchitectCompassUiActionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(StaticDecorationUiStatePayload.ID, StaticDecorationUiStatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(StaticDecorationUiActionPayload.ID, StaticDecorationUiActionPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(BuildingReserveRequestPayload.ID, BuildingReserveRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(PaletteSelectionPayload.ID, PaletteSelectionPayload.CODEC);
		WoodcuttingEvents.register();
		ModBlocks.register();
		ModBlockEntities.register();
		ModVillagers.register();
		ModEntities.register();
		ModItems.register();
		BaronRewardRegistry.register();
		BaronLegendaryItemEffects.register();
		ModScreenHandlers.register();
		WoodEatingEvents.register();
		GrowthHealthManager.register();
		BaronMobManager.register();
		BaronBehaviorManager.register();
		EnchantedAxeProjectileManager.register();
		TreeGrowthSneakManager.register();
		FarmingEvents.register();
		FarmingDeathManager.register();
		FarmingGrowthManager.register();
		FarmingBreedingXpManager.register();
		CraftDebugCommands.register();
		FossilDebugCommands.register();
		FossilWorldGeneration.register();
		FossilHintManager.register();
		FossilIncubatorBlock.registerBreakProtection();
		CookingPotBlock.registerBreakProtection();
		EatingDeliveryManager.register();
		EatingAdvancedManager.register();
		ArchaeologistInteractionManager.register();
		GrandSiteHighlightManager.register();
		TemporalReturnManager.register();
		FossilDrillManager.register();
		ColossalAegisManager.register();
		PaletteSelectionManager.register();
		TravelingBonusCache.register();
		TravelingXpManager.register();
		BuildingComfortManager.register();
		BuildingXpManager.register();
		BuildingMagnetManager.register();
		BuildingScaffoldingManager.register();
		BuildingPlan2DManager.register();
		BuildingPlan3DManager.register();
		BuildingReserveChestManager.register();
		StaticDecorationManager.register();
		BuildingMiniatureManager.register();
		ArchitectCompassUiManager.register();
		TravelingPerkManager.register();
		TravelingDoubleJumpManager.register();
		TravelingMiniaturizationManager.register();
		TravelingDeathRecallManager.register();
		TravelingCompassManager.register();
		LandMountManager.register();
		GrapplingHookManager.register();
		TravelingDebugCommands.register();
		CraftingStationDurabilityManager.register();
		LuckyBlockDelayedEventManager.init();
		LuckyBlockChoiceManager.register();
		FishingWeatherManager.register();
        FishingNetManager.register();
		FishingMiniGameManager.register();
		FishingArmorMovementManager.register();
		SeaMonsterManager.register();

		PayloadTypeRegistry.playC2S().register(UnlockRequestPayload.ID, UnlockRequestPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TreeStatePayload.ID, TreeStatePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ResetTreePayload.ID, ResetTreePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(MiningTogglePayload.ID, MiningTogglePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(BuildingMagnetTogglePayload.ID, BuildingMagnetTogglePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(OpenMythicCraftingPayload.ID, OpenMythicCraftingPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TravelingDoubleJumpPayload.ID, TravelingDoubleJumpPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(OpenTravelingCompassPayload.ID, OpenTravelingCompassPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TitleStateRequestPayload.ID, TitleStateRequestPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(TitleSelectionPayload.ID, TitleSelectionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TitleStatePayload.ID, TitleStatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(FishingMiniGameActionPayload.ID, (payload, context) ->
				context.player().server.execute(() -> FishingMiniGameManager.handle(context.player(), payload))
		);

		ServerPlayNetworking.registerGlobalReceiver(DeliveryPhoneActionPayload.ID, (payload, context) ->
				context.player().server.execute(() -> EatingDeliveryManager.handle(context.player(), payload))
		);

		ServerPlayNetworking.registerGlobalReceiver(SignatureDishCreatePayload.ID, (payload, context) ->
				context.player().server.execute(() -> SignatureDishManager.handle(context.player(), payload))
		);

		ServerPlayNetworking.registerGlobalReceiver(PaletteSelectionPayload.ID, (payload, context) -> context.player().server.execute(() -> PaletteSelectionManager.select(context.player(), payload.index())));

		ServerPlayNetworking.registerGlobalReceiver(MiningTogglePayload.ID, (payload, context) ->
				context.player().server.execute(() -> {
					if (PlayerCooldownManager.tryUse(context.player(), "net:mining_toggle", 1)) {
						MiningToggleState.setAreaMiningEnabled(context.player(), payload.enabled());
					} else {
						MiningToggleState.sync(context.player());
					}
				})
		);

		ServerPlayNetworking.registerGlobalReceiver(
				VeinMiningTogglePayload.ID,
				(payload, context) ->
						context.player().server.execute(() ->
								VeinMiningToggleState.setEnabled(
										context.player().getUuid(),
										payload.enabled()
								)
						)
		);

		ServerPlayNetworking.registerGlobalReceiver(BuildingMagnetTogglePayload.ID, (payload, context) ->
				context.player().server.execute(() ->
						BuildingMagnetState.setEnabled(context.player().getUuid(), payload.enabled())
				)
		);

		ServerPlayNetworking.registerGlobalReceiver(BuildingReserveRequestPayload.ID, (payload, context) ->
				context.player().server.execute(() -> {
					if (PlayerCooldownManager.tryUse(context.player(), "net:building_reserve", 2))
						BuildingReserveChestManager.requestHeldBlock(context.player());
				})
		);

		ServerPlayNetworking.registerGlobalReceiver(BuildingPlanUiActionPayload.ID, (payload, context) ->
				context.player().server.execute(() ->
						BuildingPlanUiManager.handle(context.player(), payload)
				)
		);

		ServerPlayNetworking.registerGlobalReceiver(ArchitectCompassUiActionPayload.ID, (payload, context) ->
				context.player().server.execute(() ->
						ArchitectCompassUiManager.handle(context.player(), payload)
				)
		);

		ServerPlayNetworking.registerGlobalReceiver(StaticDecorationUiActionPayload.ID, (payload, context) ->
				context.player().server.execute(() ->
						StaticDecorationUiManager.handle(context.player(), payload)
				)
		);

		ServerPlayNetworking.registerGlobalReceiver(OpenMythicCraftingPayload.ID, (payload, context) -> {
			context.player().server.execute(() -> { if (PlayerCooldownManager.tryUse(context.player(), "net:open_crafting", 5)) MythicCraftingScreenHandler.openPortable(context.player()); });
		});

		ServerPlayNetworking.registerGlobalReceiver(OpenTravelingCompassPayload.ID, (payload, context) -> {
			context.player().server.execute(() -> { if (PlayerCooldownManager.tryUse(context.player(), "net:open_traveling_compass", 5)) TravelingCompassScreenHandler.open(context.player()); });
		});

		ServerPlayNetworking.registerGlobalReceiver(TravelingDoubleJumpPayload.ID, (payload, context) ->
				context.player().server.execute(() -> TravelingDoubleJumpManager.tryDoubleJump(context.player()))
		);

		ServerPlayNetworking.registerGlobalReceiver(TitleStateRequestPayload.ID, (payload, context) ->
				context.player().server.execute(() -> { if (PlayerCooldownManager.tryUse(context.player(), "net:title_state", 5)) TitleManager.sendState(context.player()); })
		);

		ServerPlayNetworking.registerGlobalReceiver(TitleSelectionPayload.ID, (payload, context) ->
				context.player().server.execute(() -> TitleManager.applySelection(
						context.player(),
						payload.titleId(),
						payload.primaryColorId(),
						payload.secondaryColorId(),
						payload.gradient(),
						payload.finishId()
				))
		);


		ServerPlayNetworking.registerGlobalReceiver(UnlockRequestPayload.ID, (payload, context) ->
				context.player().server.execute(() -> SkillType.fromId(payload.skillId()).ifPresent(type -> {
					if (!PlayerCooldownManager.tryUse(context.player(), "net:skill_unlock", 1)) return;
					SkillTreeManager.tryUnlock(context.player(), type, payload.nodeId());
					SkillTreeManager.sendStateTo(context.player(), type);
				}))
		);

		ServerPlayNetworking.registerGlobalReceiver(ResetTreePayload.ID, (payload, context) ->
				context.player().server.execute(() -> SkillType.fromId(payload.skillId()).ifPresent(type -> {
					if (!PlayerCooldownManager.tryUse(context.player(), "net:skill_reset", 5)) return;
					if (!SkillTreeManager.resetTree(context.player(), type))
						context.player().sendMessage(Text.translatable("command.mythicrpg.reset.failed"), false);
				}))
		);

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			EatingAdvancedManager.clear();
			com.mythicrpg.farming.FoodBackpackSessionManager.clearAll();
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			EatingDeliveryManager.clear();
			com.mythicrpg.farming.FoodBackpackSessionManager.clearAll();
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SkillTreeManager.sendAllStatesTo(handler.player);
			FossilCodexManager.reconcileAnalyses(handler.player);
			FossilCodexManager.sendStateTo(handler.player);
			EatingCodexManager.sendStateTo(handler.player);
			FishingCodexManager.sendStateTo(handler.player);
			SeaMonsterManager.sendState(handler.player);
			TitleManager.initializePlayer(handler.player);
			MiningToggleState.sync(handler.player);
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			PlayerCooldownManager.clearPlayer(handler.player.getUuid());
			PassiveProcSoundManager.clearPlayer(handler.player.getUuid());
			TreeGrowthSneakManager.clearPlayer(handler.player.getUuid());
			BuildingXpManager.flushPlayer(handler.player);
			BuildingXpManager.clearPlayer(handler.player.getUuid());
			BuildingUiSessionManager.clearPlayer(handler.player.getUuid());
			BuildingComfortManager.clearPlayer(handler.player.getUuid());
			BuildingMagnetState.clear(handler.player.getUuid());
			VeinMiningToggleState.clear(handler.player.getUuid());
			BuildingPlan2DManager.clearPlayer(handler.player);
			BuildingPlan3DManager.clearPlayer(handler.player);
			MythicInspirationManager.clearPlayer(handler.player.getUuid());
			ExpCharmBonusManager.clearPlayer(handler.player.getUuid());
			BaronDeathMessageRegistry.clearPlayer(handler.player.getUuid());
			com.mythicrpg.farming.FoodBackpackSessionManager.clear(handler.player);
			FishingMiniGameManager.clear(handler.player);
			FishingWeatherManager.clearPlayer(handler.player.getUuid());
			SeaMonsterManager.clearPlayer(handler.player);
		});


		ServerTickEvents.END_SERVER_TICK.register(server -> {
			com.mythicrpg.farming.FoodBackpackSessionManager.tick();
			long tick = server.getOverworld().getTime();

			if (tick % 600 != 0) {
				return;
			}

			PassiveProcSoundManager.cleanupOldEntries(tick, 1200);
			PlayerCooldownManager.cleanupOldEntries(tick, 1200);
			EntityCooldownManager.cleanupOldEntries(tick, 6000);
			BaronDeathMessageRegistry.cleanupOldEntries(tick);
			FarmingEvents.cleanupRecentReplants(tick);
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(CommandManager.literal("mythicrpg")
					.requires(source -> source.hasPermissionLevel(2))
					.then(CommandManager.literal("givepoints")
							.then(CommandManager.argument("skill", com.mojang.brigadier.arguments.StringArgumentType.word())
									.then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
											.executes(ctx -> {
												ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
												int amount = IntegerArgumentType.getInteger(ctx, "amount");
												SkillType type = SkillType.valueOf(
														com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "skill").toUpperCase()
												);

												SkillProgress progress = ModAttachments.getProgress(player, type);
												progress.addSkillPoints(amount);
												ModAttachments.setProgress(player, type, progress);
												SkillTreeManager.sendStateTo(player, type);

												ctx.getSource().sendFeedback(() ->
														Text.translatable("command.mythicrpg.givepoints.success", amount, type.displayName()), false);
												return 1;
											}))))
					.then(CommandManager.literal("resetxp")
							.then(CommandManager.argument("skill", com.mojang.brigadier.arguments.StringArgumentType.word())
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										SkillType type = SkillType.valueOf(
												com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "skill").toUpperCase()
										);

										ModAttachments.setProgress(player, type, new SkillProgress());
										SkillTreeManager.sendStateTo(player, type);
										PlayerTabNameManager.refresh(player);

										ctx.getSource().sendFeedback(() -> Text.translatable("command.mythicrpg.resetxp.success", type.displayName()), false);
										return 1;
									})))
					.then(CommandManager.literal("reset")
							.then(CommandManager.argument("skill", com.mojang.brigadier.arguments.StringArgumentType.word())
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
										SkillType type = SkillType.valueOf(
												com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "skill").toUpperCase()
										);
										boolean success = SkillTreeManager.resetTree(player, type);
										Text feedback = success
												? Text.translatable("command.mythicrpg.reset.success", type.displayName())
												: Text.translatable("command.mythicrpg.reset.failed");
										ctx.getSource().sendFeedback(() -> feedback, false);
										return success ? 1 : 0;
									})))
					.then(CommandManager.literal("fishing")
							.then(CommandManager.literal("spawnmonster")
									.then(CommandManager.literal("nessie")
											.executes(ctx -> spawnSeaMonsterForTest(
													ctx,
													SeaMonsterType.NESSIE
											)))
									.then(CommandManager.literal("megalodon")
											.executes(ctx -> spawnSeaMonsterForTest(
													ctx,
													SeaMonsterType.MEGALODON
											)))
									.then(CommandManager.literal("whale")
											.executes(ctx -> spawnSeaMonsterForTest(
													ctx,
													SeaMonsterType.WHALE
											)))))
					.then(CommandManager.literal("titles")
							.then(CommandManager.literal("unlock_all")
									.executes(ctx -> {
										ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();

										int addedCount = TitleManager.unlockAllForTesting(player);

										ctx.getSource().sendFeedback(
												() -> Text.translatable("command.mythicrpg.titles.unlock_all.success", addedCount),
												false
										);

										return 1;
									})))
			);
		});
	}
	private static int spawnSeaMonsterForTest(
			com.mojang.brigadier.context.CommandContext<
					net.minecraft.server.command.ServerCommandSource
					> context,
			SeaMonsterType type
	) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayerEntity player = context.getSource().getPlayerOrThrow();

		boolean spawned = SeaMonsterManager.debugSpawnAtFullGauge(player, type);

		Text feedback = spawned
				? Text.literal("Monstre marin invoqué : ").append(type.displayName())
				: Text.literal(
				"Impossible d'invoquer le monstre : "
				+ "une rencontre est déjà active ou l'apparition a échoué."
		);

		context.getSource().sendFeedback(() -> feedback, false);
		return spawned ? 1 : 0;
	}
}