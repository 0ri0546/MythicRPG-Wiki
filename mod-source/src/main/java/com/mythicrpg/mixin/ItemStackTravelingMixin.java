package com.mythicrpg.mixin;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.traveling.TravelingBonusCache;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public abstract class ItemStackTravelingMixin {
    @Inject(
            method = "damage(ILnet/minecraft/server/world/ServerWorld;Lnet/minecraft/server/network/ServerPlayerEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void mythicrpg$protectEquippedTravelingBoots(
            int amount,
            ServerWorld world,
            ServerPlayerEntity player,
            Consumer<Item> breakCallback,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;
        if (player.getEquippedStack(EquipmentSlot.FEET) != self) {
            return;
        }

        if (TravelingBonusCache.hasBonus(player, BonusType.TRAVEL_BOOTS_NO_DURABILITY)) {
            ci.cancel();
        }
    }
}
