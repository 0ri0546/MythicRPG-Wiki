package com.mythicrpg.mining.archaeology.relic;

/** Implemented by the PlayerInventory mixin to avoid a global weak-map lookup. */
public interface PaletteSelectionHolder {
    PaletteSelectionState mythicrpg$getPaletteSelectionState();
}
