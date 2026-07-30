package com.mythicrpg.fighting.barons;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public final class DiamondBaronBehavior {

    private DiamondBaronBehavior() {
    }

    public static void applyPromotion(VexEntity vex, ServerWorld world) {
        vex.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        vex.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);

        world.spawnParticles(
                ParticleTypes.ENCHANT,
                vex.getX(),
                vex.getBodyY(0.6),
                vex.getZ(),
                18,
                0.25,
                0.25,
                0.25,
                0.05
        );

        world.playSound(
                null,
                vex.getBlockPos(),
                SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND.value(),
                SoundCategory.HOSTILE,
                0.7f,
                1.4f
        );
    }
}
