package com.mythicrpg.fighting;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PoisonParticleEffects {

    private static final int TICK_INTERVAL = 10;
    private static final int PARTICLE_COUNT = 6;
    private static final double RADIUS = 0.6;
    private static final double SEARCH_RADIUS = 50.0;
    private static final int SEARCH_CELL_SIZE = 64;
    private static final int MAX_RINGS_PER_WORLD = 256;

    private static final DustParticleEffect GREEN_DUST =
            new DustParticleEffect(new Vector3f(0.2f, 0.8f, 0.2f), 1.0f);

    private static int tickCounter = 0;
    private static int totalTicks = 0;

    private PoisonParticleEffects() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PoisonParticleEffects::onServerTick);
    }

    private static void onServerTick(MinecraftServer server) {
        totalTicks++;
        tickCounter++;

        if (tickCounter < TICK_INTERVAL) {
            return;
        }

        tickCounter = 0;

        double rotation = (totalTicks % 360) * (Math.PI / 180.0);

        for (ServerWorld world : server.getWorlds()) {
            spawnParticlesForWorld(world, rotation);
        }
    }

    private static void spawnParticlesForWorld(ServerWorld world, double rotation) {
        Map<SearchCell, SearchRegion> regions = new HashMap<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            SearchCell cell = new SearchCell(
                    Math.floorDiv(player.getBlockX(), SEARCH_CELL_SIZE),
                    Math.floorDiv(player.getBlockY(), SEARCH_CELL_SIZE),
                    Math.floorDiv(player.getBlockZ(), SEARCH_CELL_SIZE)
            );
            regions.computeIfAbsent(cell, ignored -> new SearchRegion())
                    .include(player.getBoundingBox());
        }

        Set<LivingEntity> alreadyHandled = new HashSet<>();
        int rendered = 0;
        outer:
        for (SearchRegion region : regions.values()) {
            List<LivingEntity> nearbyPoisoned = world.getEntitiesByClass(
                    LivingEntity.class,
                    region.expandedBox(),
                    entity -> entity.hasStatusEffect(StatusEffects.POISON)
            );

            for (LivingEntity entity : nearbyPoisoned) {
                if (region.isVisibleToAnyPlayer(entity.getBoundingBox())
                        && alreadyHandled.add(entity)) {
                    spawnRing(world, entity, rotation);
                    rendered++;
                    if (rendered >= MAX_RINGS_PER_WORLD) break outer;
                }
            }
        }
    }

    private record SearchCell(int x, int y, int z) {
    }

    private static final class SearchRegion {
        private final List<Box> viewerAreas = new ArrayList<>();
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private void include(Box box) {
            viewerAreas.add(box.expand(SEARCH_RADIUS));
            minX = Math.min(minX, box.minX);
            minY = Math.min(minY, box.minY);
            minZ = Math.min(minZ, box.minZ);
            maxX = Math.max(maxX, box.maxX);
            maxY = Math.max(maxY, box.maxY);
            maxZ = Math.max(maxZ, box.maxZ);
        }

        private Box expandedBox() {
            return new Box(minX, minY, minZ, maxX, maxY, maxZ).expand(SEARCH_RADIUS);
        }

        private boolean isVisibleToAnyPlayer(Box entityBox) {
            for (Box viewerArea : viewerAreas) {
                if (viewerArea.intersects(entityBox)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void spawnRing(ServerWorld world, LivingEntity entity, double rotationOffset) {
        double centerY = entity.getBodyY(0.5);

        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double angle = rotationOffset + (2 * Math.PI * i / PARTICLE_COUNT);
            double offsetX = Math.cos(angle) * RADIUS;
            double offsetZ = Math.sin(angle) * RADIUS;

            world.spawnParticles(
                    GREEN_DUST,
                    entity.getX() + offsetX,
                    centerY,
                    entity.getZ() + offsetZ,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }
}
