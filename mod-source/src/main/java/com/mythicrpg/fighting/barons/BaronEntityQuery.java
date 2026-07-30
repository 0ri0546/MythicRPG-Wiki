package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class BaronEntityQuery {

    private static final Map<ServerWorld, Map<UUID, LivingEntity>> LOADED_BARONS = new IdentityHashMap<>();

    private BaronEntityQuery() {
    }

    public static void track(LivingEntity entity) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }

        if (!BaronMobManager.isBaron(entity)) {
            return;
        }

        LOADED_BARONS
                .computeIfAbsent(world, ignored -> new HashMap<>())
                .put(entity.getUuid(), entity);
    }

    public static void untrack(Entity entity) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }

        Map<UUID, LivingEntity> worldBarons = LOADED_BARONS.get(world);
        if (worldBarons == null) {
            return;
        }

        worldBarons.remove(entity.getUuid());

        if (worldBarons.isEmpty()) {
            LOADED_BARONS.remove(world);
        }
    }

    public static void clearAll() {
        LOADED_BARONS.clear();
    }

    public static void forEachNearbyBaron(
            ServerWorld world,
            double behaviorRadius,
            double idleParticleRadius,
            NearbyBaronConsumer consumer
    ) {
        Map<UUID, LivingEntity> worldBarons = LOADED_BARONS.get(world);

        if (worldBarons == null || worldBarons.isEmpty()) {
            return;
        }

        double behaviorRadiusSquared = behaviorRadius * behaviorRadius;
        double idleParticleRadiusSquared = idleParticleRadius * idleParticleRadius;
        Iterator<Map.Entry<UUID, LivingEntity>> iterator = worldBarons.entrySet().iterator();

        while (iterator.hasNext()) {
            LivingEntity entity = iterator.next().getValue();

            if (!isTrackedBaronValid(world, entity)) {
                iterator.remove();
                continue;
            }

            boolean withinBehaviorRadius = false;
            boolean withinIdleParticleRadius = false;

            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isSpectator()) {
                    continue;
                }

                double distanceSquared = entity.squaredDistanceTo(player);

                if (distanceSquared <= behaviorRadiusSquared) {
                    withinBehaviorRadius = true;
                }

                if (distanceSquared <= idleParticleRadiusSquared) {
                    withinIdleParticleRadius = true;
                }

                if (withinBehaviorRadius && withinIdleParticleRadius) {
                    break;
                }
            }

            if (withinBehaviorRadius) {
                consumer.accept(
                        world,
                        entity,
                        BaronMobManager.getBaronType(entity),
                        withinIdleParticleRadius
                );
            }
        }

        if (worldBarons.isEmpty()) {
            LOADED_BARONS.remove(world);
        }
    }

    public static boolean isValidPlayerTarget(ServerPlayerEntity player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isCreative();
    }

    public static ServerPlayerEntity findNearestValidPlayerTarget(
            ServerWorld world,
            LivingEntity source,
            double radius
    ) {
        ServerPlayerEntity nearest = null;
        double nearestDistanceSq = radius * radius;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!isValidPlayerTarget(player)) {
                continue;
            }

            double distanceSq = source.squaredDistanceTo(player);

            if (distanceSq > nearestDistanceSq) {
                continue;
            }

            nearestDistanceSq = distanceSq;
            nearest = player;
        }

        return nearest;
    }

    @FunctionalInterface
    public interface NearbyBaronConsumer {
        void accept(
                ServerWorld world,
                LivingEntity entity,
                BaronType type,
                boolean showIdleParticles
        );
    }

    private static boolean isTrackedBaronValid(ServerWorld world, LivingEntity entity) {
        return entity.getWorld() == world
                && entity.isAlive()
                && !entity.isRemoved()
                && BaronMobManager.isBaron(entity);
    }
}
