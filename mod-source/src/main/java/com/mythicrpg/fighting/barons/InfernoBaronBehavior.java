package com.mythicrpg.fighting.barons;

import com.mythicrpg.mixin.GuardianEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InfernoBaronBehavior {

    private static final double RANGE = 20.0;
    private static final int DAMAGE_INTERVAL_TICKS = 5;
    private static final int RESET_GRACE_TICKS = 30;

    private static final float BASE_DAMAGE = 1.5f;
    private static final float DAMAGE_PER_STACK = 2.0f;
    private static final float MAX_DAMAGE = 20.0f;

    private static final Map<UUID, InfernoData> DATA = new HashMap<>();

    private InfernoBaronBehavior() {
    }

    public static void tick(ServerWorld world, GuardianEntity guardian) {
        ServerPlayerEntity target = findOrKeepTarget(world, guardian);

        if (target == null) {
            resetIfExpired(world, guardian);
            return;
        }

        long now = world.getTime();
        InfernoData data = DATA.computeIfAbsent(
                guardian.getUuid(),
                uuid -> new InfernoData(target.getUuid(), now, now, 0.0)
        );

        if (!data.targetUuid.equals(target.getUuid())) {
            data.targetUuid = target.getUuid();
            data.lastDamageTick = now;
            data.lastSeenTick = now;
            data.stack = 0.0;
        }

        data.lastSeenTick = now;

        keepVanillaBeamLocked(guardian, target);
        spawnBeamParticles(world, guardian, target, data.stack);

        if (now - data.lastDamageTick < DAMAGE_INTERVAL_TICKS) {
            return;
        }

        data.lastDamageTick = now;

        float damage = (float) Math.min(MAX_DAMAGE, BASE_DAMAGE + data.stack * DAMAGE_PER_STACK);
        target.damage(world.getDamageSources().mobAttack(guardian), damage);
        spawnHitParticles(world, target, data.stack);

        data.stack += BaronScaling.getInfernoRampSpeedMultiplier(guardian);
    }

    public static void cleanup(Entity entity) {
        DATA.remove(entity.getUuid());
    }

    public static void clearAll() {
        DATA.clear();
    }

    private static ServerPlayerEntity findOrKeepTarget(ServerWorld world, GuardianEntity guardian) {
        InfernoData data = DATA.get(guardian.getUuid());

        if (data != null) {
            Entity lockedEntity = world.getEntity(data.targetUuid);

            if (lockedEntity instanceof ServerPlayerEntity lockedTarget
                    && isValidInfernoTarget(guardian, lockedTarget)) {
                return lockedTarget;
            }
        }

        LivingEntity beamTarget = guardian.getBeamTarget();
        if (beamTarget instanceof ServerPlayerEntity beamPlayer
                && isValidInfernoTarget(guardian, beamPlayer)) {
            return beamPlayer;
        }

        if (guardian.getTarget() instanceof ServerPlayerEntity vanillaTarget
                && isValidInfernoTarget(guardian, vanillaTarget)) {
            return vanillaTarget;
        }

        ServerPlayerEntity nearest = null;
        double nearestDistanceSquared = RANGE * RANGE;

        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!isValidInfernoTarget(guardian, player)) {
                continue;
            }

            double distanceSquared = guardian.squaredDistanceTo(player);
            if (distanceSquared > nearestDistanceSquared) {
                continue;
            }

            nearestDistanceSquared = distanceSquared;
            nearest = player;
        }

        return nearest;
    }

    private static boolean isValidInfernoTarget(GuardianEntity guardian, ServerPlayerEntity player) {
        return BaronEntityQuery.isValidPlayerTarget(player)
                && guardian.squaredDistanceTo(player) <= RANGE * RANGE
                && guardian.canSee(player);
    }

    private static void keepVanillaBeamLocked(GuardianEntity guardian, ServerPlayerEntity target) {
        guardian.setTarget(target);
        ((GuardianEntityAccessor) guardian).mythicrpg$setBeamTarget(target.getId());
        guardian.getLookControl().lookAt(target, 30.0f, 30.0f);
    }

    private static void resetIfExpired(ServerWorld world, GuardianEntity guardian) {
        InfernoData data = DATA.get(guardian.getUuid());

        if (data == null) {
            return;
        }

        if (world.getTime() - data.lastSeenTick >= RESET_GRACE_TICKS) {
            DATA.remove(guardian.getUuid());
        }
    }

    private static void spawnBeamParticles(
            ServerWorld world,
            GuardianEntity guardian,
            LivingEntity target,
            double stack
    ) {
        Vec3d start = guardian.getEyePos();
        Vec3d end = target.getEyePos();
        Vec3d delta = end.subtract(start);

        if (delta.lengthSquared() < 0.001) {
            return;
        }

        int particles = Math.min(12, 5 + (int) Math.ceil(stack));

        for (int i = 1; i <= particles; i++) {
            double progress = i / (double) (particles + 1);
            Vec3d pos = start.add(delta.multiply(progress));

            world.spawnParticles(
                    ParticleTypes.FLAME,
                    pos.x,
                    pos.y,
                    pos.z,
                    1,
                    0.02,
                    0.02,
                    0.02,
                    0.002
            );
        }
    }

    private static void spawnHitParticles(ServerWorld world, LivingEntity target, double stack) {
        world.spawnParticles(
                ParticleTypes.FLAME,
                target.getX(),
                target.getBodyY(0.65),
                target.getZ(),
                Math.min(22, 8 + (int) Math.ceil(stack * 2.0)),
                0.22,
                0.3,
                0.22,
                0.025
        );
    }

    private static final class InfernoData {
        private UUID targetUuid;
        private long lastDamageTick;
        private long lastSeenTick;
        private double stack;

        private InfernoData(UUID targetUuid, long lastDamageTick, long lastSeenTick, double stack) {
            this.targetUuid = targetUuid;
            this.lastDamageTick = lastDamageTick;
            this.lastSeenTick = lastSeenTick;
            this.stack = stack;
        }
    }
}
