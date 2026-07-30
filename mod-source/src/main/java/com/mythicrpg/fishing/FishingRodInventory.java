package com.mythicrpg.fishing;

import com.mythicrpg.core.ItemContainerUtils;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/** Live, server-authoritative view of the upgrades stored on one exact fishing rod. */
public final class FishingRodInventory extends SimpleInventory {
    private final ItemStack rod;
    private boolean loading = true;

    public FishingRodInventory(ItemStack rod) {
        super(FishingRodLoadout.SLOT_COUNT);
        this.rod = rod;

        DefaultedList<ItemStack> stored = ItemContainerUtils.read(
                rod,
                FishingRodLoadout.SLOT_COUNT
        );
        for (int slot = 0; slot < FishingRodLoadout.SLOT_COUNT; slot++) {
            // Preserve malformed or legacy contents so the player can remove them safely.
            // Slot validation prevents any new invalid insertion.
            heldStacks.set(slot, stored.get(slot).copy());
        }
        loading = false;
    }

    public ItemStack rod() {
        return rod;
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (!loading) {
            save();
        }
    }

    private void save() {
        DefaultedList<ItemStack> stored = DefaultedList.ofSize(
                FishingRodLoadout.SLOT_COUNT,
                ItemStack.EMPTY
        );
        for (int slot = 0; slot < FishingRodLoadout.SLOT_COUNT; slot++) {
            stored.set(slot, heldStacks.get(slot).copy());
        }
        ItemContainerUtils.write(rod, stored);
    }
}
