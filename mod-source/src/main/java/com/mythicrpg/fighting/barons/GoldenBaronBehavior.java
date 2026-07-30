package com.mythicrpg.fighting.barons;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class GoldenBaronBehavior {

    private GoldenBaronBehavior() {
    }

    public static void onDeath(LivingEntity entity) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return;
        }

        ItemStack reward = world.random.nextBoolean()
                ? new ItemStack(Items.GOLDEN_APPLE)
                : new ItemStack(Items.EMERALD);

        entity.dropStack(reward);

        world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                entity.getX(),
                entity.getBodyY(0.6),
                entity.getZ(),
                20,
                0.5,
                0.5,
                0.5,
                0.05
        );

        world.playSound(
                null,
                entity.getBlockPos(),
                SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.NEUTRAL,
                0.5f,
                1.5f
        );
    }
}
