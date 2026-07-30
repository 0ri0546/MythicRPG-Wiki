package com.mythicrpg.mining.archaeology.relic;

import com.mythicrpg.core.ModItems;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Inventaire virtuel persistant, directement adossé au composant CONTAINER de
 * la Palette tenue en offhand. Les mêmes slots sont utilisés par le client et
 * le serveur dans PlayerScreenHandler.
 */
public final class FossilPaletteInventory implements Inventory {
    private final PlayerEntity player;
    private final DefaultedList<ItemStack> contents = DefaultedList.ofSize(
            FossilPaletteItem.MAX_SLOTS,
            ItemStack.EMPTY
    );

    private ItemStack loadedPalette = ItemStack.EMPTY;
    private ContainerComponent loadedComponent = ContainerComponent.DEFAULT;

    public FossilPaletteInventory(PlayerEntity player) {
        this.player = player;
    }

    public boolean hasPalette() {
        return currentPalette().isOf(ModItems.FOSSIL_PALETTE);
    }

    public boolean isActiveSlot(int slot) {
        ItemStack palette = currentPalette();
        return palette.isOf(ModItems.FOSSIL_PALETTE)
                && slot >= 0
                && slot < FossilPaletteItem.slots(palette);
    }

    @Override
    public int size() {
        return FossilPaletteItem.MAX_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        loadIfNeeded();
        int capacity = activeCapacity();

        for (int slot = 0; slot < capacity; slot++) {
            if (!contents.get(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if (!isActiveSlot(slot)) {
            return ItemStack.EMPTY;
        }

        loadIfNeeded();
        return contents.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (!isActiveSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }

        loadIfNeeded();
        ItemStack removed = Inventories.splitStack(contents, slot, amount);

        if (!removed.isEmpty()) {
            save();
        }

        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (!isActiveSlot(slot)) {
            return ItemStack.EMPTY;
        }

        loadIfNeeded();
        ItemStack removed = Inventories.removeStack(contents, slot);

        if (!removed.isEmpty()) {
            save();
        }

        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (!isActiveSlot(slot) || !FossilPaletteItem.accepted(stack)) {
            return;
        }

        loadIfNeeded();
        ItemStack stored = stack.copy();
        stored.setCount(Math.min(stored.getCount(), Math.min(stored.getMaxCount(), getMaxCountPerStack())));
        contents.set(slot, stored);
        save();
    }

    @Override
    public void markDirty() {
        save();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return this.player == player;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return isActiveSlot(slot) && FossilPaletteItem.accepted(stack);
    }

    @Override
    public void clear() {
        if (!hasPalette()) {
            return;
        }

        loadIfNeeded();
        int capacity = activeCapacity();

        for (int slot = 0; slot < capacity; slot++) {
            contents.set(slot, ItemStack.EMPTY);
        }

        save();
    }

    private int activeCapacity() {
        ItemStack palette = currentPalette();
        return palette.isOf(ModItems.FOSSIL_PALETTE)
                ? FossilPaletteItem.slots(palette)
                : 0;
    }

    private ItemStack currentPalette() {
        return player.getOffHandStack();
    }

    private void loadIfNeeded() {
        ItemStack palette = currentPalette();

        if (!palette.isOf(ModItems.FOSSIL_PALETTE)) {
            if (!loadedPalette.isEmpty()) {
                clearCache();
            }
            return;
        }

        ContainerComponent component = palette.getOrDefault(
                DataComponentTypes.CONTAINER,
                ContainerComponent.DEFAULT
        );

        if (palette == loadedPalette && component.equals(loadedComponent)) {
            return;
        }

        loadedPalette = palette;
        loadedComponent = component;
        contents.clear();
        component.copyTo(contents);
    }

    private void save() {
        ItemStack palette = currentPalette();

        if (!palette.isOf(ModItems.FOSSIL_PALETTE)) {
            clearCache();
            return;
        }

        FossilPaletteItem.write(palette, contents);
        loadedPalette = palette;
        loadedComponent = palette.getOrDefault(
                DataComponentTypes.CONTAINER,
                ContainerComponent.DEFAULT
        );
        PaletteSelectionManager.invalidateCache(player);
    }

    private void clearCache() {
        loadedPalette = ItemStack.EMPTY;
        loadedComponent = ContainerComponent.DEFAULT;
        contents.clear();
    }
}
