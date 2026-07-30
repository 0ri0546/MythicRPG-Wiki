package com.mythicrpg.mixin;

import com.mythicrpg.mining.archaeology.relic.FossilPaletteInventory;
import com.mythicrpg.mining.archaeology.relic.FossilPaletteItem;
import com.mythicrpg.mining.archaeology.relic.FossilPaletteSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerScreenHandler.class)
public abstract class PlayerScreenHandlerPaletteMixin {
    @Unique
    private int mythicrpg$paletteSlotStart = -1;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void mythicrpg$addPaletteSlots(
            PlayerInventory inventory,
            boolean onServer,
            PlayerEntity owner,
            CallbackInfo ci
    ) {
        FossilPaletteInventory paletteInventory = new FossilPaletteInventory(owner);
        ScreenHandlerPaletteInvoker invoker = (ScreenHandlerPaletteInvoker) this;
        PlayerScreenHandler handler = (PlayerScreenHandler) (Object) this;

        mythicrpg$paletteSlotStart = handler.slots.size();

        for (int index = 0; index < FossilPaletteItem.MAX_SLOTS; index++) {
            int column = index % 2;
            int row = index / 2;
            invoker.mythicrpg$addPaletteSlot(new FossilPaletteSlot(
                    paletteInventory,
                    index,
                    180 + column * 18,
                    8 + row * 18
            ));
        }
    }

    @Inject(method = "quickMove", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$quickMovePaletteItems(
            PlayerEntity player,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        if (mythicrpg$paletteSlotStart < 0) {
            return;
        }

        int paletteSlotEnd = mythicrpg$paletteSlotStart + FossilPaletteItem.MAX_SLOTS;
        PlayerScreenHandler handler = (PlayerScreenHandler) (Object) this;
        ScreenHandlerPaletteInvoker invoker = (ScreenHandlerPaletteInvoker) this;

        if (slotIndex >= mythicrpg$paletteSlotStart && slotIndex < paletteSlotEnd) {
            mythicrpg$quickMoveFromPalette(handler, invoker, slotIndex, cir);
            return;
        }

        // Dans l'inventaire joueur, Maj-clic sur un bloc tente d'abord de le
        // ranger dans les slots actifs de la Palette. Si aucun emplacement
        // n'accepte le stack, le comportement vanilla reprend normalement.
        if (slotIndex >= 9 && slotIndex < 45) {
            Slot sourceSlot = handler.getSlot(slotIndex);

            if (!sourceSlot.hasStack() || !FossilPaletteItem.accepted(sourceSlot.getStack())) {
                return;
            }

            ItemStack source = sourceSlot.getStack();
            ItemStack original = source.copy();

            if (!invoker.mythicrpg$insertPaletteItem(
                    source,
                    mythicrpg$paletteSlotStart,
                    paletteSlotEnd,
                    false
            )) {
                return;
            }

            if (source.isEmpty()) {
                sourceSlot.setStack(ItemStack.EMPTY);
            } else {
                sourceSlot.markDirty();
            }

            cir.setReturnValue(original);
        }
    }

    @Unique
    private static void mythicrpg$quickMoveFromPalette(
            PlayerScreenHandler handler,
            ScreenHandlerPaletteInvoker invoker,
            int slotIndex,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        Slot slot = handler.getSlot(slotIndex);

        if (!slot.hasStack() || !slot.isEnabled()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        ItemStack source = slot.getStack();
        ItemStack original = source.copy();

        // Inventaire principal + hotbar, sans l'offhand qui contient la Palette.
        if (!invoker.mythicrpg$insertPaletteItem(source, 9, 45, false)) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (source.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        cir.setReturnValue(original);
    }
}
