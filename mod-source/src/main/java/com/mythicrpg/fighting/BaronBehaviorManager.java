package com.mythicrpg.fighting;

import com.mythicrpg.core.EntityCooldownManager;
import com.mythicrpg.fighting.barons.AlchemistBaronBehavior;
import com.mythicrpg.fighting.barons.BalloonBaronBehavior;
import com.mythicrpg.fighting.barons.BarrageBaronBehavior;
import com.mythicrpg.fighting.barons.BaronDeathMessageRegistry;
import com.mythicrpg.fighting.barons.BaronEntityQuery;
import com.mythicrpg.fighting.barons.ChargingBaronBehavior;
import com.mythicrpg.fighting.barons.DarknightBaronBehavior;
import com.mythicrpg.fighting.barons.DrownedKingBaronBehavior;
import com.mythicrpg.fighting.barons.DruidBaronBehavior;
import com.mythicrpg.fighting.barons.HotheadBaronBehavior;
import com.mythicrpg.fighting.barons.UndyingWolfBaronBehavior;
import com.mythicrpg.fighting.barons.InkBaronBehavior;
import com.mythicrpg.fighting.barons.InfernoBaronBehavior;
import com.mythicrpg.fighting.barons.MoltenBaronBehavior;
import com.mythicrpg.fighting.barons.NukeBaronBehavior;
import com.mythicrpg.fighting.barons.PanicBaronBehavior;
import com.mythicrpg.fighting.barons.SurvivorBaronBehavior;
import com.mythicrpg.fighting.barons.SwimmingBaronBehavior;
import com.mythicrpg.fighting.barons.ThrowerBaronBehavior;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.world.ServerWorld;

public final class BaronBehaviorManager {

    private static final int BEHAVIOR_INTERVAL_TICKS = 10;
    private static final double BARON_BEHAVIOR_RADIUS = 96.0;
    private static final double BARON_IDLE_PARTICLE_RADIUS = 64.0;

    private static int behaviorTickCounter = 0;

