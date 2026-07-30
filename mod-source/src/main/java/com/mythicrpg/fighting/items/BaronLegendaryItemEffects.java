package com.mythicrpg.fighting.items;

import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.PlayerCooldownManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class BaronLegendaryItemEffects {
    public static final String BARONS_DOLL_TAG = "mythicrpg_barons_doll_clone";

    private static final String WITHER_SHIELD_PROC_COOLDOWN = "wither_shield_proc";
    private static final int WITHER_SHIELD_PROC_COOLDOWN_TICKS = 20 * 2;
    private static final int WITHER_DURATION_TICKS = 20 * 3;

    private static final int BEAM_RANGE_SQUARED = 12 * 12;
    private static final int BEAM_DURATION_TICKS = 20 * 3;
    private static final int BEAM_VISUAL_INTERVAL_TICKS = 4;
    private static final int BEAM_SLOWNESS_REFRESH_INTERVAL_TICKS = 5;
    private static final int BEAM_SLOWNESS_DURATION_TICKS = 12;
    private static final int BEAM_SLOWNESS_AMPLIFIER = 1;
    private static final float[] BEAM_DAMAGE_BY_STAGE = {5.0f, 7.5f, 10.0f};
    private static final DustParticleEffect BEAM_TARGET_PARTICLE =
            new DustParticleEffect(new Vector3f(1.0f, 0.0f, 0.0f), 1.25f);
    private static final int DOLL_MAX_AGE_TICKS = 20 * 60;
    private static final double DOLL_RETARGET_RADIUS = 32.0;

    private static final List<BeamTask> BEAMS = new ArrayList<>();
    private static final List<DollTask> DOLLS = new ArrayList<>();

    private BaronLegendaryItemEffects() {
    }

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> {
            if (!blocked) {
                return;
            }

            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }

            if (!(source.getAttacker() instanceof LivingEntity attacker)) {
                return;
            }

            if (source.getSource() != attacker) {
                return;
            }

            if (!isBlockingWitherShield(player)) {
                return;
            }

            if (!PlayerCooldownManager.tryUse(player, WITHER_SHIELD_PROC_COOLDOWN, WITHER_SHIELD_PROC_COOLDOWN_TICKS)) {
                return;
            }

            attacker.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, WITHER_DURATION_TICKS, 0));

            if (player.getWorld() instanceof ServerWorld world) {
                world.spawnParticles(ParticleTypes.SOUL, attacker.getX(), attacker.getBodyY(0.6), attacker.getZ(), 12, 0.25, 0.35, 0.25, 0.02);
                world.playSound(null, attacker.getBlockPos(), SoundEvents.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 0.35f, 1.7f);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(BaronLegendaryItemEffects::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            BEAMS.clear();
            DOLLS.clear();
        });
    }

    public static void startBeam(ServerPlayerEntity player, LivingEntity target) {
        UUID playerUuid = player.getUuid();
        BEAMS.removeIf(task -> task.playerUuid.equals(playerUuid));
        BEAMS.add(new BeamTask(playerUuid, target.getUuid(), player.getWorld().getRegistryKey(), player.getWorld().getTime(), 1, player.getWorld().getTime()));
    }

    public static void spawnBeamTargetMarker(ServerWorld world, LivingEntity target, boolean burst) {
        double x = target.getX();
        double y = target.getBodyY(0.75);
        double z = target.getZ();

        double radius = Math.max(0.35, target.getWidth() * 0.65);
        int circlePoints = burst ? 18 : 10;

        for (int i = 0; i < circlePoints; i++) {
            double angle = (Math.PI * 2.0 * i) / circlePoints;
            double px = x + Math.cos(angle) * radius;
            double pz = z + Math.sin(angle) * radius;

            world.spawnParticles(BEAM_TARGET_PARTICLE, px, y, pz, 1, 0.0, 0.0, 0.0, 0.0);
        }

        double cross = radius * 0.85;

        world.spawnParticles(BEAM_TARGET_PARTICLE, x - cross, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(BEAM_TARGET_PARTICLE, x + cross, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(BEAM_TARGET_PARTICLE, x, y + cross, z, 1, 0.0, 0.0, 0.0, 0.0);
        world.spawnParticles(BEAM_TARGET_PARTICLE, x, y - cross, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    public static void trackDoll(ServerPlayerEntity player, ArmorStandEntity doll) {
        removeExistingDoll(player.getUuid(), player.getServer());
        DOLLS.add(new DollTask(player.getUuid(), doll.getUuid(), player.getWorld().getRegistryKey(), player.getWorld().getTime()));
        retargetAttackers(player, doll);
    }

    private static void tick(MinecraftServer server) {
        if (!BEAMS.isEmpty()) {
            tickBeams(server);
        }

        if (!DOLLS.isEmpty()) {
            tickDolls(server);
        }
    }

    private static void tickBeams(MinecraftServer server) {
        Iterator<BeamTask> iterator = BEAMS.iterator();

        while (iterator.hasNext()) {
            BeamTask task = iterator.next();
            ServerWorld world = server.getWorld(task.worldKey);

            if (world == null) {
                iterator.remove();
                continue;
            }

            long elapsed = world.getTime() - task.startTick;

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(task.playerUuid);
            Entity rawTarget = world.getEntity(task.targetUuid);

            if (!(rawTarget instanceof LivingEntity target) || player == null || !target.isAlive()) {
                iterator.remove();
                continue;
            }

            if (elapsed > BEAM_DURATION_TICKS || player.squaredDistanceTo(target) > BEAM_RANGE_SQUARED || !player.canSee(target)) {
                iterator.remove();
                continue;
            }

            if (world.getTime() - task.lastVisualTick >= BEAM_VISUAL_INTERVAL_TICKS) {
                spawnBeamTargetMarker(world, target, false);
                task.lastVisualTick = world.getTime();
            }

            if (target instanceof MobEntity
                    && elapsed % BEAM_SLOWNESS_REFRESH_INTERVAL_TICKS == 0L) {
                target.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS,
                        BEAM_SLOWNESS_DURATION_TICKS,
                        BEAM_SLOWNESS_AMPLIFIER
                ));
            }

            if (elapsed < task.nextStage * 20L) {
                continue;
            }

            int stageIndex = Math.max(0, Math.min(task.nextStage - 1, BEAM_DAMAGE_BY_STAGE.length - 1));
            target.damage(world.getDamageSources().magic(), BEAM_DAMAGE_BY_STAGE[stageIndex]);
            world.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_GUARDIAN_ATTACK, SoundCategory.PLAYERS, 0.25f, 1.2f + task.nextStage * 0.2f);
            task.nextStage++;

            if (task.nextStage > BEAM_DAMAGE_BY_STAGE.length) {
                iterator.remove();
            }
        }
    }

    private static void tickDolls(MinecraftServer server) {
        Iterator<DollTask> iterator = DOLLS.iterator();

        while (iterator.hasNext()) {
            DollTask task = iterator.next();
            ServerWorld world = server.getWorld(task.worldKey);

            if (world == null) {
                iterator.remove();
                continue;
            }

            Entity rawDoll = world.getEntity(task.dollUuid);

            if (!(rawDoll instanceof ArmorStandEntity doll) || !doll.isAlive()) {
                iterator.remove();
                continue;
            }

            if (world.getTime() - task.spawnTick >= DOLL_MAX_AGE_TICKS) {
                doll.discard();
                iterator.remove();
                continue;
            }

            ServerPlayerEntity player = server.getPlayerManager().getPlayer(task.playerUuid);

            long age = world.getTime() - task.spawnTick;

            if (player != null && age % 20L == 0L) {
                retargetAttackers(player, doll);
            }

            if (age % 30L == 0L) {
                double angle = (world.getTime() % 360) * 0.017453292519943295;
                doll.addVelocity(Math.cos(angle) * 0.02, 0.0, Math.sin(angle) * 0.02);
                doll.velocityModified = true;
                world.spawnParticles(ParticleTypes.POOF, doll.getX(), doll.getBodyY(0.5), doll.getZ(), 2, 0.15, 0.2, 0.15, 0.0);
            }
        }
    }

    private static void removeExistingDoll(UUID playerUuid, MinecraftServer server) {
        Iterator<DollTask> iterator = DOLLS.iterator();

        while (iterator.hasNext()) {
            DollTask task = iterator.next();

            if (!task.playerUuid.equals(playerUuid)) {
                continue;
            }

            ServerWorld world = server.getWorld(task.worldKey);

            if (world != null && world.getEntity(task.dollUuid) instanceof Entity doll) {
                doll.discard();
            }

            iterator.remove();
        }
    }

    private static void retargetAttackers(ServerPlayerEntity player, LivingEntity doll) {
        if (!(player.getWorld() instanceof ServerWorld world)) {
            return;
        }

        Box box = player.getBoundingBox().expand(DOLL_RETARGET_RADIUS);
        List<MobEntity> mobs = world.getEntitiesByClass(MobEntity.class, box, mob -> mob.getTarget() == player);

        for (MobEntity mob : mobs) {
            mob.setTarget(doll);
        }
    }

    private static boolean isBlockingWitherShield(PlayerEntity player) {
        return player.isUsingItem() && player.getActiveItem().isOf(ModItems.WITHER_SHIELD);
    }

    private static final class BeamTask {
        private final UUID playerUuid;
        private final UUID targetUuid;
        private final net.minecraft.registry.RegistryKey<World> worldKey;
        private final long startTick;
        private int nextStage;
        private long lastVisualTick;

        private BeamTask(UUID playerUuid, UUID targetUuid, net.minecraft.registry.RegistryKey<World> worldKey, long startTick, int nextStage, long lastVisualTick) {
            this.playerUuid = playerUuid;
            this.targetUuid = targetUuid;
            this.worldKey = worldKey;
            this.startTick = startTick;
            this.nextStage = nextStage;
            this.lastVisualTick = lastVisualTick;
        }
    }

    private record DollTask(UUID playerUuid, UUID dollUuid, net.minecraft.registry.RegistryKey<World> worldKey, long spawnTick) {
    }
}
