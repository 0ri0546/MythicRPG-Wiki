package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Runtime manager for lightweight Traveling perks. */
public final class TravelingPerkManager {

    private static final int MOVEMENT_PASSIVE_INTERVAL_TICKS = 5;
    private static final int BIOME_CHECK_INTERVAL_TICKS = 10;
    private static final int DOLPHINS_GRACE_DURATION_TICKS = 12;

    // Exact level-I values used by Minecraft's Soul Speed enchantment data.
    private static final double SOUL_SPEED_MOVEMENT_BONUS = 0.0405;
    private static final double SOUL_SPEED_EFFICIENCY_BONUS = 1.0;

    private static final Identifier SOUL_SPEED_MOVEMENT_ID =
            Identifier.of("mythicrpg", "traveling_soul_walker_speed");
    private static final Identifier SOUL_SPEED_EFFICIENCY_ID =
            Identifier.of("mythicrpg", "traveling_soul_walker_efficiency");

    private static final Map<UUID, Identifier> CURRENT_BIOME_BY_PLAYER = new HashMap<>();
    private static int tickCounter;

    private TravelingPerkManager() {
    }

    public static void clearRuntimePlayer(UUID playerUuid) {
        CURRENT_BIOME_BY_PLAYER.remove(playerUuid);
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            boolean refreshMovement = tickCounter % MOVEMENT_PASSIVE_INTERVAL_TICKS == 0;
            boolean refreshBiome = tickCounter % BIOME_CHECK_INTERVAL_TICKS == 0;

            if (!refreshMovement && !refreshBiome) {
                return;
            }

            var players = server.getPlayerManager().getPlayerList();
            if (players.isEmpty()) {
                return;
            }

            TravelingProgressState progressState = refreshBiome
                    ? TravelingProgressState.get(server)
                    : null;

            for (ServerPlayerEntity player : players) {
                if (refreshMovement) {
                    refreshMovementPassives(player);
                }
                if (refreshBiome) {
                    tickBiomeMomentum(player, progressState);
                }
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CURRENT_BIOME_BY_PLAYER.remove(handler.player.getUuid())
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CURRENT_BIOME_BY_PLAYER.clear();
            tickCounter = 0;
        });
    }

    private static void refreshMovementPassives(ServerPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator()) {
            setSoulWalkerModifiers(player, false);
            return;
        }

        boolean soulWalkerActive = TravelingBonusCache.hasBonus(
                player, BonusType.TRAVEL_SOUL_WALKER
        ) && !player.hasVehicle()
                && !player.getAbilities().flying
                && player.getServerWorld().getBlockState(player.getVelocityAffectingPos()).isIn(BlockTags.SOUL_SPEED_BLOCKS);

        setSoulWalkerModifiers(player, soulWalkerActive);

        if (TravelingBonusCache.hasBonus(player, BonusType.TRAVEL_DOLPHINS_GRACE)
                && (player.isTouchingWater() || player.isSwimming())) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.DOLPHINS_GRACE,
                    DOLPHINS_GRACE_DURATION_TICKS,
                    0,
                    true,
                    false,
                    false
            ));
        }
    }

    private static void setSoulWalkerModifiers(ServerPlayerEntity player, boolean active) {
        setModifier(
                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED),
                SOUL_SPEED_MOVEMENT_ID,
                SOUL_SPEED_MOVEMENT_BONUS,
                active
        );
        setModifier(
                player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_EFFICIENCY),
                SOUL_SPEED_EFFICIENCY_ID,
                SOUL_SPEED_EFFICIENCY_BONUS,
                active
        );
    }

    private static void setModifier(
            EntityAttributeInstance instance,
            Identifier id,
            double value,
            boolean active
    ) {
        if (instance == null) {
            return;
        }

        if (!active) {
            if (instance.hasModifier(id)) {
                instance.removeModifier(id);
            }
            return;
        }

        if (!instance.hasModifier(id)) {
            instance.addTemporaryModifier(new EntityAttributeModifier(
                    id,
                    value,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }
    }

    private static void tickBiomeMomentum(
            ServerPlayerEntity player,
            TravelingProgressState progressState
    ) {
        UUID playerUuid = player.getUuid();
        if (player.isSpectator()
                || !player.isAlive()
                || !TravelingBonusCache.hasBonus(player, BonusType.TRAVEL_BIOME_SPEED)) {
            CURRENT_BIOME_BY_PLAYER.remove(playerUuid);
            return;
        }

        Optional<RegistryKey<Biome>> biomeKey = player.getServerWorld()
                .getBiome(player.getBlockPos())
                .getKey();

        if (biomeKey.isEmpty()) {
            return;
        }

        Identifier currentBiome = biomeKey.get().getValue();
        Identifier previousBiome = CURRENT_BIOME_BY_PLAYER.get(playerUuid);

        if (previousBiome == null) {
            CURRENT_BIOME_BY_PLAYER.put(playerUuid, currentBiome);
            progressState.recordRecentBiome(playerUuid, currentBiome);
            return;
        }

        if (previousBiome.equals(currentBiome)) {
            return;
        }

        CURRENT_BIOME_BY_PLAYER.put(playerUuid, currentBiome);
        boolean outsideRecentHistory = progressState.recordRecentBiome(playerUuid, currentBiome);

        if (!outsideRecentHistory) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                TravelingXpConfig.getBiomeSpeedDurationTicks(),
                0,
                true,
                false,
                true
        ));
    }
}
