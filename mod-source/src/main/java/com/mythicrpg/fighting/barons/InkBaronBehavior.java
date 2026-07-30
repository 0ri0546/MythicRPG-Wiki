package com.mythicrpg.fighting.barons;

import com.mythicrpg.core.EntityCooldownManager;
import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class InkBaronBehavior {

    private static final String COOLDOWN_INK_RETALIATE = "baron_ink_retaliate";
    private static final int COOLDOWN_TICKS = 20;
    private static final int BLINDNESS_DURATION_TICKS = 40;

    private InkBaronBehavior() {
    }

    public static void handleHit(LivingEntity target, DamageSource source) {
        if (!(target instanceof SquidEntity squid)) {
            return;
        }

        if (BaronMobManager.getBaronType(squid) != BaronType.INK) {
            return;
        }

        Entity attacker = source.getAttacker();
        if (!(attacker instanceof ServerPlayerEntity player)) {
            return;
        }

        if (!(squid.getWorld() instanceof ServerWorld world)) {
            return;
        }

        if (!EntityCooldownManager.tryUse(squid, COOLDOWN_INK_RETALIATE, COOLDOWN_TICKS)) {
            return;
        }

        int blindnessDurationTicks = Math.max(1, (int) Math.round(
                BLINDNESS_DURATION_TICKS * BaronScaling.getInkBlindnessDurationMultiplier(squid)
        ));

        BaronDeathMessageRegistry.rememberBaronDanger(player, BaronType.INK);

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.BLINDNESS,
                blindnessDurationTicks,
                0,
                false,
                true
        ));

        Vec3d direction = player.getEyePos().subtract(squid.getPos());
        if (direction.lengthSquared() > 0.001) {
            direction = direction.normalize();
        } else {
            direction = Vec3d.ZERO;
        }

        for (int i = 0; i < 18; i++) {
            double progress = i / 17.0;
            world.spawnParticles(
                    ParticleTypes.SQUID_INK,
                    squid.getX() + direction.x * progress * 2.0,
                    squid.getBodyY(0.5) + direction.y * progress * 1.5,
                    squid.getZ() + direction.z * progress * 2.0,
                    2,
                    0.08,
                    0.08,
                    0.08,
                    0.02
            );
        }

        world.playSound(
                null,
                squid.getBlockPos(),
                SoundEvents.ENTITY_SQUID_SQUIRT,
                SoundCategory.HOSTILE,
                0.8F,
                0.8F
        );
    }
}
