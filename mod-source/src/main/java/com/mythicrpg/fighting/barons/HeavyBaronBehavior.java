package com.mythicrpg.fighting.barons;

import com.mythicrpg.MythicRPG;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public final class HeavyBaronBehavior {

    private static final Identifier KNOCKBACK_RESISTANCE_MODIFIER_ID =
            Identifier.of(MythicRPG.MOD_ID, "heavy_baron_knockback_resistance");

    private HeavyBaronBehavior() {
    }

    public static void applyPromotion(CreeperEntity creeper, ServerWorld world) {
        EntityAttributeInstance knockbackResistance =
                creeper.getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);

        if (knockbackResistance != null
                && knockbackResistance.getModifier(KNOCKBACK_RESISTANCE_MODIFIER_ID) == null) {
            knockbackResistance.addPersistentModifier(new EntityAttributeModifier(
                    KNOCKBACK_RESISTANCE_MODIFIER_ID,
                    1.0,
                    EntityAttributeModifier.Operation.ADD_VALUE
            ));
        }

        world.spawnParticles(
                ParticleTypes.ASH,
                creeper.getX(),
                creeper.getBodyY(0.4),
                creeper.getZ(),
                20,
                0.35,
                0.2,
                0.35,
                0.02
        );

        world.playSound(
                null,
                creeper.getBlockPos(),
                SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.HOSTILE,
                0.35f,
                1.5f
        );
    }
}
