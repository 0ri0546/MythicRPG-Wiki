package com.mythicrpg.mixin;

import com.mythicrpg.crafting.CraftXpManager;
import com.mythicrpg.crafting.ReinforcedCraftManager;
import com.mythicrpg.crafting.LuckyInfusionManager;
import com.mythicrpg.crafting.PortableCraftingManager;
import com.mythicrpg.crafting.MythicCraftingScreenHandler;
import com.mythicrpg.crafting.PortableCraftingScreenHandler;
import com.mythicrpg.crafting.ResourceSaverManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CraftingResultSlot.class)
public abstract class CraftingResultSlotMixin {

    @Shadow
    @Final
    private RecipeInputInventory input;

    @Shadow
    @Final
    private PlayerEntity player;

    @Shadow
    private int amount;

    @Inject(method = "onCrafted(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"))
    private void mythicrpg$grantCraftingXp(ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        if (stack.isEmpty()) {
            return;
        }

        int craftedItemAmount = Math.max(1, this.amount);

        ItemStack resultPerCraftStack = ((CraftingResultSlot) (Object) this).getStack();
        int resultPerCraftCount = resultPerCraftStack.isEmpty()
                ? Math.max(1, stack.getCount())
                : Math.max(1, resultPerCraftStack.getCount());

        ItemStack resultPerCraft = stack.copyWithCount(resultPerCraftCount);

        int craftedTimes = Math.max(
                1,
                (int) Math.ceil(craftedItemAmount / (double) resultPerCraftCount)
        );

        if (serverPlayer.currentScreenHandler instanceof MythicCraftingScreenHandler mythicCraftingScreenHandler) {
            if (!mythicCraftingScreenHandler.tryConsumeStationDurability(serverPlayer, craftedTimes)) {
                return;
            }
        } else if (serverPlayer.currentScreenHandler instanceof PortableCraftingScreenHandler) {
            if (!PortableCraftingManager.tryConsumeCharges(serverPlayer, craftedTimes)) {
                return;
            }

            PortableCraftingManager.sendDurability(serverPlayer);
        }

        mythicrpg$resourceSaverResultPerCraft = resultPerCraft.copy();
        mythicrpg$resourceSaverCraftedTimes = craftedTimes;
        mythicrpg$resourceSaverEligible = ResourceSaverManager.isEligible(
                serverPlayer,
                input,
                resultPerCraft
        );

        LuckyInfusionManager.applyToResult(serverPlayer, input, stack);
        ReinforcedCraftManager.apply(serverPlayer, stack);

        CraftXpManager.handleCraft(
                serverPlayer,
                input,
                resultPerCraft,
                craftedTimes
        );
    }

    @Unique
    private DefaultedList<ItemStack> mythicrpg$resourceSaverInputBefore = null;

    @Unique
    private ItemStack mythicrpg$resourceSaverResultPerCraft = ItemStack.EMPTY;

    @Unique
    private int mythicrpg$resourceSaverCraftedTimes = 0;

    @Unique
    private boolean mythicrpg$resourceSaverEligible = false;

    @Inject(
            method = "onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private void mythicrpg$captureResourceSaverInput(
            PlayerEntity taker,
            ItemStack stack,
            CallbackInfo ci
    ) {
        if (!(taker instanceof ServerPlayerEntity serverPlayer)
                || !ResourceSaverManager.hasResourceSaver(serverPlayer)) {
            mythicrpg$clearResourceSaverData();
            return;
        }

        mythicrpg$resourceSaverInputBefore = DefaultedList.ofSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < input.size(); i++) {
            mythicrpg$resourceSaverInputBefore.set(i, input.getStack(i).copy());
        }

        mythicrpg$resourceSaverResultPerCraft = ItemStack.EMPTY;
        mythicrpg$resourceSaverCraftedTimes = 0;
        mythicrpg$resourceSaverEligible = false;
    }

    @Inject(
            method = "onTakeItem(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("TAIL")
    )
    private void mythicrpg$restoreResourceSaverInput(
            PlayerEntity taker,
            ItemStack stack,
            CallbackInfo ci
    ) {
        if (!(taker instanceof ServerPlayerEntity serverPlayer)) {
            mythicrpg$clearResourceSaverData();
            return;
        }

        ResourceSaverManager.tryRestoreSavedResources(
                serverPlayer,
                mythicrpg$resourceSaverInputBefore,
                input,
                mythicrpg$resourceSaverResultPerCraft,
                mythicrpg$resourceSaverCraftedTimes,
                mythicrpg$resourceSaverEligible
        );

        mythicrpg$clearResourceSaverData();
    }

    @Unique
    private void mythicrpg$clearResourceSaverData() {
        mythicrpg$resourceSaverInputBefore = null;
        mythicrpg$resourceSaverResultPerCraft = ItemStack.EMPTY;
        mythicrpg$resourceSaverCraftedTimes = 0;
        mythicrpg$resourceSaverEligible = false;
    }
}