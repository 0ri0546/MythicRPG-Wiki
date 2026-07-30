package com.mythicrpg.woodcutting;

import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import com.mythicrpg.mixin.PersistentProjectileEntityAccessor;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class EnchantedAxeProjectileManager {

    private static final String SPLIT_PROJECTILE_TAG = "mythicrpg_split_projectile";

    private static final int SPLIT_DELAY_TICKS = 8;
    private static final int SPLIT_ARROW_MIN_AGE_TICKS = 20 * 2;
    private static final double SPLIT_ARROW_STUCK_VELOCITY_SQUARED = 0.003;
    private static final List<PendingSplit> PENDING_SPLITS = new ArrayList<>();
    private static final List<TrackedSplitArrow> TRACKED_SPLIT_ARROWS = new ArrayList<>();

    private EnchantedAxeProjectileManager() {
    }

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }

            onEntityLoad(entity, serverWorld);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickPendingSplits();
            cleanupSplitArrows(server);
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(EnchantedAxeProjectileManager::onAllowDamage);
    }

    private static void onEntityLoad(Entity entity, ServerWorld world) {
        if (!(entity instanceof ProjectileEntity projectile)) {
            return;
        }

        if (!isSupportedProjectile(projectile)) {
            return;
        }

        if (projectile.getCommandTags().contains(SPLIT_PROJECTILE_TAG)) {
            return;
        }

        if (!(projectile.getOwner() instanceof ServerPlayerEntity player)) {
            return;
        }

        if (!player.getOffHandStack().isOf(ModItems.ENCHANTED_AXE)) {
            return;
        }

        markAsSplitProjectile(projectile);
        damageOffhandAxe(player);

        PENDING_SPLITS.add(new PendingSplit(
                projectile,
                player,
                projectile.getVelocity().x,
                projectile.getVelocity().y,
                projectile.getVelocity().z,
                projectile.getYaw(),
                projectile.getPitch(),
                SPLIT_DELAY_TICKS
        ));

        world.spawnParticles(
                ParticleTypes.CRIT,
                projectile.getX(),
                projectile.getY(),
                projectile.getZ(),
                8,
                0.08,
                0.08,
                0.08,
                0.02
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_WOOD_HIT,
                SoundCategory.PLAYERS,
                0.35f,
                1.6f
        );
    }

    private static boolean isSupportedProjectile(ProjectileEntity projectile) {
        return projectile instanceof ArrowEntity
                || projectile instanceof SnowballEntity
                || projectile instanceof EggEntity;
    }

    private static void tickPendingSplits() {
        Iterator<PendingSplit> iterator = PENDING_SPLITS.iterator();

        while (iterator.hasNext()) {
            PendingSplit split = iterator.next();

            split.ticksLeft--;

            if (split.ticksLeft > 0) {
                continue;
            }

            iterator.remove();

            if (!split.player.isAlive()) {
                continue;
            }

            if (!(split.player.getWorld() instanceof ServerWorld world)) {
                continue;
            }

            duplicateProjectileFromPlayer(world, split);
        }
    }

    private static void duplicateProjectileFromPlayer(ServerWorld world, PendingSplit split) {
        Entity duplicateEntity = split.original.getType().create(world);

        if (!(duplicateEntity instanceof ProjectileEntity duplicateProjectile)) {
            return;
        }

        ServerPlayerEntity player = split.player;

        double forwardX = -Math.sin(Math.toRadians(player.getYaw())) * 0.6;
        double forwardZ = Math.cos(Math.toRadians(player.getYaw())) * 0.6;

        double spawnX = player.getX() + forwardX;
        double spawnY = player.getEyeY() - 0.1;
        double spawnZ = player.getZ() + forwardZ;

        markAsSplitProjectile(duplicateEntity);

        duplicateEntity.refreshPositionAndAngles(
                spawnX,
                spawnY,
                spawnZ,
                split.yaw,
                split.pitch
        );

        duplicateEntity.setVelocity(
                split.velocityX,
                split.velocityY,
                split.velocityZ
        );

        duplicateEntity.setNoGravity(split.original.hasNoGravity());

        duplicateProjectile.setOwner(player);

        world.spawnEntity(duplicateEntity);

        world.spawnParticles(
                ParticleTypes.ENCHANTED_HIT,
                spawnX,
                spawnY,
                spawnZ,
                12,
                0.12,
                0.12,
                0.12,
                0.03
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS,
                0.25f,
                1.8f
        );
    }

    private static void markAsSplitProjectile(Entity entity) {
        entity.addCommandTag(SPLIT_PROJECTILE_TAG);

        if (entity instanceof PersistentProjectileEntity projectile) {
            projectile.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;

            if (projectile.getWorld() instanceof ServerWorld world) {
                trackSplitArrow(world, projectile.getUuid());
            }
        }
    }

    private static void trackSplitArrow(ServerWorld world, UUID projectileUuid) {
        RegistryKey<World> worldKey = world.getRegistryKey();

        for (TrackedSplitArrow tracked : TRACKED_SPLIT_ARROWS) {
            if (tracked.worldKey.equals(worldKey) && tracked.projectileUuid.equals(projectileUuid)) {
                return;
            }
        }

        TRACKED_SPLIT_ARROWS.add(new TrackedSplitArrow(worldKey, projectileUuid));
    }

    private static void cleanupSplitArrows(MinecraftServer server) {
        Iterator<TrackedSplitArrow> iterator = TRACKED_SPLIT_ARROWS.iterator();

        while (iterator.hasNext()) {
            TrackedSplitArrow tracked = iterator.next();
            ServerWorld world = server.getWorld(tracked.worldKey);

            if (world == null) {
                iterator.remove();
                continue;
            }

            Entity entity = world.getEntity(tracked.projectileUuid);

            if (!(entity instanceof PersistentProjectileEntity projectile) || entity.isRemoved()) {
                iterator.remove();
                continue;
            }

            projectile.pickupType = PersistentProjectileEntity.PickupPermission.CREATIVE_ONLY;

            if (projectile.age < SPLIT_ARROW_MIN_AGE_TICKS) {
                continue;
            }

            boolean inGround = ((PersistentProjectileEntityAccessor) projectile).mythicrpg$isInGround();
            boolean almostStopped = projectile.getVelocity().lengthSquared() <= SPLIT_ARROW_STUCK_VELOCITY_SQUARED;

            if (!inGround && !almostStopped) {
                continue;
            }

            projectile.discard();
            iterator.remove();
        }
    }

    private static void damageOffhandAxe(ServerPlayerEntity player) {
        if (player.isCreative()) {
            return;
        }

        ItemStack axe = player.getOffHandStack();

        if (!axe.isOf(ModItems.ENCHANTED_AXE)) {
            return;
        }

        if (!axe.isDamageable()) {
            return;
        }

        axe.damage(
                30,
                player.getServerWorld(),
                player,
                item -> player.sendEquipmentBreakStatus(item, EquipmentSlot.OFFHAND)
        );

        player.currentScreenHandler.sendContentUpdates();
    }

    private static final class TrackedSplitArrow {
        private final RegistryKey<World> worldKey;
        private final UUID projectileUuid;

        private TrackedSplitArrow(RegistryKey<World> worldKey, UUID projectileUuid) {
            this.worldKey = worldKey;
            this.projectileUuid = projectileUuid;
        }
    }

    private static final class PendingSplit {
        private final Entity original;
        private final ServerPlayerEntity player;
        private final double velocityX;
        private final double velocityY;
        private final double velocityZ;
        private final float yaw;
        private final float pitch;
        private int ticksLeft;

        private PendingSplit(
                Entity original,
                ServerPlayerEntity player,
                double velocityX,
                double velocityY,
                double velocityZ,
                float yaw,
                float pitch,
                int ticksLeft
        ) {
            this.original = original;
            this.player = player;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.ticksLeft = ticksLeft;
        }
    }

    private static boolean onAllowDamage(
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        Entity sourceEntity = source.getSource();

        if (sourceEntity == null) {
            return true;
        }

        if (!sourceEntity.getCommandTags().contains(SPLIT_PROJECTILE_TAG)) {
            return true;
        }

        target.timeUntilRegen = 0;

        return true;
    }
}