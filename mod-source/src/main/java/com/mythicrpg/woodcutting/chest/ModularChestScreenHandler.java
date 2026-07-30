package com.mythicrpg.woodcutting.chest;

import com.mythicrpg.crafting.ModScreenHandlers;
import com.mythicrpg.woodcutting.ChestModuleItem;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

/** Custom vanilla-chest handler with fixed module slots and up to 108 storage slots. */
public final class ModularChestScreenHandler extends ScreenHandler {

    public static final int STORAGE_START = 0;

    private static final int PROPERTY_CHEST_COUNT = 0;
    private static final int PROPERTY_TOTAL_CAPACITY = 1;
    private static final int PROPERTY_COUNT = 2;

    private final Inventory storageInventory;
    private final Inventory moduleInventory;
    private final PropertyDelegate properties;
    private final int storageSlotCount;
    private final int moduleSlotCount;
    private final int storageEnd;
    private final int moduleStart;
    private final int moduleEnd;
    private final int playerInventoryStart;
    private final int playerInventoryEnd;
    private final int hotbarStart;
    private final int hotbarEnd;
    @Nullable
    private final ModularChestInventory liveInventory;

    public static ModularChestScreenHandler createSingleClient(int syncId, PlayerInventory playerInventory) {
        return new ModularChestScreenHandler(
                ModScreenHandlers.MODULAR_CHEST_SINGLE,
                syncId,
                playerInventory,
                1,
                new SimpleInventory(ModularChestInventory.MAX_SLOTS_PER_CHEST),
                new SimpleInventory(1),
                new ArrayPropertyDelegate(PROPERTY_COUNT),
                null
        );
    }

    public static ModularChestScreenHandler createDoubleClient(int syncId, PlayerInventory playerInventory) {
        return new ModularChestScreenHandler(
                ModScreenHandlers.MODULAR_CHEST_DOUBLE,
                syncId,
                playerInventory,
                2,
                new SimpleInventory(ModularChestInventory.MAX_TOTAL_STORAGE),
                new SimpleInventory(ModularChestInventory.MAX_CHESTS),
                new ArrayPropertyDelegate(PROPERTY_COUNT),
                null
        );
    }

