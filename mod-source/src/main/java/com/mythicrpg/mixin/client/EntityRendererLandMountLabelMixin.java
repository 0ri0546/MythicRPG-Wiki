package com.mythicrpg.mixin.client;

import com.mythicrpg.traveling.LandMountDataAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererLandMountLabelMixin<T extends Entity> {

    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$hideOwnedLabelForLocalRider(
            T entity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null
                || !(entity instanceof LandMountDataAccess access)
                || !access.mythicrpg$isAdoptedLandMount()) {
            return;
        }

        if (entity.hasPassenger(client.player)) {
            cir.setReturnValue(false);
        }
    }
}
