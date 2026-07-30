package com.mythicrpg.fighting.barons;

import com.mythicrpg.core.EntityCooldownManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ThrowerBaronBehavior {

    private static final double MOB_SCAN_RADIUS = 32.0;
    private static final double PLAYER_TARGET_RADIUS = 24.0;
    private static final int WINDUP_TICKS = 12;
    private static final int COOLDOWN_TICKS = 160;
    private static final int SEARCH_INTERVAL_TICKS = 30;

    private static final double THROW_SPEED = 3.0;
    private static final double THROW_UPWARD_SPEED = 0.75;

    private static final String COOLDOWN_THROWER = "baron_thrower_spider";
    private static final String COOLDOWN_THROWER_SEARCH = "baron_thrower_spider_search";

    private static final Map<UUID, ThrowData> THROW_DATA = new HashMap<>();

    private ThrowerBaronBehavior() {
    }

    public static void tick(ServerWorld world, SpiderEntity spider) {
        ThrowData data = THROW_DATA.get(spider.getUuid());

        if (data != null) {
            tickWindup(world, spider, data);
            return;
        }

        tryStartThrow(world, spider);
    }

    public static void cleanup(Entity entity) {
        THROW_DATA.remove(entity.getUuid());
    }

    public static void clearAll() {
        THROW_DATA.clear();
    }

    private static void tryStartThrow(ServerWorld world, SpiderEntity spider) {
        int throwCooldownTicks = BaronScaling.getThrowerCooldownTicks(spider, COOLDOWN_TICKS);

        if (EntityCooldownManager.isOnCooldown(spider, COOLDOWN_THROWER, throwCooldownTicks)) {
            return;
        }

        int searchIntervalTicks = BaronScaling.getThrowerSearchIntervalTicks(spider, SEARCH_INTERVAL_TICKS);

        if (!EntityCooldownManager.tryUse(spider, COOLDOWN_THROWER_SEARCH, searchIntervalTicks)) {
            return;
        }

        ServerPlayerEntity player = findTargetPlayer(world, spider);

        if (player == null) {
            return;
        }

        LivingEntity projectileMob = findMobToThrow(world, spider, player);

        if (projectileMob == null) {
            return;
        }

        if (!EntityCooldownManager.tryUse(spider, COOLDOWN_THROWER, throwCooldownTicks)) {
            return;
        }

        THROW_DATA.put(
                spider.getUuid(),
                new ThrowData(projectileMob, player, world.getTime())
        );

        spider.setVelocity(Vec3d.ZERO);
        spider.getLookControl().lookAt(projectileMob, 45.0f, 45.0f);

        world.spawnParticles(
                ParticleTypes.POOF,
                projectileMob.getX(),
                projectileMob.getBodyY(0.55),
                projectileMob.getZ(),
                12,
                0.35,
                0.35,
                0.35,
                0.02
        );

        world.spawnParticles(
                ParticleTypes.CRIT,
                projectileMob.getX(),
                projectileMob.getBodyY(0.75),
                projectileMob.getZ(),
                8,
                0.25,
                0.25,
                0.25,
                0.02
        );

        world.spawnParticles(
                ParticleTypes.CLOUD,
                spider.getX(),
                spider.getBodyY(0.75),
                spider.getZ(),
                10,
                0.4,
                0.25,
                0.4,
                0.03
        );

        world.playSound(
                null,
                spider.getX(),
                spider.getY(),
                spider.getZ(),
                SoundEvents.ENTITY_SPIDER_AMBIENT,
                SoundCategory.HOSTILE,
                0.6f,
                0.75f
        );
    }

    private static void tickWindup(ServerWorld world, SpiderEntity spider, ThrowData data) {
        LivingEntity mobToThrow = data.mobToThrow;
        ServerPlayerEntity player = data.player;

        if (!mobToThrow.isAlive()
                || mobToThrow.isRemoved()
                || mobToThrow.getWorld() != world
                || !BaronEntityQuery.isValidPlayerTarget(player)
                || player.getWorld() != world) {
            THROW_DATA.remove(spider.getUuid());
            return;
        }

        long elapsed = world.getTime() - data.startTick;

        spider.setVelocity(Vec3d.ZERO);
        spider.getLookControl().lookAt(mobToThrow, 45.0f, 45.0f);

        if (elapsed % 4 == 0) {
            world.spawnParticles(
                    ParticleTypes.POOF,
                    mobToThrow.getX(),
                    mobToThrow.getBodyY(0.6),
                    mobToThrow.getZ(),
                    6,
                    0.25,
                    0.25,
                    0.25,
                    0.02
            );

            world.spawnParticles(
                    ParticleTypes.CRIT,
                    mobToThrow.getX(),
                    mobToThrow.getBodyY(0.7),
                    mobToThrow.getZ(),
                    3,
                    0.18,
                    0.18,
                    0.18,
                    0.01
            );

            world.spawnParticles(
                    ParticleTypes.CLOUD,
                    spider.getX(),
                    spider.getBodyY(0.25),
                    spider.getZ(),
                    5,
                    0.35,
                    0.08,
                    0.35,
                    0.02
            );
        }

        if (elapsed < WINDUP_TICKS) {
            return;
        }

        launchMob(world, spider, mobToThrow, player);
        THROW_DATA.remove(spider.getUuid());
    }

    private static ServerPlayerEntity findTargetPlayer(ServerWorld world, SpiderEntity spider) {
        if (spider.getTarget() instanceof ServerPlayerEntity target
                && BaronEntityQuery.isValidPlayerTarget(target)
                && spider.squaredDistanceTo(target) <= PLAYER_TARGET_RADIUS * PLAYER_TARGET_RADIUS) {
            return target;
        }

        ServerPlayerEntity nearest = null;
        double nearestDistanceSquared = PLAYER_TARGET_RADIUS * PLAYER_TARGET_RADIUS;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!BaronEntityQuery.isValidPlayerTarget(player)) {
                continue;
            }

            double distanceSquared = spider.squaredDistanceTo(player);
            if (distanceSquared > nearestDistanceSquared) {
                continue;
            }

            nearestDistanceSquared = distanceSquared;
            nearest = player;
        }

        return nearest;
    }

    private static LivingEntity findMobToThrow(
            ServerWorld world,
            SpiderEntity spider,
            ServerPlayerEntity player
    ) {
        Box box = spider.getBoundingBox().expand(MOB_SCAN_RADIUS);
        List<LivingEntity> candidates = world.getEntitiesByClass(
                LivingEntity.class,
                box,
                entity -> isValidThrownMob(spider, entity)
        );

        LivingEntity bestCandidate = null;
        double bestScore = Double.MAX_VALUE;

        for (LivingEntity candidate : candidates) {
            double score = scoreCandidate(spider, player, candidate);

            if (score >= bestScore) {
                continue;
            }

            bestScore = score;
            bestCandidate = candidate;
        }

        return bestCandidate;
    }

    private static boolean isValidThrownMob(SpiderEntity spider, LivingEntity entity) {
        if (entity == spider) {
            return false;
        }

        if (!entity.isAlive()) {
            return false;
        }

        if (entity instanceof PlayerEntity) {
            return false;
        }

        if (entity.hasVehicle() || entity.hasPassengers()) {
            return false;
        }

        return entity instanceof HostileEntity || entity instanceof IronGolemEntity;
    }

    private static double scoreCandidate(
            SpiderEntity spider,
            ServerPlayerEntity player,
            LivingEntity entity
    ) {
        return entity.squaredDistanceTo(spider) * 0.75
                + entity.squaredDistanceTo(player) * 0.25;
    }

    private static void launchMob(
            ServerWorld world,
            SpiderEntity spider,
            LivingEntity mobToThrow,
            ServerPlayerEntity player
    ) {
        Vec3d direction = player.getPos().add(0.0, 0.8, 0.0).subtract(mobToThrow.getPos());

        if (direction.lengthSquared() < 0.001) {
            direction = player.getRotationVec(1.0f).negate();
        }

        direction = direction.normalize();

        mobToThrow.setVelocity(
                direction.x * THROW_SPEED,
                THROW_UPWARD_SPEED + Math.max(0.0, direction.y) * 0.25,
                direction.z * THROW_SPEED
        );
        mobToThrow.velocityModified = true;

        world.playSound(
                null,
                mobToThrow.getX(),
                mobToThrow.getY(),
                mobToThrow.getZ(),
                SoundEvents.ENTITY_SLIME_JUMP,
                SoundCategory.HOSTILE,
                1.0f,
                0.75f
        );

        world.spawnParticles(
                ParticleTypes.POOF,
                mobToThrow.getX(),
                mobToThrow.getBodyY(0.5),
                mobToThrow.getZ(),
                25,
                0.35,
                0.25,
                0.35,
                0.08
        );

        world.spawnParticles(
                ParticleTypes.WITCH,
                spider.getX(),
                spider.getBodyY(0.65),
                spider.getZ(),
                18,
                0.4,
                0.25,
                0.4,
                0.05
        );
    }

    private static final class ThrowData {
        private final LivingEntity mobToThrow;
        private final ServerPlayerEntity player;
        private final long startTick;

        private ThrowData(
                LivingEntity mobToThrow,
                ServerPlayerEntity player,
                long startTick
        ) {
            this.mobToThrow = mobToThrow;
            this.player = player;
            this.startTick = startTick;
        }
    }
}
