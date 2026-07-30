package com.mythicrpg.fighting.barons;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public final class NukeBaronBehavior {

    private static final List<RegistryEntry<StatusEffect>> EFFECTS = List.of(
            StatusEffects.POISON,
            StatusEffects.SLOWNESS,
            StatusEffects.WEAKNESS,
            StatusEffects.BLINDNESS,
            StatusEffects.WITHER,
            StatusEffects.MINING_FATIGUE
    );

    private NukeBaronBehavior() {
    }

    public static void onDeath(LivingEntity entity) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }

        RegistryEntry<StatusEffect> effect = EFFECTS.get(world.random.nextInt(EFFECTS.size()));

        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(
                world,
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );

        float radius = (float) (3.0f * BaronScaling.getNukeCloudRadiusMultiplier(entity));
        int duration = Math.max(1, (int) Math.round(100 * BaronScaling.getNukeCloudDurationMultiplier(entity)));

        cloud.setOwner(entity);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusGrowth(-0.02f);
        cloud.addEffect(new StatusEffectInstance(effect, 120, 0));

        world.spawnEntity(cloud);

        world.spawnParticles(
                ParticleTypes.WITCH,
                entity.getX(),
                entity.getBodyY(0.5),
                entity.getZ(),
                25,
                0.7,
                0.4,
                0.7,
                0.04
        );

        world.playSound(
                null,
                entity.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.HOSTILE,
                0.45f,
                1.4f
        );
    }
}
