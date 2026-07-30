
package com.mythicrpg.fishing;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
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
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class FishingRodScreenHandler extends ScreenHandler {
    public static final int UPGRADE_SLOT_COUNT = 3;
    private static final int PROPERTY_COUNT = 1;
    private static final int PROPERTY_RUNES_UNLOCKED = 0;

    private static final int PLAYER_INVENTORY_START = UPGRADE_SLOT_COUNT;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_START + 27;
    private static final int OFFHAND_INVENTORY_SLOT = 40;

    private final Inventory inventory;
    private final PlayerInventory playerInventory;
    private final PropertyDelegate properties;
    private final int sourceInventorySlot;
    private final int sourceScreenSlot;
    private final UUID openedRodId;

    public FishingRodScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(UPGRADE_SLOT_COUNT),
                new ArrayPropertyDelegate(PROPERTY_COUNT),
                -1,
                null
        );
    }

    public FishingRodScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            int sourceInventorySlot,
            UUID openedRodId
    ) {
        this(
                syncId,
                playerInventory,
                inventory,
                serverProperties(playerInventory.player),
                sourceInventorySlot,
                openedRodId
        );
    }

    private FishingRodScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties,
            int sourceInventorySlot,
            UUID openedRodId
    ) {
        super(ModScreenHandlers.FISHING_ROD, syncId);
        checkSize(inventory, UPGRADE_SLOT_COUNT);
        checkDataCount(properties, PROPERTY_COUNT);
        this.inventory = inventory;
        this.playerInventory = playerInventory;
        this.properties = properties;
        this.sourceInventorySlot = sourceInventorySlot;
        this.sourceScreenSlot = toScreenSlot(sourceInventorySlot);
        this.openedRodId = openedRodId;

        inventory.onOpen(playerInventory.player);

        addSlot(new BaitSlot(inventory, FishingRodLoadout.BAIT_SLOT, 44, 35));
        addSlot(new RuneSlot(inventory, FishingRodLoadout.FIRST_RUNE_SLOT, 98, 35, FishingRodLoadout.SECOND_RUNE_SLOT));
        addSlot(new RuneSlot(inventory, FishingRodLoadout.SECOND_RUNE_SLOT, 120, 35, FishingRodLoadout.FIRST_RUNE_SLOT));

        addPlayerInventorySlots(playerInventory);
        addProperties(properties);
    }

    public boolean hasRuneSlots() {
        return properties.get(PROPERTY_RUNES_UNLOCKED) != 0;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (!rodStillPresent()) {
            closeScreen(player);
            return false;
        }
        return inventory.canPlayerUse(player);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!rodStillPresent()) {
            closeScreen(player);
            return;
        }
        if (isLockedSourceAction(slotIndex, button, actionType)) {
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
        if (!rodStillPresent()) {
            closeScreen(player);
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        if (!rodStillPresent() || index == sourceScreenSlot) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(index);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();
        if (index < UPGRADE_SLOT_COUNT) {
            if (!insertItem(stack, PLAYER_INVENTORY_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FishingUpgradeItem upgrade) {
            int start = upgrade.isBait()
                    ? FishingRodLoadout.BAIT_SLOT
                    : FishingRodLoadout.FIRST_RUNE_SLOT;
            int end = upgrade.isBait()
                    ? FishingRodLoadout.BAIT_SLOT + 1
                    : FishingRodLoadout.SECOND_RUNE_SLOT + 1;
            if (!insertItem(stack, start, end, false)) {
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

    private boolean rodStillPresent() {
        if (sourceInventorySlot < 0 || openedRodId == null) {
            return true;
        }
        if (sourceInventorySlot >= playerInventory.size()) {
            return false;
        }
        return FishingRodData.matches(playerInventory.getStack(sourceInventorySlot), openedRodId);
    }

    private boolean isLockedSourceAction(int slotIndex, int button, SlotActionType actionType) {
        if (sourceInventorySlot < 0) {
            return false;
        }
        if (slotIndex == sourceScreenSlot) {
            return true;
        }
        if (actionType == SlotActionType.SWAP) {
            if (sourceInventorySlot >= 0 && sourceInventorySlot < 9 && button == sourceInventorySlot) {
                return true;
            }
            return sourceInventorySlot == OFFHAND_INVENTORY_SLOT && button == OFFHAND_INVENTORY_SLOT;
        }
        return false;
    }

    private static int toScreenSlot(int inventorySlot) {
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return PLAYER_HOTBAR_START + inventorySlot;
        }
        if (inventorySlot >= 9 && inventorySlot < 36) {
            return PLAYER_INVENTORY_START + inventorySlot - 9;
        }
        return -1;
    }

    private static void closeScreen(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.closeHandledScreen();
        }
    }

    private static PropertyDelegate serverProperties(PlayerEntity player) {
        return new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (index != PROPERTY_RUNES_UNLOCKED) {
                    return 0;
                }
                return player instanceof ServerPlayerEntity serverPlayer
                        && SkillTreeManager.hasBonus(
                                serverPlayer,
                                SkillType.FISHING,
                                BonusType.FISHING_RUNE_SLOTS
                        ) ? 1 : 0;
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

    private static final class BaitSlot extends Slot {
        private BaitSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return stack.getItem() instanceof FishingUpgradeItem upgrade && upgrade.isBait();
        }
    }

    private final class RuneSlot extends Slot {
        private final int otherSlot;

        private RuneSlot(Inventory inventory, int index, int x, int y, int otherSlot) {
            super(inventory, index, x, y);
            this.otherSlot = otherSlot;
        }

        @Override
        public boolean isEnabled() {
            // Existing runes remain removable after an eventual tree reset.
            return hasRuneSlots() || hasStack();
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (!hasRuneSlots()
                    || !(stack.getItem() instanceof FishingUpgradeItem upgrade)
                    || !upgrade.isRune()) {
                return false;
            }
            ItemStack existing = inventory.getStack(otherSlot);
            return existing.isEmpty() || existing.getItem() != stack.getItem();
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return true;
        }
    }
}
