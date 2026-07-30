
package com.mythicrpg.fishing;

import com.mythicrpg.crafting.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public final class FishNetScreenHandler extends ScreenHandler {
    private static final int PROPERTY_COUNT = 1;
    private static final int PROPERTY_CAPACITY = 0;
    private static final int PLAYER_INVENTORY_START = FishNetBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory inventory;
    private final PropertyDelegate properties;

    public FishNetScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(FishNetBlockEntity.INVENTORY_SIZE),
                new ArrayPropertyDelegate(PROPERTY_COUNT)
        );
    }

    public FishNetScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            int capacity
    ) {
        this(syncId, playerInventory, inventory, capacityProperties(capacity));
    }

    private FishNetScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties
    ) {
        super(ModScreenHandlers.FISH_NET, syncId);
        checkSize(inventory, FishNetBlockEntity.INVENTORY_SIZE);
        checkDataCount(properties, PROPERTY_COUNT);
        this.inventory = inventory;
        this.properties = properties;
        inventory.onOpen(playerInventory.player);

        for (int slot = 0; slot < FishNetBlockEntity.INVENTORY_SIZE; slot++) {
            addSlot(new CatchSlot(inventory, slot, 44 + slot * 18, 35));
        }
        addPlayerInventorySlots(playerInventory);
        addProperties(properties);
    }

    public int capacity() {
        return Math.max(0, Math.min(FishNetBlockEntity.INVENTORY_SIZE, properties.get(PROPERTY_CAPACITY)));
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
        if (index < FishNetBlockEntity.INVENTORY_SIZE) {
            if (!insertItem(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
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
        slot.onTakeItem(player, copy);
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

    private static PropertyDelegate capacityProperties(int capacity) {
        int bounded = Math.max(0, Math.min(FishNetBlockEntity.INVENTORY_SIZE, capacity));
        return new PropertyDelegate() {
            @Override
            public int get(int index) {
                return index == PROPERTY_CAPACITY ? bounded : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int size() {
                return PROPERTY_COUNT;
            }
        };
    }

    private final class CatchSlot extends Slot {
        private CatchSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isEnabled() {
            return getIndex() < capacity();
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return isEnabled();
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            super.onTakeItem(player, stack);
            if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
                FishingCatchData.read(stack).ifPresent(caught ->
                        FishingCodexManager.record(serverPlayer, caught)
                );
            }
        }
    }
}
