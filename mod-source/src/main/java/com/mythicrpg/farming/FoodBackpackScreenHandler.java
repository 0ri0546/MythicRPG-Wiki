package com.mythicrpg.farming;

import com.mythicrpg.core.ModItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;

public class FoodBackpackScreenHandler extends GenericContainerScreenHandler {

    private static final int BACKPACK_ROWS = 6;
    private static final int BACKPACK_SLOT_COUNT = 54;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = 36;
    private static final int PLAYER_INVENTORY_START = BACKPACK_SLOT_COUNT;
    private static final int PLAYER_HOTBAR_START = PLAYER_INVENTORY_START + 27;
    private static final int OFFHAND_INVENTORY_SLOT = 40;

    private final PlayerInventory playerInventory;
    private final int openedBackpackInventorySlot;
    private final int openedBackpackScreenSlot;
    private final String openedBackpackId;

    public FoodBackpackScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory backpackInventory
    ) {
        this(syncId, playerInventory, backpackInventory, -1, "");
    }

    public FoodBackpackScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory backpackInventory,
            int openedBackpackInventorySlot,
            String openedBackpackId
    ) {
        super(
                ScreenHandlerType.GENERIC_9X6,
                syncId,
                playerInventory,
                backpackInventory,
                BACKPACK_ROWS
        );

        this.playerInventory = playerInventory;
        this.openedBackpackInventorySlot = openedBackpackInventorySlot;
        this.openedBackpackScreenSlot = toScreenSlot(openedBackpackInventorySlot);
        this.openedBackpackId = openedBackpackId == null ? "" : openedBackpackId;

        for (int i = 0; i < BACKPACK_SLOT_COUNT; i++) {
            Slot oldSlot = this.slots.get(i);

            this.slots.set(
                    i,
                    new FarmingOnlySlot(
                            backpackInventory,
                            i,
                            oldSlot.x,
                            oldSlot.y
                    )
            );
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (!isOpenedBackpackStillPresent()) {
            closeScreen(player);
            return false;
        }

        return true;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (!isOpenedBackpackStillPresent()) {
            closeScreen(player);
            return;
        }

        if (isLockedBackpackSlotAction(slotIndex, button, actionType)) {
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);

        if (!isOpenedBackpackStillPresent()) {
            closeScreen(player);
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (!isOpenedBackpackStillPresent()) {
            closeScreen(player);
            return ItemStack.EMPTY;
        }

        if (slotIndex == openedBackpackScreenSlot) {
            return ItemStack.EMPTY;
        }

        return super.quickMove(player, slotIndex);
    }

    private boolean isLockedBackpackSlotAction(int slotIndex, int button, SlotActionType actionType) {
        if (openedBackpackInventorySlot < 0) {
            return false;
        }

        // Bloque toute action directe sur le slot qui contient le backpack ouvert.
        if (slotIndex == openedBackpackScreenSlot) {
            return true;
        }

        // Bloque les swaps via touches 1-9 / offhand si le backpack ouvert est la cible du swap.
        if (actionType == SlotActionType.SWAP) {
            if (button == openedBackpackInventorySlot) {
                return true;
            }

            if (openedBackpackInventorySlot == OFFHAND_INVENTORY_SLOT && button == OFFHAND_INVENTORY_SLOT) {
                return true;
            }
        }

        return false;
    }

    private boolean isOpenedBackpackStillPresent() {
        if (openedBackpackInventorySlot < 0) {
            return true;
        }

        if (openedBackpackInventorySlot >= playerInventory.size()) {
            return false;
        }

        ItemStack current = playerInventory.getStack(openedBackpackInventorySlot);
        return current.isOf(ModItems.FOOD_BACKPACK)
                && (openedBackpackId.isBlank()
                        || openedBackpackId.equals(FoodBackpackDeathData.getBackpackId(current)));
    }

    private static void closeScreen(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.closeHandledScreen();
        }
    }

    private static int toScreenSlot(int inventorySlot) {
        if (inventorySlot < 0) {
            return -1;
        }

        // PlayerInventory slots 0-8 = hotbar.
        // Dans GenericContainerScreenHandler, la hotbar est après les 27 slots d'inventaire principal.
        if (inventorySlot >= 0 && inventorySlot < 9) {
            return PLAYER_HOTBAR_START + inventorySlot;
        }

        // PlayerInventory slots 9-35 = inventaire principal.
        if (inventorySlot >= 9 && inventorySlot < PLAYER_INVENTORY_SLOT_COUNT) {
            return PLAYER_INVENTORY_START + (inventorySlot - 9);
        }

        // Offhand / armor : pas affichés dans cette interface.
        return -1;
    }

    private static final class FarmingOnlySlot extends Slot {

        private FarmingOnlySlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return FoodBackpackItem.isAcceptedItem(stack);
        }
    }
}