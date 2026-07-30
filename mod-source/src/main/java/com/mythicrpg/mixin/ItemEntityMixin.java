package com.mythicrpg.mixin;

import com.mythicrpg.farming.EnchantedFlowerSmeltManager;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.eating.EatingPreservationManager;
import com.mythicrpg.traveling.TravelingDeathRecallManager;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void mythicrpg$smeltPickupWithEnchantedFlower(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemEntity itemEntity = (ItemEntity) (Object) this;
        EnchantedFlowerSmeltManager.trySmeltPickup(serverPlayer, itemEntity);
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$tickDroppedItems(CallbackInfo ci) {
        ItemEntity itemEntity = (ItemEntity) (Object) this;
        if (!(itemEntity.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        if (itemEntity.getItemAge() % 20 != 0) {
            return;
        }

        if (EatingPreservationManager.updateDroppedStorage(
                itemEntity.getStack(),
                serverWorld.getTime()
        )) {
            itemEntity.setStack(itemEntity.getStack().copy());
        }

        if (itemEntity.getStack().isOf(ModItems.DEATH_RECALL_TOKEN)
                && TravelingDeathRecallManager.shouldDiscardDroppedToken(
                        itemEntity.getStack(),
                        serverWorld.getServer()
                )) {
            itemEntity.discard();
            ci.cancel();
        }
    }

}