package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.mining.archaeology.polish.ArchaeologyPolishEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ColossalAegisManager {

    private static final Map<UUID, Float> STORED_FORCE = new HashMap<>();
    private static final List<ExpandingWave> ACTIVE_WAVES = new ArrayList<>();
    private static final Vector3f WAVE_COLOR = new Vector3f(0.78F, 0.90F, 1.00F);

    private ColossalAegisManager() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ColossalAegisManager::tickWaves);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                STORED_FORCE.remove(handler.player.getUuid())
        );
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                STORED_FORCE.remove(oldPlayer.getUuid())
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            STORED_FORCE.clear();
            ACTIVE_WAVES.clear();
        });
    }

    public static boolean active(LivingEntity entity) {
        return entity instanceof PlayerEntity player
                && player.isUsingItem()
                && player.getActiveItem().getItem() instanceof ColossalAegisItem;
    }

    public static void record(LivingEntity entity, float damage) {
        if (!(entity instanceof ServerPlayerEntity) || !active(entity) || damage <= 0.0F) {
            return;
        }

        STORED_FORCE.merge(entity.getUuid(), damage, Float::sum);
        if (entity.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(
                    new DustParticleEffect(WAVE_COLOR, 0.55F),
                    entity.getX(),
                    entity.getBodyY(0.55),
                    entity.getZ(),
                    3,
                    0.28,
                    0.38,
                    0.28,
                    0.015
            );
        }
    }

    public static double knockbackMultiplier(LivingEntity entity) {
        if (!active(entity)) {
            return 1.0;
        }
        int level = RelicItemData.getLevel(entity.getActiveItem()).value();
        return Math.max(0.1, 0.65 - level * 0.1);
    }

    public static void releaseWave(ServerPlayerEntity player, ItemStack stack) {
        float stored = STORED_FORCE.getOrDefault(player.getUuid(), 0.0F);
        STORED_FORCE.remove(player.getUuid());
        if (stored <= 0.0F || !(player.getWorld() instanceof ServerWorld world)) {
            return;
        }

        int level = RelicItemData.getLevel(stack).value();
        double radius = 2.5 + level * 0.6;
        double force = Math.min(2.2, 0.45 + stored * 0.045 + level * 0.12);
        Box box = player.getBoundingBox().expand(radius);

        for (LivingEntity target : world.getEntitiesByClass(
                LivingEntity.class,
                box,
                candidate -> candidate != player && candidate.isAlive()
        )) {
            Vec3d delta = target.getPos().subtract(player.getPos());
            double horizontalLength = Math.max(0.1, Math.sqrt(delta.x * delta.x + delta.z * delta.z));
            target.addVelocity(
                    delta.x / horizontalLength * force,
                    0.18 + level * 0.025,
                    delta.z / horizontalLength * force
            );
            target.velocityModified = true;
        }

        ACTIVE_WAVES.add(new ExpandingWave(
                world.getRegistryKey(),
                player.getPos(),
                radius,
                0.75,
                world.getTime()
        ));

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_IRON_GOLEM_ATTACK,
                SoundCategory.PLAYERS,
                0.85F + Math.min(0.35F, stored * 0.01F),
                0.72F + level * 0.025F
        );
    }

    private static void tickWaves(MinecraftServer server) {
        if (ACTIVE_WAVES.isEmpty()) {
            return;
        }

        Iterator<ExpandingWave> iterator = ACTIVE_WAVES.iterator();
        while (iterator.hasNext()) {
            ExpandingWave wave = iterator.next();
            ServerWorld world = server.getWorld(wave.dimension);
            if (world == null) {
                iterator.remove();
                continue;
            }
            if (world.getTime() < wave.nextStepAt) {
                continue;
            }

            DustParticleEffect dust = new DustParticleEffect(WAVE_COLOR, 0.8F);
            int points = Math.max(16, (int) Math.ceil(wave.currentRadius * 11.0));
            ArchaeologyPolishEffects.spawnHorizontalRing(
                    world,
                    dust,
                    wave.center,
                    wave.currentRadius,
                    points,
                    0.12
            );

            if (wave.currentRadius >= wave.maxRadius * 0.72) {
                ArchaeologyPolishEffects.spawnHorizontalRing(
                        world,
                        ParticleTypes.CLOUD,
                        wave.center,
                        wave.currentRadius,
                        Math.max(10, points / 3),
                        0.08
                );
            }

            wave.currentRadius += 0.62;
            wave.nextStepAt = world.getTime() + 2L;
            if (wave.currentRadius > wave.maxRadius + 0.25) {
                iterator.remove();
            }
        }
    }

    private static final class ExpandingWave {
        private final RegistryKey<World> dimension;
        private final Vec3d center;
        private final double maxRadius;
        private double currentRadius;
        private long nextStepAt;

        private ExpandingWave(
                RegistryKey<World> dimension,
                Vec3d center,
                double maxRadius,
                double currentRadius,
                long nextStepAt
        ) {
            this.dimension = dimension;
            this.center = center;
            this.maxRadius = maxRadius;
            this.currentRadius = currentRadius;
            this.nextStepAt = nextStepAt;
        }
    }
}
