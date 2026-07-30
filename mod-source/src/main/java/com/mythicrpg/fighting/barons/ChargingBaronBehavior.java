package com.mythicrpg.fighting.barons;

import com.mythicrpg.core.EntityCooldownManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChargingBaronBehavior {

    private static final double TRIGGER_RANGE = 18.0;
    private static final double MIN_RANGE = 4.0;

    private static final int WINDUP_TICKS = 24;
    private static final int DASH_TICKS = 10;
    private static final int RECOVERY_TICKS = 28;
    private static final int COOLDOWN_TICKS = 90;

    private static final double DASH_SPEED = 1.05;
    private static final double HIT_RANGE = 2.4;
    private static final float DAMAGE = 7.0f;
    private static final String COOLDOWN_CHARGING_BARON = "baron_charging_baron";

    private static final Map<UUID, ChargeData> CHARGE_DATA = new HashMap<>();

    private ChargingBaronBehavior() {
    }

    public static void tick(ServerWorld world, LivingEntity charger) {
        ChargeData data = CHARGE_DATA.get(charger.getUuid());

        if (data != null) {
            tickExistingCharge(charger, world, data);
            return;
        }

        tryStartWindup(charger, world);
    }

    public static void cleanup(Entity entity) {
        CHARGE_DATA.remove(entity.getUuid());
    }

    public static void clearAll() {
        CHARGE_DATA.clear();
    }

    private static void tryStartWindup(LivingEntity charger, ServerWorld world) {
        if (!(charger instanceof MobEntity mob)) {
            return;
        }

        if (!(mob.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }

        if (!BaronEntityQuery.isValidPlayerTarget(target)) {
            return;
        }

        double distanceSquared = charger.squaredDistanceTo(target);

        if (distanceSquared > TRIGGER_RANGE * TRIGGER_RANGE) {
            return;
        }

        if (distanceSquared < MIN_RANGE * MIN_RANGE) {
            return;
        }

        if (EntityCooldownManager.isOnCooldown(charger, COOLDOWN_CHARGING_BARON, COOLDOWN_TICKS)) {
            return;
        }

        Vec3d direction = target.getPos().subtract(charger.getPos());
        direction = new Vec3d(direction.x, 0.0, direction.z);

        if (direction.lengthSquared() <= 0.001) {
            return;
        }

        direction = direction.normalize();

        CHARGE_DATA.put(
                charger.getUuid(),
                new ChargeData(
                        ChargePhase.WINDUP,
                        world.getTime(),
                        direction,
                        false
                )
        );

        charger.setVelocity(Vec3d.ZERO);

        world.spawnParticles(
                ParticleTypes.ANGRY_VILLAGER,
                charger.getX(),
                charger.getBodyY(0.9),
                charger.getZ(),
                18,
                0.45,
                0.35,
                0.45,
                0.04
        );

        world.playSound(
                null,
                charger.getBlockPos(),
                SoundEvents.ENTITY_RAVAGER_ROAR,
                SoundCategory.HOSTILE,
                0.8f,
                1.25f
        );
    }

    private static void tickExistingCharge(
            LivingEntity charger,
            ServerWorld world,
            ChargeData data
    ) {
        long elapsed = world.getTime() - data.phaseStartTick;

        if (data.phase == ChargePhase.WINDUP) {
            tickWindup(charger, world, data, elapsed);
            return;
        }

        if (data.phase == ChargePhase.DASH) {
            tickDash(charger, world, data, elapsed);
            return;
        }

        if (data.phase == ChargePhase.RECOVERY) {
            tickRecovery(charger, world, data, elapsed);
        }
    }

    private static void tickWindup(
            LivingEntity charger,
            ServerWorld world,
            ChargeData data,
            long elapsed
    ) {
        charger.setVelocity(Vec3d.ZERO);

        if (elapsed % 4 == 0) {
            world.spawnParticles(
                    ParticleTypes.CRIT,
                    charger.getX(),
                    charger.getBodyY(0.8),
                    charger.getZ(),
                    8,
                    0.35,
                    0.25,
                    0.35,
                    0.03
            );

            world.spawnParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    charger.getX(),
                    charger.getBodyY(1.0),
                    charger.getZ(),
                    4,
                    0.3,
                    0.25,
                    0.3,
                    0.02
            );
        }

        if (elapsed < WINDUP_TICKS) {
            return;
        }

        data.phase = ChargePhase.DASH;
        data.phaseStartTick = world.getTime();

        charger.setVelocity(
                data.direction.x * DASH_SPEED,
                0.08,
                data.direction.z * DASH_SPEED
        );

        world.spawnParticles(
                ParticleTypes.CLOUD,
                charger.getX() - data.direction.x * 0.8,
                charger.getBodyY(0.3),
                charger.getZ() - data.direction.z * 0.8,
                25,
                0.35,
                0.2,
                0.35,
                0.08
        );

        world.playSound(
                null,
                charger.getBlockPos(),
                SoundEvents.ENTITY_RAVAGER_ATTACK,
                SoundCategory.HOSTILE,
                1.0f,
                0.8f
        );
    }

    private static void tickDash(
            LivingEntity charger,
            ServerWorld world,
            ChargeData data,
            long elapsed
    ) {
        charger.setVelocity(
                data.direction.x * DASH_SPEED,
                charger.getVelocity().y,
                data.direction.z * DASH_SPEED
        );

        if (elapsed % 2 == 0) {
            world.spawnParticles(
                    ParticleTypes.CLOUD,
                    charger.getX() - data.direction.x * 0.5,
                    charger.getBodyY(0.25),
                    charger.getZ() - data.direction.z * 0.5,
                    8,
                    0.25,
                    0.15,
                    0.25,
                    0.04
            );
        }

        tryImpact(charger, world, data);

        int dashTicks = Math.max(1, (int) Math.round(DASH_TICKS * BaronScaling.getChargingDistanceMultiplier(charger)));

        if (elapsed < dashTicks) {
            return;
        }

        startRecovery(charger, world, data);
    }

    private static void tryImpact(
            LivingEntity charger,
            ServerWorld world,
            ChargeData data
    ) {
        if (data.hasHit) {
            return;
        }

        if (!(charger instanceof MobEntity mob)) {
            return;
        }

        if (!(mob.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }

        if (!BaronEntityQuery.isValidPlayerTarget(target)) {
            return;
        }

        if (charger.squaredDistanceTo(target) > HIT_RANGE * HIT_RANGE) {
            return;
        }

        data.hasHit = true;
        target.damage(
                world.getDamageSources().mobAttack(charger),
                (float) (DAMAGE * BaronScaling.getChargingDamageMultiplier(charger))
        );

        Vec3d knockback = target.getPos().subtract(charger.getPos());

        if (knockback.lengthSquared() > 0.001) {
            knockback = new Vec3d(knockback.x, 0.0, knockback.z).normalize();

            target.addVelocity(
                    knockback.x * 1.0,
                    0.45,
                    knockback.z * 1.0
            );
            target.velocityModified = true;
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 35, 4));
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 35, 0));

        world.spawnParticles(
                ParticleTypes.EXPLOSION,
                target.getX(),
                target.getBodyY(0.5),
                target.getZ(),
                1,
                0.0,
                0.0,
                0.0,
                0.0
        );

        world.playSound(
                null,
                target.getBlockPos(),
                SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK,
                SoundCategory.HOSTILE,
                1.0f,
                0.75f
        );

        startRecovery(charger, world, data);
    }

    private static void startRecovery(
            LivingEntity charger,
            ServerWorld world,
            ChargeData data
    ) {
        data.phase = ChargePhase.RECOVERY;
        data.phaseStartTick = world.getTime();

        charger.setVelocity(Vec3d.ZERO);
        charger.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, RECOVERY_TICKS, 6));

        world.spawnParticles(
                ParticleTypes.SMOKE,
                charger.getX(),
                charger.getBodyY(0.6),
                charger.getZ(),
                12,
                0.35,
                0.25,
                0.35,
                0.03
        );
    }

    private static void tickRecovery(
            LivingEntity charger,
            ServerWorld world,
            ChargeData data,
            long elapsed
    ) {
        charger.setVelocity(Vec3d.ZERO);

        if (elapsed < RECOVERY_TICKS) {
            return;
        }

        CHARGE_DATA.remove(charger.getUuid());
        EntityCooldownManager.markUsed(charger, COOLDOWN_CHARGING_BARON);
    }

    private enum ChargePhase {
        WINDUP,
        DASH,
        RECOVERY
    }

    private static final class ChargeData {
        private ChargePhase phase;
        private long phaseStartTick;
        private final Vec3d direction;
        private boolean hasHit;

        private ChargeData(
                ChargePhase phase,
                long phaseStartTick,
                Vec3d direction,
                boolean hasHit
        ) {
            this.phase = phase;
            this.phaseStartTick = phaseStartTick;
            this.direction = direction;
            this.hasHit = hasHit;
        }
    }
}
