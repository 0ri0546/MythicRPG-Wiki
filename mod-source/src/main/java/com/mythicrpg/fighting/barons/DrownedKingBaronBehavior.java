package com.mythicrpg.fighting.barons;

import com.mythicrpg.core.EntityCooldownManager;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class DrownedKingBaronBehavior {

    private static final double CHARGE_RANGE = 24.0;
    private static final double MIN_CHARGE_DISTANCE = 4.0;
    private static final double CHARGE_SPEED = 1.15;
    private static final int CHARGE_COOLDOWN_TICKS = 70;

    private static final double HIT_RANGE = 2.2;
    private static final int HIT_COOLDOWN_TICKS = 20;
    private static final int CHARGE_ACTIVE_TICKS = 25;
    private static final float CHARGE_DAMAGE = 6.0f;
    private static final float SPIN_DEGREES_PER_TICK = 35.0f;
    private static final double AURA_RADIUS = 0.9;
    private static final int AURA_POINTS = 6;

    private static final String COOLDOWN_DROWNED_KING_CHARGE = "baron_drowned_king_charge";
    private static final String COOLDOWN_DROWNED_KING_HIT = "baron_drowned_king_hit";

    private DrownedKingBaronBehavior() {
    }

    public static void applyPromotion(DrownedEntity drowned, ServerWorld world) {
        ItemStack trident = new ItemStack(Items.TRIDENT);

        world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.RIPTIDE)
                .ifPresent(entry -> trident.addEnchantment(entry, 3));

        drowned.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        drowned.equipStack(EquipmentSlot.OFFHAND, trident);

        drowned.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        drowned.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0f);

        world.spawnParticles(
                ParticleTypes.SPLASH,
                drowned.getX(),
                drowned.getBodyY(0.6),
                drowned.getZ(),
                30,
                0.5,
                0.4,
                0.5,
                0.08
        );

        world.playSound(
                null,
                drowned.getBlockPos(),
                SoundEvents.ITEM_TRIDENT_RETURN,
                SoundCategory.HOSTILE,
                0.8f,
                1.4f
        );
    }

    public static void tick(ServerWorld world, DrownedEntity drowned) {
        tryCharge(drowned, world);
        tryImpact(drowned, world);
        tickDashVisuals(drowned, world);
    }

    private static void tryCharge(DrownedEntity drowned, ServerWorld world) {
        if (!(drowned.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }

        if (!BaronEntityQuery.isValidPlayerTarget(target)) {
            return;
        }

        if (!drowned.isTouchingWater()) {
            return;
        }

        double distanceSquared = drowned.squaredDistanceTo(target);

        if (distanceSquared > CHARGE_RANGE * CHARGE_RANGE) {
            return;
        }

        if (distanceSquared < MIN_CHARGE_DISTANCE * MIN_CHARGE_DISTANCE) {
            return;
        }

        if (!EntityCooldownManager.tryUse(drowned, COOLDOWN_DROWNED_KING_CHARGE, CHARGE_COOLDOWN_TICKS)) {
            return;
        }

        Vec3d start = drowned.getPos().add(0.0, drowned.getHeight() * 0.5, 0.0);
        Vec3d end = target.getEyePos();
        Vec3d direction = end.subtract(start);

        if (direction.lengthSquared() <= 0.001) {
            return;
        }

        direction = direction.normalize();

        double chargeMultiplier = BaronScaling.getDrownedKingChargeMultiplier(drowned);
        double chargeSpeed = CHARGE_SPEED * chargeMultiplier;

        drowned.setVelocity(new Vec3d(
                direction.x * chargeSpeed,
                Math.max(direction.y * chargeSpeed, 0.12 * chargeMultiplier),
                direction.z * chargeSpeed
        ));
        drowned.velocityModified = true;

        world.spawnParticles(
                ParticleTypes.SPLASH,
                drowned.getX(),
                drowned.getBodyY(0.5),
                drowned.getZ(),
                30,
                0.45,
                0.35,
                0.45,
                0.08
        );

        world.spawnParticles(
                ParticleTypes.BUBBLE,
                drowned.getX(),
                drowned.getBodyY(0.5),
                drowned.getZ(),
                20,
                0.35,
                0.35,
                0.35,
                0.06
        );

        world.playSound(
                null,
                drowned.getBlockPos(),
                SoundEvents.ITEM_TRIDENT_RETURN,
                SoundCategory.HOSTILE,
                1.0f,
                0.75f
        );
    }

    private static void tryImpact(DrownedEntity drowned, ServerWorld world) {
        if (!(drowned.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }

        if (!BaronEntityQuery.isValidPlayerTarget(target)) {
            return;
        }

        long now = world.getTime();
        long lastCharge = EntityCooldownManager.getLastTick(drowned, COOLDOWN_DROWNED_KING_CHARGE);

        if (now - lastCharge > CHARGE_ACTIVE_TICKS) {
            return;
        }

        if (drowned.squaredDistanceTo(target) > HIT_RANGE * HIT_RANGE) {
            return;
        }

        if (!EntityCooldownManager.tryUse(drowned, COOLDOWN_DROWNED_KING_HIT, HIT_COOLDOWN_TICKS)) {
            return;
        }

        target.damage(
                world.getDamageSources().mobAttack(drowned),
                (float) (CHARGE_DAMAGE * BaronScaling.getDrownedKingChargeMultiplier(drowned))
        );

        Vec3d knockbackDirection = target.getPos().subtract(drowned.getPos());

        if (knockbackDirection.lengthSquared() > 0.001) {
            knockbackDirection = knockbackDirection.normalize();

            target.addVelocity(
                    knockbackDirection.x * 0.65,
                    0.25,
                    knockbackDirection.z * 0.65
            );
            target.velocityModified = true;
        }

        world.spawnParticles(
                ParticleTypes.SPLASH,
                target.getX(),
                target.getBodyY(0.5),
                target.getZ(),
                35,
                0.45,
                0.35,
                0.45,
                0.08
        );

        world.playSound(
                null,
                target.getBlockPos(),
                SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED,
                SoundCategory.HOSTILE,
                0.9f,
                1.0f
        );
    }

    private static void tickDashVisuals(DrownedEntity drowned, ServerWorld world) {
        long now = world.getTime();
        long lastCharge = EntityCooldownManager.getLastTick(drowned, COOLDOWN_DROWNED_KING_CHARGE);
        long elapsed = now - lastCharge;

        if (elapsed < 0 || elapsed > CHARGE_ACTIVE_TICKS) {
            return;
        }

        float newYaw = drowned.getYaw() + SPIN_DEGREES_PER_TICK;
        drowned.setYaw(newYaw);
        spawnAura(world, drowned, elapsed);
    }

    private static void spawnAura(ServerWorld world, DrownedEntity drowned, long elapsed) {
        double centerX = drowned.getX();
        double centerY = drowned.getBodyY(0.5);
        double centerZ = drowned.getZ();
        double baseAngle = elapsed * 0.65;

        for (int i = 0; i < AURA_POINTS; i++) {
            double step = (Math.PI * 2.0 / AURA_POINTS) * i;

            double angle1 = baseAngle + step;
            double x1 = centerX + Math.cos(angle1) * AURA_RADIUS;
            double z1 = centerZ + Math.sin(angle1) * AURA_RADIUS;
            double y1 = centerY + Math.sin(baseAngle + step) * 0.25;

            world.spawnParticles(ParticleTypes.NAUTILUS, x1, y1, z1, 1, 0.0, 0.0, 0.0, 0.0);
            world.spawnParticles(ParticleTypes.GLOW, x1, y1, z1, 1, 0.0, 0.0, 0.0, 0.0);

            double angle2 = -baseAngle + step;
            double x2 = centerX + Math.cos(angle2) * (AURA_RADIUS * 0.7);
            double z2 = centerZ + Math.sin(angle2) * (AURA_RADIUS * 0.7);
            double y2 = centerY + Math.cos(baseAngle + step) * 0.2;

            world.spawnParticles(ParticleTypes.SPLASH, x2, y2, z2, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
