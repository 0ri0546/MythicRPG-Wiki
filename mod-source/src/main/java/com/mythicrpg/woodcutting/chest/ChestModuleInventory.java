package com.mythicrpg.woodcutting.chest;

import com.mythicrpg.woodcutting.ChestModuleItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

/** Dedicated non-automatable module slots exposed only by the chest GUI. */
public final class ChestModuleInventory implements Inventory {

    private final ModularChestInventory chestInventory;

    public ChestModuleInventory(ModularChestInventory chestInventory) {
        this.chestInventory = chestInventory;
    }

    public boolean isActive(int slot) {
        return slot >= 0 && slot < chestInventory.chestCount();
    }

    public boolean canApply(int slot, ItemStack module) {
        return isActive(slot)
                && (module.isEmpty() || ChestModuleItem.isModule(module))
                && chestInventory.canChangeModule(slot, module);
    }

    @Override
    public int size() {
        return ModularChestInventory.MAX_CHESTS;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < chestInventory.chestCount(); slot++) {
            if (!getStack(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return isActive(slot) ? chestInventory.getModule(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if (amount <= 0 || !isActive(slot)) {
            return ItemStack.EMPTY;
        }
        return removeStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot) {
        if (!isActive(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack old = getStack(slot);
        if (old.isEmpty() || !chestInventory.tryChangeModule(slot, ItemStack.EMPTY)) {
            return ItemStack.EMPTY;
        }
        return old.copy();
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (!isActive(slot) || (!stack.isEmpty() && !ChestModuleItem.isModule(stack))) {
            return;
        }
        chestInventory.tryChangeModule(slot, stack);
    }

    @Override
    public void markDirty() {
        chestInventory.markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return chestInventory.canPlayerUse(player);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return isActive(slot) && ChestModuleItem.isModule(stack) && canApply(slot, stack);
    }

    @Override
    public void clear() {
        for (int slot = 0; slot < chestInventory.chestCount(); slot++) {
            chestInventory.tryChangeModule(slot, ItemStack.EMPTY);
        }
    }
}
