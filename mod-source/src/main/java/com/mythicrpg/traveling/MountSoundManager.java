package com.mythicrpg.traveling;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Set;

/** Filters only sounds that communicate an attack or hostile state. */
public final class MountSoundManager {
    private static final Set<SoundEvent> BLOCKED_SOUNDS = Set.of(
            SoundEvents.ENTITY_BEE_LOOP_AGGRESSIVE,
            SoundEvents.ENTITY_BEE_STING,
            SoundEvents.ENTITY_BLAZE_SHOOT,
            SoundEvents.ENTITY_BREEZE_CHARGE,
            SoundEvents.ENTITY_BREEZE_INHALE,
            SoundEvents.ENTITY_BREEZE_SHOOT,
            SoundEvents.ENTITY_ENDERMAN_SCREAM,
            SoundEvents.ENTITY_ENDERMAN_STARE,
            SoundEvents.ENTITY_ENDERMAN_TELEPORT,
            SoundEvents.ENTITY_GHAST_SCREAM,
            SoundEvents.ENTITY_GHAST_SHOOT,
            SoundEvents.ENTITY_GHAST_WARN,
            SoundEvents.ENTITY_GOAT_PREPARE_RAM,
            SoundEvents.ENTITY_GOAT_RAM_IMPACT,
            SoundEvents.ENTITY_GOAT_SCREAMING_PREPARE_RAM,
            SoundEvents.ENTITY_GOAT_SCREAMING_RAM_IMPACT,
            SoundEvents.ENTITY_HOGLIN_ANGRY,
            SoundEvents.ENTITY_HOGLIN_ATTACK,
            SoundEvents.ENTITY_PHANTOM_BITE,
            SoundEvents.ENTITY_PHANTOM_SWOOP,
            SoundEvents.ENTITY_POLAR_BEAR_WARNING,
            SoundEvents.ENTITY_RAVAGER_ATTACK,
            SoundEvents.ENTITY_RAVAGER_CELEBRATE,
            SoundEvents.ENTITY_RAVAGER_ROAR,
            SoundEvents.ENTITY_RAVAGER_STUNNED,
            SoundEvents.ENTITY_SNOW_GOLEM_SHOOT
    );

    private MountSoundManager() {
    }

    public static boolean shouldBlock(Entity entity, SoundEvent sound) {
        return entity instanceof MobEntity mob
                && LandMountManager.isAdoptedMount(mob)
                && BLOCKED_SOUNDS.contains(sound);
    }
}