    public ModularChestScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            ModularChestInventory storageInventory
    ) {
        this(
                storageInventory.chestCount() == 1
                        ? ModScreenHandlers.MODULAR_CHEST_SINGLE
                        : ModScreenHandlers.MODULAR_CHEST_DOUBLE,
                syncId,
                playerInventory,
                storageInventory.chestCount(),
                storageInventory.screenView(),
                new ChestModuleInventory(storageInventory),
                createServerProperties(storageInventory),
                storageInventory
        );
    }

    private ModularChestScreenHandler(
            ScreenHandlerType<?> type,
            int syncId,
            PlayerInventory playerInventory,
            int chestCount,
            Inventory storageInventory,
            Inventory moduleInventory,
            PropertyDelegate properties,
            @Nullable ModularChestInventory liveInventory
    ) {
        super(type, syncId);
        checkDataCount(properties, PROPERTY_COUNT);
        this.storageInventory = storageInventory;
        this.moduleInventory = moduleInventory;
        this.properties = properties;
        this.liveInventory = liveInventory;
        this.moduleSlotCount = Math.max(1, Math.min(ModularChestInventory.MAX_CHESTS, chestCount));
        this.storageSlotCount = this.moduleSlotCount * ModularChestInventory.MAX_SLOTS_PER_CHEST;
        this.storageEnd = STORAGE_START + storageSlotCount;
        this.moduleStart = storageEnd;
        this.moduleEnd = moduleStart + moduleSlotCount;
        this.playerInventoryStart = moduleEnd;
        this.playerInventoryEnd = playerInventoryStart + 27;
        this.hotbarStart = playerInventoryEnd;
        this.hotbarEnd = hotbarStart + 9;

        storageInventory.onOpen(playerInventory.player);

        for (int slot = 0; slot < storageSlotCount; slot++) {
            int column = slot % 9;
            int row = slot / 9;
            addSlot(new StorageSlot(storageInventory, slot, 9 + column * 18, 19 + row * 18));
        }

        if (moduleSlotCount == 1) {
            addSlot(new ModuleSlot(moduleInventory, 0, 189, 56));
        } else {
            addSlot(new ModuleSlot(moduleInventory, 0, 189, 38));
            addSlot(new ModuleSlot(moduleInventory, 1, 189, 74));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        9 + column * 18,
                        141 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 9 + column * 18, 199));
        }

        addProperties(properties);
    }

    public int getChestCount() {
        return Math.max(1, Math.min(ModularChestInventory.MAX_CHESTS, properties.get(PROPERTY_CHEST_COUNT)));
    }

    public int getCapacity() {
        int vanillaMinimum = ModularChestInventory.BASE_SLOTS_PER_CHEST * getChestCount();
        return Math.max(
                vanillaMinimum,
                Math.min(storageSlotCount, properties.get(PROPERTY_TOTAL_CAPACITY))
        );
    }

    public boolean isStorageSlotActive(int storageSlot) {
        return storageSlot >= 0 && storageSlot < getCapacity();
    }

    public boolean isModuleSlotActive(int moduleSlot) {
        return moduleSlot >= 0 && moduleSlot < getChestCount();
    }

    public int getModuleStart() {
        return moduleStart;
    }

    public int getStorageSlotCount() {
        return storageSlotCount;
    }

    /** Used by the vanilla chest viewer tracker to recognize this custom handler. */
    public boolean isViewing(ChestBlockEntity chest) {
        return liveInventory != null && liveInventory.containsChest(chest);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return storageInventory.canPlayerUse(player);
    }

    @Override
    public void onSlotClick(
            int slotIndex,
            int button,
            SlotActionType actionType,
            PlayerEntity player
    ) {
        if (slotIndex >= moduleStart
                && slotIndex < moduleEnd
                && moduleInventory instanceof ChestModuleInventory modules) {
            int moduleIndex = slotIndex - moduleStart;
            if (!isModuleSlotActive(moduleIndex)) {
                return;
            }

            if (actionType == SlotActionType.QUICK_MOVE) {
                quickMove(player, slotIndex);
                return;
            }

            // Keep the dedicated module slot transactional. Other vanilla click
            // modes (number-key swap, throw, drag, clone, collect-all) are
            // deliberately refused so they cannot bypass the capacity check.
            if (actionType != SlotActionType.PICKUP || (button != 0 && button != 1)) {
                return;
            }

            handleModulePickup(player, modules, moduleIndex);
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        if (slotIndex >= moduleStart && slotIndex < moduleEnd) {
            return quickMoveModuleToPlayer(player, slotIndex, slot);
        }

        ItemStack source = slot.getStack();
        ItemStack original = source.copy();

        if (slotIndex >= STORAGE_START && slotIndex < storageEnd) {
            if (!insertItem(source, playerInventoryStart, hotbarEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= playerInventoryStart && slotIndex < hotbarEnd) {
            boolean moved = false;
            if (ChestModuleItem.isModule(source)) {
                moved = insertItem(source, moduleStart, moduleEnd, false);
            }
            if (!moved && !insertItem(source, STORAGE_START, storageEnd, false)) {
                if (slotIndex < hotbarStart) {
                    if (!insertItem(source, hotbarStart, hotbarEnd, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!insertItem(source, playerInventoryStart, playerInventoryEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }

        if (source.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTakeItem(player, source);
        return original;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        storageInventory.onClose(player);
    }

    private void handleModulePickup(
            PlayerEntity player,
            ChestModuleInventory modules,
            int moduleIndex
    ) {
        ItemStack installed = moduleInventory.getStack(moduleIndex).copy();
        ItemStack cursor = getCursorStack();
        Slot moduleSlot = slots.get(moduleStart + moduleIndex);

        if (cursor.isEmpty()) {
            if (installed.isEmpty()) {
                return;
            }

            ItemStack removed = moduleInventory.removeStack(moduleIndex);
            if (removed.isEmpty()) {
                notifyCannotShrink(player);
                return;
            }

            setCursorStack(removed);
            moduleSlot.markDirty();
            sendContentUpdates();
            return;
        }

        if (!ChestModuleItem.isModule(cursor)) {
            return;
        }

        // A replacement must leave room on the cursor for the old module.
        // Normal module stacks are size one, but this also defends against
        // command-created overstacked items.
        if (!installed.isEmpty() && cursor.getCount() != 1) {
            return;
        }

        ItemStack requested = cursor.copyWithCount(1);
        if (sameItemAndComponents(installed, requested)) {
            return;
        }
        if (!modules.canApply(moduleIndex, requested)) {
            if (ChestModuleItem.extraSlots(requested) < ChestModuleItem.extraSlots(installed)) {
                notifyCannotShrink(player);
            }
            return;
        }

        moduleInventory.setStack(moduleIndex, requested);
        if (!sameItemAndComponents(moduleInventory.getStack(moduleIndex), requested)) {
            return;
        }

        if (installed.isEmpty()) {
            ItemStack remainingCursor = cursor.copy();
            remainingCursor.decrement(1);
            setCursorStack(remainingCursor);
        } else {
            setCursorStack(installed);
        }

        moduleSlot.markDirty();
        sendContentUpdates();
    }

    private ItemStack quickMoveModuleToPlayer(PlayerEntity player, int slotIndex, Slot moduleSlot) {
        ItemStack installed = moduleSlot.getStack();
        if (installed.isEmpty()
                || !canInsertFully(installed, playerInventoryStart, hotbarEnd, true)) {
            return ItemStack.EMPTY;
        }

        int moduleIndex = slotIndex - moduleStart;
        ItemStack removed = moduleInventory.removeStack(moduleIndex);
        if (removed.isEmpty()) {
            notifyCannotShrink(player);
            return ItemStack.EMPTY;
        }

        ItemStack result = removed.copy();
        if (!insertItem(removed, playerInventoryStart, hotbarEnd, true) || !removed.isEmpty()) {
            moduleInventory.setStack(moduleIndex, result);
            return ItemStack.EMPTY;
        }

        moduleSlot.onTakeItem(player, result);
        sendContentUpdates();
        return result;
    }

    private static boolean sameItemAndComponents(ItemStack first, ItemStack second) {
        return first.isEmpty() && second.isEmpty()
                || !first.isEmpty()
                && !second.isEmpty()
                && ItemStack.areItemsAndComponentsEqual(first, second);
    }

    private static void notifyCannotShrink(PlayerEntity player) {
        player.sendMessage(
                Text.translatable("message.mythicrpg.chest_module.cannot_shrink")
                        .formatted(Formatting.RED),
                true
        );
    }

    private boolean canInsertFully(ItemStack source, int start, int end, boolean fromLast) {
        ItemStack remaining = source.copy();
        int index = fromLast ? end - 1 : start;
        int step = fromLast ? -1 : 1;

        for (; index >= start && index < end && !remaining.isEmpty(); index += step) {
            Slot target = slots.get(index);
            if (!target.isEnabled() || !target.canInsert(remaining)) {
                continue;
            }

            ItemStack present = target.getStack();
            int maximum = Math.min(target.getMaxItemCount(remaining), remaining.getMaxCount());
            if (present.isEmpty()) {
                remaining.decrement(Math.min(maximum, remaining.getCount()));
            } else if (ItemStack.areItemsAndComponentsEqual(present, remaining)) {
                int room = maximum - present.getCount();
                if (room > 0) {
                    remaining.decrement(Math.min(room, remaining.getCount()));
                }
            }
        }
        return remaining.isEmpty();
    }

    private static PropertyDelegate createServerProperties(ModularChestInventory inventory) {
        return new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case PROPERTY_CHEST_COUNT -> inventory.chestCount();
                    case PROPERTY_TOTAL_CAPACITY -> inventory.activeSize();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                // Server state is derived directly from the live chest inventories.
            }

            @Override
            public int size() {
                return PROPERTY_COUNT;
            }
        };
    }

    private final class StorageSlot extends Slot {
        private StorageSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isEnabled() {
            return isStorageSlotActive(getIndex());
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return isEnabled() && super.canInsert(stack);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return isEnabled() && super.canTakeItems(player);
        }
    }

    private final class ModuleSlot extends Slot {
        private ModuleSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean isEnabled() {
            return isModuleSlotActive(getIndex());
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (!isEnabled() || !ChestModuleItem.isModule(stack)) {
                return false;
            }
            return !(moduleInventory instanceof ChestModuleInventory modules)
                    || modules.canApply(getIndex(), stack);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            // The backing inventory performs the authoritative shrink transaction.
            return isEnabled();
        }

        @Override
        public int getMaxItemCount() {
            return 1;
        }
    }
}
