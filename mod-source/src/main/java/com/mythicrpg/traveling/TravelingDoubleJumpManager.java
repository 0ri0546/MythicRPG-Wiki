package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TravelingDoubleJumpManager {
    private static final double DOUBLE_JUMP_VERTICAL_VELOCITY = 0.42;
    private static final int MIN_AIR_TICKS_BEFORE_DOUBLE_JUMP = 4;

    private static final Set<UUID> CONSUMED_SINCE_GROUND = new HashSet<>();
    private static final Map<UUID, Integer> AIRBORNE_SINCE_AGE = new HashMap<>();

    private TravelingDoubleJumpManager() {
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                clearPlayer(handler.player.getUuid())
        );

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CONSUMED_SINCE_GROUND.clear();
            AIRBORNE_SINCE_AGE.clear();
        });
    }

    static void tickPlayer(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        if (!player.isAlive()
                || player.isSpectator()
                || !TravelingBonusCache.hasBonus(player, BonusType.TRAVEL_DOUBLE_JUMP)) {
            clearPlayer(playerId);
            return;
        }

        if (canRecharge(player)) {
            CONSUMED_SINCE_GROUND.remove(playerId);
            AIRBORNE_SINCE_AGE.remove(playerId);
            return;
        }

        AIRBORNE_SINCE_AGE.putIfAbsent(playerId, player.age);
    }

    public static void tryDoubleJump(ServerPlayerEntity player) {
        if (!TravelingBonusCache.hasBonus(player, BonusType.TRAVEL_DOUBLE_JUMP)) {
            return;
        }

        UUID playerId = player.getUuid();
        int airborneSinceAge = AIRBORNE_SINCE_AGE.getOrDefault(playerId, player.age);

        if (player.age - airborneSinceAge < MIN_AIR_TICKS_BEFORE_DOUBLE_JUMP) {
            return;
        }

        if (!canUseDoubleJump(player) || !CONSUMED_SINCE_GROUND.add(playerId)) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        player.setVelocity(velocity.x, Math.max(velocity.y, DOUBLE_JUMP_VERTICAL_VELOCITY), velocity.z);
        player.fallDistance = 0.0F;
        player.velocityModified = true;
    }

    private static void clearPlayer(UUID playerId) {
        CONSUMED_SINCE_GROUND.remove(playerId);
        AIRBORNE_SINCE_AGE.remove(playerId);
    }

    private static boolean canRecharge(ServerPlayerEntity player) {
        return player.isOnGround()
                || player.isTouchingWater()
                || player.isClimbing()
                || player.hasVehicle()
                || player.getAbilities().flying;
    }

    private static boolean canUseDoubleJump(ServerPlayerEntity player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isOnGround()
                && !player.isTouchingWater()
                && !player.isSwimming()
                && !player.isClimbing()
                && !player.isFallFlying()
                && !player.hasVehicle()
                && !player.getAbilities().flying;
    }
}
