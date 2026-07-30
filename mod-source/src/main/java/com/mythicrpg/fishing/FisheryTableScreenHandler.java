
package com.mythicrpg.fishing;

import com.mythicrpg.core.ModItems;
import com.mythicrpg.crafting.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class FisheryTableScreenHandler extends ScreenHandler {
    private static final int PLAYER_INVENTORY_START = FisheryTableBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory inventory;

    public FisheryTableScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(FisheryTableBlockEntity.INVENTORY_SIZE)
        );
    }

    public FisheryTableScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory
    ) {
        super(ModScreenHandlers.FISHERY_TABLE, syncId);
        checkSize(inventory, FisheryTableBlockEntity.INVENTORY_SIZE);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        addSlot(new InputSlot(inventory, FisheryTableBlockEntity.INPUT_SLOT, 52, 35));
        addSlot(new OutputSlot(inventory, FisheryTableBlockEntity.OUTPUT_SLOT, 106, 35));
        addPlayerInventorySlots(playerInventory);
    }

    public ItemStack previewOutput() {
        if (inventory instanceof FisheryTableBlockEntity table) {
            return table.previewOutput();
        }
        FishingCatchData.Catch caught = FishingCatchData.read(
                inventory.getStack(FisheryTableBlockEntity.INPUT_SLOT)
        ).orElse(null);
        if (caught == null) return ItemStack.EMPTY;
        return new ItemStack(ModItems.fishingMaterial(
                caught.rarity(),
                caught.family() == FishingFamily.CRUSTACEAN
        ));
    }

    public boolean canTransformClient() {
        ItemStack preview = previewOutput();
        if (preview.isEmpty()) return false;
        ItemStack output = inventory.getStack(FisheryTableBlockEntity.OUTPUT_SLOT);
        return output.isEmpty()
                || (ItemStack.areItemsAndComponentsEqual(output, preview)
                && output.getCount() < output.getMaxCount());
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != 0 || !(inventory instanceof FisheryTableBlockEntity table)) return false;
        if (!table.canPlayerUse(player) || !table.transformOne()) return false;
        sendContentUpdates();
        return true;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index < FisheryTableBlockEntity.INVENTORY_SIZE) {
            if (!insertItem(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (FishingCatchData.read(stack).isPresent()) {
            if (!insertItem(
                    stack,
                    FisheryTableBlockEntity.INPUT_SLOT,
                    FisheryTableBlockEntity.INPUT_SLOT + 1,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (index >= PLAYER_INVENTORY_START && index < PLAYER_INVENTORY_END) {
            if (!insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index >= HOTBAR_START && index < HOTBAR_END) {
            if (!insertItem(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        if (stack.getCount() == copy.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTakeItem(player, stack);
        return copy;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        83 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 141));
        }
    }

    private static final class InputSlot extends Slot {
        private InputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return FishingCatchData.read(stack).isPresent();
        }
    }

    private static final class OutputSlot extends Slot {
        private OutputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}
