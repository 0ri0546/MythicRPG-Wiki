package com.mythicrpg.mixin.client;

import com.mythicrpg.fishing.MythicFishingRodItem;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingBobberEntityRenderer.class)
public abstract class FishingBobberEntityRendererMixin {

    /**
     * Vanilla vérifie uniquement minecraft:fishing_rod pour choisir
     * le côté depuis lequel la ligne doit partir.
     *
     * Les trois cannes Mythic héritent de MythicFishingRodItem :
     * elles doivent donc être reconnues comme des cannes valides.
     */
    @Redirect(
            method = "getHandPos(Lnet/minecraft/entity/player/PlayerEntity;FF)"
                    + "Lnet/minecraft/util/math/Vec3d;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;"
                            + "isOf(Lnet/minecraft/item/Item;)Z"
            )
    )
    private boolean mythicrpg$recognizeCustomFishingRod(
            ItemStack stack,
            Item expectedItem
    ) {
        return stack.isOf(expectedItem)
                || stack.getItem() instanceof MythicFishingRodItem;
    }
}