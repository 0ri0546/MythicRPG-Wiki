package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BalloonBaronBehavior {

    private static final String BALLOON_FIREBALL_TAG = "mythicrpg_balloon_fireball";

    private static final double CLOUD_RADIUS = 3.2;
    private static final int CLOUD_DURATION_TICKS = 120;
    private static final int MAX_CLOUDS = 6;
    private static final int CLOUD_DAMAGE_INTERVAL_TICKS = 10;
    private static final float CLOUD_DAMAGE = 2.0f;

    private static final double GHAST_FALLBACK_RADIUS = 24.0;
    private static final int CLOUD_PARTICLE_INTERVAL_TICKS = 4;
    private static final int FIREBALL_OWNER_RESOLVE_GRACE_TICKS = 20;
    private static final int CLOUD_RING_POINTS = 16;

    private static final Map<UUID, TrackedBalloonFireball> TRACKED_FIREBALLS = new HashMap<>();
    private static final Map<UUID, PendingBalloonFireball> PENDING_FIREBALLS = new HashMap<>();
    private static final List<BalloonCloud> CLOUDS = new ArrayList<>();

    private static final DustParticleEffect PURPLE_DUST =
            new DustParticleEffect(new Vector3f(0.55f, 0.0f, 1.0f), 1.8f);

    private static final DustParticleEffect PINK_DUST =
            new DustParticleEffect(new Vector3f(1.0f, 0.1f, 0.85f), 1.4f);

    private BalloonBaronBehavior() {
    }

    public static void tickGlobal() {
        tickPendingFireballs();
        tickFireballs();
        tickClouds();
    }

    public static void clearAll() {
        PENDING_FIREBALLS.clear();
        TRACKED_FIREBALLS.clear();
        CLOUDS.clear();
    }

    public static void tryTrackFireball(Entity entity, ServerWorld world) {
        if (!(entity instanceof FireballEntity fireball)) {
            return;
        }

        if (TRACKED_FIREBALLS.containsKey(fireball.getUuid())) {
            return;
        }

        if (fireball.getCommandTags().contains(BALLOON_FIREBALL_TAG)) {
            trackFireball(fireball, world);
            return;
        }

        Entity owner = fireball.getOwner();

        if (owner instanceof GhastEntity ghast) {
            if (BaronMobManager.getBaronType(ghast) == BaronType.BALLOON) {
                trackFireball(
                        fireball,
                        world,
                        BaronScaling.getBalloonCloudMultiplier(ghast)
                );
            }
            return;
        }

        if (owner == null) {
            PENDING_FIREBALLS.put(
                    fireball.getUuid(),
                    new PendingBalloonFireball(fireball, world, world.getTime())
            );
        }
    }

    public static void cleanup(Entity entity) {
        PendingBalloonFireball pending = PENDING_FIREBALLS.remove(entity.getUuid());
        TrackedBalloonFireball tracked = TRACKED_FIREBALLS.remove(entity.getUuid());

        if (!shouldSpawnCloudOnRemoval(entity)) {
            return;
        }

        if (tracked != null) {
            spawnCloud(tracked.world, tracked.lastPosition, tracked.cloudMultiplier);
            return;
        }

        if (pending == null || !(entity instanceof FireballEntity fireball)) {
            return;
        }

        GhastEntity source = getBalloonGhastSource(fireball, pending.world);

        if (source != null) {
            spawnCloud(
                    pending.world,
                    fireball.getPos(),
                    BaronScaling.getBalloonCloudMultiplier(source)
            );
        }
    }

    private static void tickPendingFireballs() {
        Iterator<Map.Entry<UUID, PendingBalloonFireball>> iterator =
                PENDING_FIREBALLS.entrySet().iterator();

        while (iterator.hasNext()) {
            PendingBalloonFireball pending = iterator.next().getValue();
            FireballEntity fireball = pending.fireball;

            if (!fireball.isAlive() || fireball.isRemoved()) {
                iterator.remove();
                continue;
            }

            Entity owner = fireball.getOwner();

            if (owner instanceof GhastEntity ghast) {
                iterator.remove();

                if (BaronMobManager.getBaronType(ghast) == BaronType.BALLOON) {
                    trackFireball(
                            fireball,
                            pending.world,
                            BaronScaling.getBalloonCloudMultiplier(ghast)
                    );
                    spawnTrackingParticles(pending.world, fireball);
                }
                continue;
            }

            if (pending.world.getTime() - pending.loadTick < FIREBALL_OWNER_RESOLVE_GRACE_TICKS) {
                continue;
            }

            iterator.remove();

            GhastEntity nearbyBalloonGhast = findNearbyBalloonGhast(fireball, pending.world);

            if (nearbyBalloonGhast != null) {
                trackFireball(
                        fireball,
                        pending.world,
                        BaronScaling.getBalloonCloudMultiplier(nearbyBalloonGhast)
                );
                spawnTrackingParticles(pending.world, fireball);
            }
        }
    }

    private static void spawnTrackingParticles(ServerWorld world, FireballEntity fireball) {
        world.spawnParticles(
                PURPLE_DUST,
                fireball.getX(),
                fireball.getY(),
                fireball.getZ(),
                8,
                0.15,
                0.15,
                0.15,
                0.01
        );
    }

    private static void trackFireball(FireballEntity fireball, ServerWorld world) {
        GhastEntity source = getBalloonGhastSource(fireball, world);
        double cloudMultiplier = source == null
                ? 1.0
                : BaronScaling.getBalloonCloudMultiplier(source);

        trackFireball(fireball, world, cloudMultiplier);
    }

    private static void trackFireball(
            FireballEntity fireball,
            ServerWorld world,
            double cloudMultiplier
    ) {
        fireball.addCommandTag(BALLOON_FIREBALL_TAG);
        TRACKED_FIREBALLS.put(
                fireball.getUuid(),
                new TrackedBalloonFireball(
                        fireball,
                        world,
                        fireball.getPos(),
                        cloudMultiplier
                )
        );
    }

    private static void tickFireballs() {
        Iterator<Map.Entry<UUID, TrackedBalloonFireball>> iterator =
                TRACKED_FIREBALLS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackedBalloonFireball> entry = iterator.next();
            TrackedBalloonFireball tracked = entry.getValue();
            FireballEntity fireball = tracked.fireball;

            if (fireball.isAlive() && !fireball.isRemoved()) {
                tracked.lastPosition = fireball.getPos();
                continue;
            }

            iterator.remove();

            if (shouldSpawnCloudOnRemoval(fireball)) {
                spawnCloud(tracked.world, tracked.lastPosition, tracked.cloudMultiplier);
            }
        }
    }

    private static boolean shouldSpawnCloudOnRemoval(Entity entity) {
        Entity.RemovalReason removalReason = entity.getRemovalReason();

        if (removalReason == null) {
            return !entity.isAlive();
        }

        return removalReason.shouldDestroy();
    }

    private static GhastEntity getBalloonGhastSource(
            FireballEntity fireball,
            ServerWorld world
    ) {
        Entity owner = fireball.getOwner();

        if (owner instanceof GhastEntity ghast
                && BaronMobManager.getBaronType(ghast) == BaronType.BALLOON) {
            return ghast;
        }

        return findNearbyBalloonGhast(fireball, world);
    }

    private static GhastEntity findNearbyBalloonGhast(
            FireballEntity fireball,
            ServerWorld world
    ) {
        Box box = fireball.getBoundingBox().expand(GHAST_FALLBACK_RADIUS);
        List<GhastEntity> nearbyBalloonGhasts = world.getEntitiesByClass(
                GhastEntity.class,
                box,
                ghast -> ghast.isAlive()
                        && BaronMobManager.getBaronType(ghast) == BaronType.BALLOON
        );

        return nearbyBalloonGhasts.isEmpty() ? null : nearbyBalloonGhasts.getFirst();
    }

    private static void spawnCloud(ServerWorld world, Vec3d impactPosition, double cloudMultiplier) {
        Vec3d position = findGroundPosition(world, impactPosition);
        long now = world.getTime();
        long endTick = now + Math.max(1, (int) Math.round(CLOUD_DURATION_TICKS * cloudMultiplier));

        if (CLOUDS.size() >= MAX_CLOUDS) {
            CLOUDS.removeFirst();
        }

        CLOUDS.add(new BalloonCloud(
                world,
                position,
                endTick,
                now,
                (float) (CLOUD_DAMAGE * cloudMultiplier)
        ));

        world.spawnParticles(
                PURPLE_DUST,
                position.x,
                position.y + 0.2,
                position.z,
                80,
                1.0,
                0.25,
                1.0,
                0.02
        );

        world.spawnParticles(
                ParticleTypes.DRAGON_BREATH,
                position.x,
                position.y + 0.25,
                position.z,
                50,
                0.9,
                0.3,
                0.9,
                0.03
        );

        world.playSound(
                null,
                position.x,
                position.y,
                position.z,
                SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
                SoundCategory.HOSTILE,
                0.9f,
                1.4f
        );
    }

    private static Vec3d findGroundPosition(ServerWorld world, Vec3d position) {
        BlockPos pos = BlockPos.ofFloored(position);

        for (int i = 0; i < 12; i++) {
            if (!world.getBlockState(pos).isAir()) {
                BlockPos above = pos.up();
                return new Vec3d(
                        above.getX() + 0.5,
                        above.getY() + 0.05,
                        above.getZ() + 0.5
                );
            }

            pos = pos.down();
        }

        return position;
    }

    private static void tickClouds() {
        Iterator<BalloonCloud> iterator = CLOUDS.iterator();

        while (iterator.hasNext()) {
            BalloonCloud cloud = iterator.next();
            long now = cloud.world.getTime();

            if (now >= cloud.endTick) {
                iterator.remove();
                continue;
            }

            if (now % CLOUD_PARTICLE_INTERVAL_TICKS == 0) {
                spawnCloudParticles(cloud);
            }

            if (now >= cloud.nextDamageTick) {
                cloud.nextDamageTick = now + CLOUD_DAMAGE_INTERVAL_TICKS;
                damageEntitiesInsideCloud(cloud);
            }
        }
    }

    private static void spawnCloudParticles(BalloonCloud cloud) {
        ServerWorld world = cloud.world;
        Vec3d pos = cloud.position;

        long time = world.getTime();
        double baseAngle = time * 0.18;

        for (int i = 0; i < CLOUD_RING_POINTS; i++) {
            double angle = baseAngle + (Math.PI * 2.0 / CLOUD_RING_POINTS) * i;

            double x = pos.x + Math.cos(angle) * CLOUD_RADIUS;
            double z = pos.z + Math.sin(angle) * CLOUD_RADIUS;
            double y = pos.y + 0.08;

            world.spawnParticles(PURPLE_DUST, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);

            if (i % 4 == 0) {
                world.spawnParticles(ParticleTypes.DRAGON_BREATH, x, y + 0.15, z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        world.spawnParticles(
                PINK_DUST,
                pos.x,
                pos.y + 0.25,
                pos.z,
                6,
                CLOUD_RADIUS * 0.35,
                0.15,
                CLOUD_RADIUS * 0.35,
                0.01
        );

        world.spawnParticles(
                ParticleTypes.WITCH,
                pos.x,
                pos.y + 0.2,
                pos.z,
                3,
                CLOUD_RADIUS * 0.25,
                0.2,
                CLOUD_RADIUS * 0.25,
                0.02
        );
    }

    private static void damageEntitiesInsideCloud(BalloonCloud cloud) {
        ServerWorld world = cloud.world;
        Vec3d pos = cloud.position;

        Box box = new Box(
                pos.x - CLOUD_RADIUS,
                pos.y - 1.0,
                pos.z - CLOUD_RADIUS,
                pos.x + CLOUD_RADIUS,
                pos.y + 2.0,
                pos.z + CLOUD_RADIUS
        );

        List<LivingEntity> targets = world.getEntitiesByClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive()
                        && !(entity instanceof GhastEntity)
                        && entity.squaredDistanceTo(pos) <= CLOUD_RADIUS * CLOUD_RADIUS
        );

        for (LivingEntity target : targets) {
            BaronDeathMessageRegistry.rememberBaronDanger(target, BaronType.BALLOON);
            target.damage(world.getDamageSources().magic(), cloud.damage);
        }
    }

    private static final class PendingBalloonFireball {
        private final FireballEntity fireball;
        private final ServerWorld world;
        private final long loadTick;

        private PendingBalloonFireball(
                FireballEntity fireball,
                ServerWorld world,
                long loadTick
        ) {
            this.fireball = fireball;
            this.world = world;
            this.loadTick = loadTick;
        }
    }

    private static final class TrackedBalloonFireball {
        private final FireballEntity fireball;
        private final ServerWorld world;
        private Vec3d lastPosition;
        private final double cloudMultiplier;

        private TrackedBalloonFireball(
                FireballEntity fireball,
                ServerWorld world,
                Vec3d lastPosition,
                double cloudMultiplier
        ) {
            this.fireball = fireball;
            this.world = world;
            this.lastPosition = lastPosition;
            this.cloudMultiplier = cloudMultiplier;
        }
    }

    private static final class BalloonCloud {
        private final ServerWorld world;
        private final Vec3d position;
        private final long endTick;
        private final float damage;
        private long nextDamageTick;

        private BalloonCloud(
                ServerWorld world,
                Vec3d position,
                long endTick,
                long nextDamageTick,
                float damage
        ) {
            this.world = world;
            this.position = position;
            this.endTick = endTick;
            this.nextDamageTick = nextDamageTick;
            this.damage = damage;
        }
    }
}
