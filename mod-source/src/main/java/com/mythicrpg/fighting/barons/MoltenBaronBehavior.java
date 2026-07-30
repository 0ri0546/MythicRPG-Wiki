package com.mythicrpg.fighting.barons;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

public final class MoltenBaronBehavior {

    private MoltenBaronBehavior() {
    }

    public static boolean allowDamage(LivingEntity target, DamageSource source) {
        if (!(target instanceof IronGolemEntity)) {
            return true;
        }

        if (BaronMobManager.getBaronType(target) != BaronType.MOLTEN) {
            return true;
        }

        if (!isPreventedDamage(source)) {
            return true;
        }

        target.extinguish();

        if (target.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(
                    ParticleTypes.FLAME,
                    target.getX(),
                    target.getBodyY(0.5),
                    target.getZ(),
                    5,
                    0.25,
                    0.35,
                    0.25,
                    0.02
            );
        }

        return false;
    }

    private static boolean isPreventedDamage(DamageSource source) {
        return source.isOf(DamageTypes.IN_FIRE)
                || source.isOf(DamageTypes.ON_FIRE)
                || source.isOf(DamageTypes.LAVA)
                || source.isOf(DamageTypes.HOT_FLOOR)
                || source.isOf(DamageTypes.IN_WALL)
                || source.isOf(DamageTypes.CRAMMING)
                || source.isOf(DamageTypes.DROWN);
    }
}
