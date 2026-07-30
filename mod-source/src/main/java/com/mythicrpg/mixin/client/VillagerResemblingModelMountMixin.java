package com.mythicrpg.mixin.client;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.MerchantEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerResemblingModel.class)
public abstract class VillagerResemblingModelMountMixin {
    private static final float DEFAULT_ARMS_PITCH = -0.75F;
    private static final float RIDING_ARMS_PITCH = -2.25F;

    @Shadow
    @Final
    private ModelPart root;

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void mythicrpg$raiseMountedMerchantArms(
            Entity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (!(entity instanceof MerchantEntity)) {
            return;
        }

        ModelPart arms = root.getChild("arms");
        arms.pitch = entity instanceof net.minecraft.entity.LivingEntity living
                && LandMountManager.isAdoptedMount(living)
                && entity.hasPassengers()
                ? RIDING_ARMS_PITCH
                : DEFAULT_ARMS_PITCH;
    }
}
