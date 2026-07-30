package com.mythicrpg.mining.archaeology.relic;

import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;

/** Transient selection state stored directly on a PlayerInventory instance. */
public final class PaletteSelectionState {
    int selectedIndex = -1;
    ItemStack cachedStack = ItemStack.EMPTY;
    ContainerComponent loadedComponent;

    void reset() {
        selectedIndex = -1;
        cachedStack = ItemStack.EMPTY;
        loadedComponent = null;
    }
}