    private BaronBehaviorManager() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(BaronBehaviorManager::onAllowDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(BaronBehaviorManager::onAfterDeath);

        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            BalloonBaronBehavior.tryTrackFireball(entity, serverWorld);

            if (entity instanceof LivingEntity livingEntity && BaronMobManager.isBaron(livingEntity)) {
                BaronEntityQuery.track(livingEntity);
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof FireballEntity) {
                BalloonBaronBehavior.cleanup(entity);
            }

            BaronEntityQuery.untrack(entity);

            if (BaronMobManager.isBaron(entity)) {
                EntityCooldownManager.clearEntity(entity.getUuid());
                ChargingBaronBehavior.cleanup(entity);
                InfernoBaronBehavior.cleanup(entity);
                ThrowerBaronBehavior.cleanup(entity);

                if (entity instanceof LivingEntity livingEntity) {
                    UndyingWolfBaronBehavior.cleanup(livingEntity);
                }
            }
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            behaviorTickCounter = 0;
            BaronEntityQuery.clearAll();
            BalloonBaronBehavior.clearAll();
            ChargingBaronBehavior.clearAll();
            InfernoBaronBehavior.clearAll();
            ThrowerBaronBehavior.clearAll();
            UndyingWolfBaronBehavior.clearAll();
            BaronDeathMessageRegistry.clearAll();
            EntityCooldownManager.clearAll();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            behaviorTickCounter++;

            BalloonBaronBehavior.tickGlobal();

            if (behaviorTickCounter % BEHAVIOR_INTERVAL_TICKS != 0) {
                return;
            }

            for (ServerWorld world : server.getWorlds()) {
                tickBaronBehaviors(world);
            }
        });
    }

    private static boolean onAllowDamage(
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        Entity attacker = source.getAttacker();

        boolean targetIsBaron = BaronMobManager.isBaron(target);
        boolean attackerIsBaron = attacker instanceof LivingEntity livingAttacker
                && BaronMobManager.isBaron(livingAttacker);

        if (!targetIsBaron && !attackerIsBaron) {
            return true;
        }

        DruidBaronBehavior.handleHit(target, attacker);
        InkBaronBehavior.handleHit(target, source);

        if (attackerIsBaron && attacker instanceof LivingEntity livingAttacker) {
            if (!UndyingWolfBaronBehavior.allowOutgoingDamage(livingAttacker)) {
                return false;
            }
        }

        if (targetIsBaron) {
            PanicBaronBehavior.handleHit(target);

            if (!UndyingWolfBaronBehavior.allowDamage(target, source, amount)) {
                return false;
            }

            if (!SwimmingBaronBehavior.allowDamage(target, source)) {
                return false;
            }

            if (!MoltenBaronBehavior.allowDamage(target, source)) {
                return false;
            }

            return SurvivorBaronBehavior.allowDamage(target, source);
        }

        return true;
    }

    private static void onAfterDeath(
            LivingEntity entity,
            DamageSource damageSource
    ) {
        if (!BaronMobManager.isBaron(entity)) {
            return;
        }

        BaronEntityQuery.untrack(entity);
        EntityCooldownManager.clearEntity(entity.getUuid());
        ChargingBaronBehavior.cleanup(entity);
        InfernoBaronBehavior.cleanup(entity);
        ThrowerBaronBehavior.cleanup(entity);
        UndyingWolfBaronBehavior.cleanup(entity);

        BaronType type = BaronMobManager.getBaronType(entity);

        if (type == BaronType.NUKE && entity instanceof ZombieEntity) {
            NukeBaronBehavior.onDeath(entity);
            return;
        }

    }

    private static void tickBaronBehaviors(ServerWorld world) {
        BaronEntityQuery.forEachNearbyBaron(
                world,
                BARON_BEHAVIOR_RADIUS,
                BARON_IDLE_PARTICLE_RADIUS,
                BaronBehaviorManager::tickTrackedBaron
        );
    }

    private static void tickTrackedBaron(
            ServerWorld world,
            LivingEntity baron,
            BaronType type,
            boolean showIdleParticles
    ) {
        if (showIdleParticles) {
            BaronMobManager.spawnBaronParticles(world, baron, type);
        }

        tickSingleBaronBehavior(world, baron, type);
    }

    private static void tickSingleBaronBehavior(
            ServerWorld world,
            LivingEntity baron,
            BaronType type
    ) {
        switch (type) {
            case BARRAGE -> {
                if (baron instanceof SkeletonEntity skeleton) {
                    BarrageBaronBehavior.tick(world, skeleton);
                }
            }
            case DARKNIGHT -> {
                if (baron instanceof SpiderEntity spider) {
                    DarknightBaronBehavior.tick(world, spider);
                }
            }
            case ALCHEMIST -> {
                if (baron instanceof WitchEntity witch) {
                    AlchemistBaronBehavior.tick(world, witch);
                }
            }
            case HOTHEAD -> {
                if (baron instanceof BlazeEntity blaze) {
                    HotheadBaronBehavior.tick(world, blaze);
                }
            }
            case SWIMMING -> {
                if (baron instanceof EndermanEntity enderman) {
                    SwimmingBaronBehavior.tick(world, enderman);
                }
            }
            case DROWNED_KING -> {
                if (baron instanceof DrownedEntity drowned) {
                    DrownedKingBaronBehavior.tick(world, drowned);
                }
            }
            case CHARGING -> {
                if (baron instanceof RavagerEntity || baron instanceof HoglinEntity) {
                    ChargingBaronBehavior.tick(world, baron);
                }
            }
            case UNDYING_WOLF -> {
                if (baron instanceof WolfEntity wolf) {
                    UndyingWolfBaronBehavior.tick(world, wolf);
                }
            }
            case INFERNO -> {
                if (baron instanceof GuardianEntity guardian) {
                    InfernoBaronBehavior.tick(world, guardian);
                }
            }
            case THROWER -> {
                if (baron instanceof SpiderEntity spider) {
                    ThrowerBaronBehavior.tick(world, spider);
                }
            }
            default -> {
            }
        }
    }
}
