package com.mythicrpg.fighting.barons;

import com.mythicrpg.MythicRPG;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class StalkerBaronBehavior {

    private static final Identifier STALKER_SCALE_MODIFIER_ID =
            Identifier.of(MythicRPG.MOD_ID, "stalker_baron_scale");

    private StalkerBaronBehavior() {
    }

    public static void applyPromotion(WitherSkeletonEntity witherSkeleton, ServerWorld world, double genericBaronScaleBonus) {
        EntityAttributeInstance scale = witherSkeleton.getAttributeInstance(EntityAttributes.GENERIC_SCALE);

        if (scale == null) {
            return;
        }

        if (scale.getModifier(STALKER_SCALE_MODIFIER_ID) != null) {
            return;
        }

        scale.addPersistentModifier(new EntityAttributeModifier(
                STALKER_SCALE_MODIFIER_ID,
                BaronScaling.getStalkerScaleModifier(witherSkeleton, genericBaronScaleBonus),
                EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE
        ));

        witherSkeleton.calculateDimensions();
    }
}
