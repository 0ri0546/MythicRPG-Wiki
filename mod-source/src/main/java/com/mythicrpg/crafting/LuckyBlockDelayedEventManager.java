package com.mythicrpg.crafting;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import com.mythicrpg.MythicRPG;

public final class LuckyBlockDelayedEventManager {

    private static final List<LuckyBlockTask> TASKS = new ArrayList<>();

    private LuckyBlockDelayedEventManager() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(LuckyBlockDelayedEventManager::tick);
    }

    public static void scheduleSafeTnt(
            ServerWorld world,
            UUID tntUuid,
            BlockPos fallbackPos,
            int delayTicks
    ) {
        TASKS.add(new SafeTntTask(
                world.getRegistryKey(),
                tntUuid,
                fallbackPos.toImmutable(),
                delayTicks
        ));
    }

    public static void scheduleBaronRitual(
            ServerWorld world,
            BlockPos pos,
            UUID playerUuid
    ) {
        TASKS.add(new BaronRitualTask(
                world.getRegistryKey(),
                pos.toImmutable(),
                playerUuid
        ));
    }

    public static void scheduleTemple(
            ServerWorld world,
            BlockPos pos,
            Direction playerFacing
    ) {
        TASKS.add(new StructureTask(
                world.getRegistryKey(),
                pos.toImmutable(),
                playerFacing,
                StructureType.TEMPLE
        ));
    }

    public static void scheduleCoinFlipChoice(
            ServerWorld world,
            BlockPos pos,
            Direction playerFacing
    ) {
        TASKS.add(new StructureTask(
                world.getRegistryKey(),
                pos.toImmutable(),
                playerFacing,
                StructureType.COIN_FLIP_CHOICE
        ));
    }

    private static void tick(MinecraftServer server) {
        Iterator<LuckyBlockTask> iterator = TASKS.iterator();

        while (iterator.hasNext()) {
            LuckyBlockTask task = iterator.next();

            try {
                if (task.tick(server)) {
                    iterator.remove();
                }
            } catch (Exception exception) {
                MythicRPG.LOGGER.warn("Lucky Block delayed task failed and was removed.", exception);
                iterator.remove();
            }
        }
    }

    private interface LuckyBlockTask {
        boolean tick(MinecraftServer server);
    }

    private enum StructureType {
        TEMPLE,
        COIN_FLIP_CHOICE
    }

    private static final class StructureTask implements LuckyBlockTask {

        private final RegistryKey<World> worldKey;
        private final BlockPos pos;
        private final Direction playerFacing;
        private final StructureType type;

        private StructureTask(
                RegistryKey<World> worldKey,
                BlockPos pos,
                Direction playerFacing,
                StructureType type
        ) {
            this.worldKey = worldKey;
            this.pos = pos;
            this.playerFacing = playerFacing;
            this.type = type;
        }

        @Override
        public boolean tick(MinecraftServer server) {
            ServerWorld world = server.getWorld(worldKey);

            if (world == null) {
                return true;
            }

            switch (type) {
                case TEMPLE -> LuckyBlockStructureGenerator.generateTemple(world, pos, playerFacing);
                case COIN_FLIP_CHOICE -> LuckyBlockStructureGenerator.generateCoinFlipChoice(
                        world,
                        pos,
                        playerFacing
                );
            }

            return true;
        }
    }

    private static final class SafeTntTask implements LuckyBlockTask {

        private final RegistryKey<World> worldKey;
        private final UUID tntUuid;
        private final BlockPos fallbackPos;
        private int ticksLeft;

        private SafeTntTask(
                RegistryKey<World> worldKey,
                UUID tntUuid,
                BlockPos fallbackPos,
                int ticksLeft
        ) {
            this.worldKey = worldKey;
            this.tntUuid = tntUuid;
            this.fallbackPos = fallbackPos;
            this.ticksLeft = ticksLeft;
        }

        @Override
        public boolean tick(MinecraftServer server) {
            ServerWorld world = server.getWorld(worldKey);

            if (world == null) {
                return true;
            }

            ticksLeft--;

            if (ticksLeft > 0) {
                return false;
            }

            Entity tnt = world.getEntity(tntUuid);
            Vec3d explosionPos = tnt == null
                    ? Vec3d.ofCenter(fallbackPos)
                    : tnt.getPos();

            if (tnt != null) {
                tnt.discard();
            }

            world.createExplosion(
                    null,
                    explosionPos.x,
                    explosionPos.y,
                    explosionPos.z,
                    4.0f,
                    World.ExplosionSourceType.NONE
            );

            return true;
        }
    }

    private static final class BaronRitualTask implements LuckyBlockTask {

        private static final int DURATION_TICKS = 20 * 3;

        private final RegistryKey<World> worldKey;
        private final BlockPos pos;
        private final UUID playerUuid;
        private int age;

        private BaronRitualTask(
                RegistryKey<World> worldKey,
                BlockPos pos,
                UUID playerUuid
        ) {
            this.worldKey = worldKey;
            this.pos = pos;
            this.playerUuid = playerUuid;
        }

        @Override
        public boolean tick(MinecraftServer server) {
            ServerWorld world = server.getWorld(worldKey);

            if (world == null) {
                return true;
            }

            age++;

            if (age == 1) {
                world.playSound(
                        null,
                        pos,
                        SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON,
                        SoundCategory.HOSTILE,
                        1.2f,
                        0.6f
                );
            }

            if (age % 3 == 0) {
                playRitualParticles(world, pos, age);
            }

            if (age == 40) {
                world.playSound(
                        null,
                        pos,
                        SoundEvents.BLOCK_BEACON_POWER_SELECT,
                        SoundCategory.HOSTILE,
                        1.1f,
                        0.7f
                );
            }

            if (age < DURATION_TICKS) {
                return false;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);

            world.spawnParticles(
                    ParticleTypes.SONIC_BOOM,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );

            world.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    pos.getX() + 0.5,
                    pos.getY() + 0.8,
                    pos.getZ() + 0.5,
                    80,
                    1.2,
                    0.8,
                    1.2,
                    0.08
            );

            world.playSound(
                    null,
                    pos,
                    SoundEvents.ENTITY_WITHER_SPAWN,
                    SoundCategory.HOSTILE,
                    0.9f,
                    1.25f
            );

            LuckyBlockBaronBridge.spawnRandomBaron(world, pos, player);

            return true;
        }

        private static void playRitualParticles(ServerWorld world, BlockPos pos, int age) {
            double centerX = pos.getX() + 0.5;
            double centerY = pos.getY() + 0.15;
            double centerZ = pos.getZ() + 0.5;

            double radius = 1.0 + (age / 60.0);

            for (int i = 0; i < 10; i++) {
                double angle = ((age * 0.25) + i) * Math.PI * 2.0 / 10.0;
                double x = centerX + Math.cos(angle) * radius;
                double z = centerZ + Math.sin(angle) * radius;

                world.spawnParticles(
                        ParticleTypes.SOUL,
                        x,
                        centerY,
                        z,
                        1,
                        0.0,
                        0.02,
                        0.0,
                        0.0
                );
            }

            world.spawnParticles(
                    ParticleTypes.REVERSE_PORTAL,
                    centerX,
                    pos.getY() + 1.0,
                    centerZ,
                    18,
                    0.55,
                    0.9,
                    0.55,
                    0.06
            );

            world.spawnParticles(
                    ParticleTypes.ENCHANT,
                    centerX,
                    pos.getY() + 0.8,
                    centerZ,
                    12,
                    0.7,
                    0.5,
                    0.7,
                    0.05
            );
        }
    }
}