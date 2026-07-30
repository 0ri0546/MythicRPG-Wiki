package com.mythicrpg.mixin.client;

import com.mythicrpg.fighting.BaronMobManager;
import com.mythicrpg.fighting.BaronType;
import net.minecraft.client.render.entity.DrownedEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.DrownedEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DrownedEntityRenderer.class)
public abstract class DrownedEntityRendererMixin {

    @Inject(
            method = "setupTransforms(Lnet/minecraft/entity/mob/DrownedEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
            at = @At("TAIL")
    )
    private void mythicrpg$applyDrownedKingDashRotation(
            DrownedEntity drowned,
            MatrixStack matrices,
            float animationProgress,
            float bodyYaw,
            float tickDelta,
            float scale,
            CallbackInfo ci
    ) {
        if (!isDrownedKing(drowned)) {
            return;
        }

        if (!isDrownedKingDashing(drowned)) {
            return;
        }

        float roll = (drowned.age + tickDelta) * 90.0f;

        // Couche le drowned sur le ventre.
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));

        // Rotation autour de l'axe pieds -> tête.
        // 90 degrés par tick = 5 tours par seconde.
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
    }

    private boolean isDrownedKing(DrownedEntity drowned) {
        if (BaronMobManager.isBaron(drowned)
                && BaronMobManager.getBaronType(drowned) == BaronType.DROWNED_KING) {
            return true;
        }

        // Fallback si les command tags ne sont pas visibles côté client.
        return drowned.hasCustomName()
                && drowned.getCustomName() != null
                && drowned.getCustomName().getString().contains(
                        Text.translatable("baron.mythicrpg.drowned_king").getString()
                );
    }

    private boolean isDrownedKingDashing(DrownedEntity drowned) {
        return drowned.isTouchingWater()
                && drowned.getVelocity().lengthSquared() > 0.35;
    }
}