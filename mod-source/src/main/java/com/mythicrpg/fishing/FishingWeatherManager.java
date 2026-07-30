package com.mythicrpg.fishing;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Local Fishing-only weather zones. Vanilla global weather is never changed. */
public final class FishingWeatherManager {
    public enum Mode {
        RAIN,
        SUN,
        STORM
    }

    public static final int BASE_RADIUS = 10;
    public static final int HARMONIZED_RADIUS = 14;
    public static final long BASE_DURATION_TICKS = 20L * 60L * 10L;
    public static final long SEALED_DURATION_TICKS = 20L * 60L * 15L;

    private static final Map<UUID, Zone> ZONES = new HashMap<>();

    private FishingWeatherManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(FishingWeatherManager::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ZONES.clear());
    }

    public static void clearPlayer(UUID playerUuid) {
        if (playerUuid != null) ZONES.remove(playerUuid);
    }

    public static void cast(
            ServerPlayerEntity player,
            Mode mode,
            BlockPos center,
            boolean correspondingSeal,
            boolean harmonized
    ) {
        ServerWorld world = player.getServerWorld();
        int radius = harmonized ? HARMONIZED_RADIUS : BASE_RADIUS;
        long duration = correspondingSeal ? SEALED_DURATION_TICKS : BASE_DURATION_TICKS;
        Zone zone = new Zone(
                player.getUuid(),
                world.getRegistryKey(),
                center.toImmutable(),
                mode,
                correspondingSeal,
                radius,
                world.getTime() + duration
        );
        ZONES.put(player.getUuid(), zone);
        world.playSound(
                null,
                center,
                mode == Mode.STORM ? SoundEvents.ITEM_TRIDENT_HIT : SoundEvents.ITEM_TRIDENT_RETURN,
                SoundCategory.PLAYERS,
                mode == Mode.STORM ? 0.85F : 0.7F,
                mode == Mode.STORM ? 0.75F : mode == Mode.SUN ? 1.35F : 1.05F
        );
        renderCastBurst(world, zone);
    }

    /** Nearest active local mode, regardless of owner. Used by weather effects and the Nessie charm. */
    public static Mode modeAt(ServerWorld world, BlockPos pos) {
        long now = world.getTime();
        Mode result = null;
        double nearest = Double.MAX_VALUE;
        for (Zone zone : ZONES.values()) {
            if (!zone.isActive(world, now)) continue;
            double distance = zone.center.getSquaredDistance(pos);
            if (distance <= zone.radius * zone.radius && distance < nearest) {
                nearest = distance;
                result = zone.mode;
            }
        }
        return result;
    }

    /** Only the zone cast by this player can progress that player's legendary hunt gauges. */
    public static HuntWeather ownedHuntWeatherAt(ServerPlayerEntity player, BlockPos pos) {
        Zone zone = ZONES.get(player.getUuid());
        ServerWorld world = player.getServerWorld();
        if (zone == null || !zone.isActive(world, world.getTime())) return null;
        if (zone.center.getSquaredDistance(pos) > zone.radius * zone.radius) return null;
        return new HuntWeather(zone.mode, zone.correspondingSeal);
    }


    private static void tick(MinecraftServer server) {
        Iterator<Zone> iterator = ZONES.values().iterator();
        while (iterator.hasNext()) {
            Zone zone = iterator.next();
            ServerWorld world = server.getWorld(zone.world);
            if (world == null || zone.expiresAt < world.getTime()) {
                iterator.remove();
                continue;
            }
            renderZone(world, zone);
        }
    }

    private static void renderCastBurst(ServerWorld world, Zone zone) {
        int points = 24;
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0D * index / points;
            double x = zone.center.getX() + 0.5D + Math.cos(angle) * zone.radius;
            double z = zone.center.getZ() + 0.5D + Math.sin(angle) * zone.radius;
            world.spawnParticles(
                    zone.mode == Mode.STORM ? ParticleTypes.ELECTRIC_SPARK
                            : zone.mode == Mode.SUN ? ParticleTypes.END_ROD
                            : ParticleTypes.SPLASH,
                    x,
                    zone.center.getY() + 1.2D,
                    z,
                    1,
                    0.05D,
                    0.1D,
                    0.05D,
                    0.01D
            );
        }
    }

    private static void renderZone(ServerWorld world, Zone zone) {
        long interval = zone.mode == Mode.STORM ? 10L : 16L;
        if (Math.floorMod(world.getTime() + zone.center.asLong(), interval) != 0L) return;

        for (ServerPlayerEntity player : world.getPlayers(candidate ->
                candidate.squaredDistanceTo(
                        zone.center.getX() + 0.5D,
                        zone.center.getY() + 0.5D,
                        zone.center.getZ() + 0.5D
                ) <= (zone.radius + 8.0D) * (zone.radius + 8.0D))) {
            if (zone.mode == Mode.SUN) {
                world.spawnParticles(
                        player,
                        ParticleTypes.END_ROD,
                        true,
                        zone.center.getX() + 0.5D,
                        zone.center.getY() + 3.0D,
                        zone.center.getZ() + 0.5D,
                        4,
                        zone.radius / 2.6D,
                        1.3D,
                        zone.radius / 2.6D,
                        0.008D
                );
            } else {
                world.spawnParticles(
                        player,
                        ParticleTypes.RAIN,
                        true,
                        zone.center.getX() + 0.5D,
                        zone.center.getY() + 5.0D,
                        zone.center.getZ() + 0.5D,
                        zone.mode == Mode.STORM ? 22 : 14,
                        zone.radius / 2.0D,
                        2.5D,
                        zone.radius / 2.0D,
                        0.05D
                );
                if (zone.mode == Mode.STORM) {
                    world.spawnParticles(
                            player,
                            ParticleTypes.ELECTRIC_SPARK,
                            true,
                            zone.center.getX() + 0.5D,
                            zone.center.getY() + 2.0D,
                            zone.center.getZ() + 0.5D,
                            4,
                            zone.radius / 2.0D,
                            1.5D,
                            zone.radius / 2.0D,
                            0.04D
                    );
                }
            }

            if (world.getTime() % 40L == 0L) {
                int ringPoints = 4;
                for (int index = 0; index < ringPoints; index++) {
                    double angle = Math.PI * 2.0D * index / ringPoints;
                    world.spawnParticles(
                            player,
                            zone.mode == Mode.STORM ? ParticleTypes.ELECTRIC_SPARK
                                    : zone.mode == Mode.SUN ? ParticleTypes.END_ROD
                                    : ParticleTypes.SPLASH,
                            true,
                            zone.center.getX() + 0.5D + Math.cos(angle) * zone.radius,
                            zone.center.getY() + 0.7D,
                            zone.center.getZ() + 0.5D + Math.sin(angle) * zone.radius,
                            1,
                            0.02D,
                            0.04D,
                            0.02D,
                            0.0D
                    );
                }
            }
        }
    }

    public record HuntWeather(Mode mode, boolean correspondingSeal) {
    }


    private record Zone(
            UUID owner,
            RegistryKey<World> world,
            BlockPos center,
            Mode mode,
            boolean correspondingSeal,
            int radius,
            long expiresAt
    ) {
        private boolean isActive(ServerWorld candidateWorld, long now) {
            return expiresAt >= now && world.equals(candidateWorld.getRegistryKey());
        }
    }
}
