package com.mythicrpg.fighting.barons;

import com.mythicrpg.core.EntityCooldownManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.WitchEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class AlchemistBaronBehavior {

    private static final double CAST_RANGE = 16.0;
    private static final int CAST_COOLDOWN_TICKS = 45;
    private static final String COOLDOWN_ALCHEMIST_CAST = "baron_alchemist_cast";

    private AlchemistBaronBehavior() {
    }

    public static void tick(ServerWorld world, WitchEntity witch) {
        if (!(witch.getTarget() instanceof ServerPlayerEntity target)) {
            return;
        }

        if (!BaronEntityQuery.isValidPlayerTarget(target)) {
            return;
        }

        if (witch.squaredDistanceTo(target) > CAST_RANGE * CAST_RANGE) {
            return;
        }

        if (!witch.canSee(target)) {
            return;
        }

        int cooldownTicks = BaronScaling.getAlchemistCooldownTicks(witch, CAST_COOLDOWN_TICKS);

        if (!EntityCooldownManager.tryUse(witch, COOLDOWN_ALCHEMIST_CAST, cooldownTicks)) {
            return;
        }

        RegistryEntry<StatusEffect> effect = getRandomEffect(world);
        int duration = getEffectDuration(effect);
        int amplifier = getEffectAmplifier(effect);

        BaronDeathMessageRegistry.rememberBaronDanger(target, BaronType.ALCHEMIST);
        target.addStatusEffect(new StatusEffectInstance(effect, duration, amplifier));

        world.spawnParticles(
                ParticleTypes.WITCH,
                witch.getX(),
                witch.getBodyY(0.8),
                witch.getZ(),
                18,
                0.35,
                0.35,
                0.35,
                0.08
        );

        world.spawnParticles(
                ParticleTypes.WITCH,
                target.getX(),
                target.getBodyY(0.6),
                target.getZ(),
                12,
                0.25,
                0.35,
                0.25,
                0.05
        );

        world.playSound(
                null,
                witch.getBlockPos(),
                SoundEvents.ENTITY_WITCH_THROW,
                SoundCategory.HOSTILE,
                0.9f,
                1.25f
        );
    }

    private static RegistryEntry<StatusEffect> getRandomEffect(ServerWorld world) {
        int roll = world.random.nextInt(5);

        return switch (roll) {
            case 0 -> StatusEffects.POISON;
            case 1 -> StatusEffects.SLOWNESS;
            case 2 -> StatusEffects.WEAKNESS;
            case 3 -> StatusEffects.BLINDNESS;
            default -> StatusEffects.MINING_FATIGUE;
        };
    }

    private static int getEffectDuration(RegistryEntry<StatusEffect> effect) {
        if (effect == StatusEffects.BLINDNESS) {
            return 60;
        }

        if (effect == StatusEffects.POISON) {
            return 80;
        }

        return 100;
    }

    private static int getEffectAmplifier(RegistryEntry<StatusEffect> effect) {
        if (effect == StatusEffects.SLOWNESS) {
            return 1;
        }

        return 0;
    }
}
