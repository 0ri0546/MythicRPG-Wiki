package com.mythicrpg.mining.archaeology.relic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/** Slot visible uniquement quand la Palette en offhand possède cet emplacement. */
public final class FossilPaletteSlot extends Slot {
    private final FossilPaletteInventory paletteInventory;

    public FossilPaletteSlot(
            FossilPaletteInventory paletteInventory,
            int index,
            int x,
            int y
    ) {
        super(paletteInventory, index, x, y);
        this.paletteInventory = paletteInventory;
    }

    @Override
    public boolean isEnabled() {
        return paletteInventory.isActiveSlot(getIndex());
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return isEnabled() && FossilPaletteItem.accepted(stack);
    }

    @Override
    public boolean canTakeItems(PlayerEntity player) {
        return isEnabled();
    }
}
