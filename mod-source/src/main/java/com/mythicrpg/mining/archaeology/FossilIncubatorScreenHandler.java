package com.mythicrpg.mining.archaeology;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.crafting.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.Optional;

public final class FossilIncubatorScreenHandler extends ScreenHandler {

    private static final int MACHINE_SLOT_COUNT = FossilIncubatorBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory inventory;
    private final PropertyDelegate properties;

    public FossilIncubatorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(FossilIncubatorBlockEntity.INVENTORY_SIZE),
                new ArrayPropertyDelegate(FossilIncubatorBlockEntity.PROPERTY_COUNT)
        );
    }

    public FossilIncubatorScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties
    ) {
        super(ModScreenHandlers.FOSSIL_INCUBATOR, syncId);
        checkSize(inventory, FossilIncubatorBlockEntity.INVENTORY_SIZE);
        checkDataCount(properties, FossilIncubatorBlockEntity.PROPERTY_COUNT);
        this.inventory = inventory;
        this.properties = properties;

        inventory.onOpen(playerInventory.player);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                int slot = column + row * 3;
                addSlot(new InputSlot(inventory, slot, 17 + column * 18, 20 + row * 18));
            }
        }

        addSlot(new InputSlot(inventory, FossilIncubatorBlockEntity.WATER_SLOT, 82, 29));
        addSlot(new InputSlot(inventory, FossilIncubatorBlockEntity.KELP_SLOT, 82, 53));
        addSlot(new ResultSlot(inventory, FossilIncubatorBlockEntity.RESULT_SLOT, 151, 38));

        addPlayerInventorySlots(playerInventory);
        addProperties(properties);
    }

    public boolean isProcessing() {
        return properties.get(FossilIncubatorBlockEntity.PROPERTY_STATUS) == 1;
    }

    public boolean hasResult() {
        return properties.get(FossilIncubatorBlockEntity.PROPERTY_STATUS) == 2;
    }

    public int getRemainingTicks() {
        return properties.get(FossilIncubatorBlockEntity.PROPERTY_REMAINING_TICKS);
    }

    public int getTotalTicks() {
        return properties.get(FossilIncubatorBlockEntity.PROPERTY_TOTAL_TICKS);
    }

    public int getProgressWidth(int maxWidth) {
        int total = getTotalTicks();
        if (total <= 0) {
            return 0;
        }
        int elapsed = Math.max(0, total - getRemainingTicks());
        return Math.min(maxWidth, elapsed * maxWidth / total);
    }

    public FossilRarity getOutputRarity() {
        int rank = properties.get(FossilIncubatorBlockEntity.PROPERTY_OUTPUT_RARITY);
        for (FossilRarity rarity : FossilRarity.values()) {
            if (rarity.rank() == rank) {
                return rarity;
            }
        }
        return FossilRarity.COMMON;
    }

    public FossilFamily getOutputFamily() {
        int ordinal = properties.get(FossilIncubatorBlockEntity.PROPERTY_OUTPUT_FAMILY);
        FossilFamily[] families = FossilFamily.values();
        return ordinal >= 0 && ordinal < families.length
                ? families[ordinal]
                : FossilFamily.SMALL_LAND;
    }

    /** Shows the current input prediction, or the saved output while processing/ready. */
    public Optional<FossilIncubationRecipe.Output> getPreviewOutput() {
        if (isProcessing() || hasResult()) {
            FossilFamily family = getOutputFamily();
            FossilRarity rarity = getOutputRarity();
            return FossilContentRegistry.skeletonItem(family, rarity)
                    .map(item -> new FossilIncubationRecipe.Output(family, rarity, item));
        }
        return FossilIncubationRecipe.resolveFossils(inventory);
    }

    public boolean canStartClient() {
        return !isProcessing()
                && !hasResult()
                && inventory.getStack(FossilIncubatorBlockEntity.RESULT_SLOT).isEmpty()
                && FossilIncubationRecipe.resolveReadyRecipe(inventory).isPresent();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != 0
                || !(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)
                || !SkillTreeManager.hasBonus(serverPlayer, SkillType.MINING, BonusType.FOSSIL_INCUBATION)) {
            return false;
        }
        if (inventory instanceof FossilIncubatorBlockEntity incubator && incubator.startProcess()) {
            sendContentUpdates();
            return true;
        }
        return false;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getStack();
        ItemStack copy = stack.copy();

        if (slotIndex == FossilIncubatorBlockEntity.RESULT_SLOT) {
            if (!(inventory instanceof FossilIncubatorBlockEntity incubator)) {
                return ItemStack.EMPTY;
            }

            ItemStack prepared = stack.copy();
            if (!incubator.prepareSkeletonClaim(player, prepared)) {
                return ItemStack.EMPTY;
            }

            ItemStack transfer = prepared.copy();
            if (!insertItem(transfer, PLAYER_INVENTORY_START, HOTBAR_END, true)
                    || !transfer.isEmpty()) {
                return ItemStack.EMPTY;
            }

            stack.decrement(prepared.getCount());
            slot.markDirty();
            incubator.recordSkeletonClaim(player, prepared);
            slot.onQuickTransfer(stack, prepared);
            return prepared;
        } else if (slotIndex < MACHINE_SLOT_COUNT) {
            if (!insertItem(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.getItem() instanceof FossilItem) {
            if (!insertItem(
                    stack,
                    FossilIncubatorBlockEntity.FOSSIL_SLOT_START,
                    FossilIncubatorBlockEntity.FOSSIL_SLOT_END,
                    false
            )) {
                return ItemStack.EMPTY;
            }
        } else if (stack.isOf(Items.WATER_BUCKET)) {
            if (!insertItem(stack, FossilIncubatorBlockEntity.WATER_SLOT, FossilIncubatorBlockEntity.WATER_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.isOf(Items.KELP)) {
            if (!insertItem(stack, FossilIncubatorBlockEntity.KELP_SLOT, FossilIncubatorBlockEntity.KELP_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_INVENTORY_START && slotIndex < PLAYER_INVENTORY_END) {
            if (!insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= HOTBAR_START && slotIndex < HOTBAR_END) {
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
                        91 + row * 18
                ));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 149));
        }
    }

    private final class InputSlot extends Slot {
        private InputSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (isProcessing()) {
                return false;
            }

            int slotIndex = getIndex();
            if (slotIndex >= FossilIncubatorBlockEntity.FOSSIL_SLOT_START
                    && slotIndex < FossilIncubatorBlockEntity.FOSSIL_SLOT_END) {
                return FossilIncubationRecipe.canInsertFossil(inventory, slotIndex, stack);
            }
            if (slotIndex == FossilIncubatorBlockEntity.WATER_SLOT) {
                return stack.isOf(Items.WATER_BUCKET);
            }
            if (slotIndex == FossilIncubatorBlockEntity.KELP_SLOT) {
                return stack.isOf(Items.KELP);
            }
            return false;
        }

        @Override
        public int getMaxItemCount(ItemStack stack) {
            int slotIndex = getIndex();
            if ((slotIndex >= FossilIncubatorBlockEntity.FOSSIL_SLOT_START
                    && slotIndex < FossilIncubatorBlockEntity.FOSSIL_SLOT_END)
                    || slotIndex == FossilIncubatorBlockEntity.WATER_SLOT) {
                return 1;
            }
            return super.getMaxItemCount(stack);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return !isProcessing();
        }
    }

    private static final class ResultSlot extends Slot {
        private ResultSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            if (inventory instanceof FossilIncubatorBlockEntity incubator) {
                incubator.claimSkeleton(player, stack);
            }
            super.onTakeItem(player, stack);
        }
    }
}
