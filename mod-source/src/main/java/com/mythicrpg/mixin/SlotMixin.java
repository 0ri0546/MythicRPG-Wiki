package com.mythicrpg.mixin;

import com.mythicrpg.core.RecipeLockFeedbackManager;
import com.mythicrpg.core.RecipeUnlockManager;
import com.mythicrpg.crafting.LuckyInfusionManager;
import com.mythicrpg.crafting.MythicCraftingScreenHandler;
import com.mythicrpg.crafting.PortableCraftingManager;
import com.mythicrpg.crafting.PortableCraftingScreenHandler;
import com.mythicrpg.crafting.RecycleCraftManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow
    public abstract ItemStack getStack();

    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$validateCraftingResultTake(
            PlayerEntity player,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Slot slot = (Slot) (Object) this;

        if (!(slot instanceof CraftingResultSlot craftingResultSlot)) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        ItemStack result = this.getStack();

        if (!RecipeUnlockManager.canCraft(serverPlayer, result)) {
            RecipeLockFeedbackManager.sendLockedCraftFeedback(serverPlayer, result);
            cir.setReturnValue(false);
            return;
        }

        if (serverPlayer.currentScreenHandler instanceof MythicCraftingScreenHandler mythicCraftingScreenHandler
                && !mythicCraftingScreenHandler.canTakeCraftingResult(serverPlayer, result)) {
            cir.setReturnValue(false);
            return;
        }

        if (serverPlayer.currentScreenHandler instanceof PortableCraftingScreenHandler
                && PortableCraftingManager.getDurability(serverPlayer) <= 0) {
            cir.setReturnValue(false);
            return;
        }

        RecipeInputInventory input = ((CraftingResultSlotAccessor) craftingResultSlot).mythicrpg$getInput();

        if (!RecycleCraftManager.canTakeRecycleResult(serverPlayer, input, result)) {
            cir.setReturnValue(false);
            return;
        }

        if (!LuckyInfusionManager.canTakeInfusionResult(serverPlayer, input, result)) {
            cir.setReturnValue(false);
        }
    }
}
