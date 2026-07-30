package com.mythicrpg.eating;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CookingPotScreenHandler extends ScreenHandler {
    private static final int MACHINE_SLOT_COUNT = CookingPotBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_INVENTORY_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_START = PLAYER_INVENTORY_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Inventory inventory;
    private final PropertyDelegate properties;

    public CookingPotScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(
                syncId,
                playerInventory,
                new SimpleInventory(CookingPotBlockEntity.INVENTORY_SIZE),
                new ArrayPropertyDelegate(CookingPotBlockEntity.PROPERTY_COUNT)
        );
    }

    public CookingPotScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            Inventory inventory,
            PropertyDelegate properties
    ) {
        super(ModScreenHandlers.COOKING_POT, syncId);
        checkSize(inventory, CookingPotBlockEntity.INVENTORY_SIZE);
        checkDataCount(properties, CookingPotBlockEntity.PROPERTY_COUNT);
        this.inventory = inventory;
        this.properties = properties;
        inventory.onOpen(playerInventory.player);

        for (int slot = 0; slot < CookingPotBlockEntity.INPUT_SLOT_END; slot++) {
            addSlot(new IngredientSlot(inventory, slot, 26 + slot * 22, 31));
        }
        addSlot(new ResultSlot(inventory, CookingPotBlockEntity.RESULT_SLOT, 145, 31));
        addPlayerInventorySlots(playerInventory);
        addProperties(properties);
    }

    public int getAllowedSlots() {
        return Math.max(0, Math.min(5, properties.get(CookingPotBlockEntity.PROPERTY_ALLOWED_SLOTS)));
    }

    public boolean isProcessing() {
        return properties.get(CookingPotBlockEntity.PROPERTY_STATUS) == 1;
    }

    public boolean hasResult() {
        return properties.get(CookingPotBlockEntity.PROPERTY_STATUS) == 2;
    }

    public int getRemainingTicks() {
        return properties.get(CookingPotBlockEntity.PROPERTY_REMAINING_TICKS);
    }

    public int getTotalTicks() {
        return properties.get(CookingPotBlockEntity.PROPERTY_TOTAL_TICKS);
    }

    public boolean hasHeat() {
        return properties.get(CookingPotBlockEntity.PROPERTY_HEAT) == 1;
    }

    public boolean hasSignaturePerk() {
        return properties.get(CookingPotBlockEntity.PROPERTY_SIGNATURE_UNLOCKED) == 1;
    }

    public boolean isSignaturePrepared() {
        return properties.get(CookingPotBlockEntity.PROPERTY_SIGNATURE_PREPARED) == 1;
    }

    public boolean canPrepareSignatureClient() {
        if (!hasSignaturePerk() || isProcessing() || hasResult()) {
            return false;
        }
        for (int slot = 0; slot < CookingPotBlockEntity.INPUT_SLOT_END; slot++) {
            if (!inventory.getStack(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public int getReadyPortions() {
        return properties.get(CookingPotBlockEntity.PROPERTY_PORTIONS);
    }

    public DishRarity getOutputRarity() {
        return DishRarity.byRank(properties.get(CookingPotBlockEntity.PROPERTY_RARITY));
    }

    public DishCategory getOutputCategory() {
        int ordinal = properties.get(CookingPotBlockEntity.PROPERTY_CATEGORY);
        DishCategory[] values = DishCategory.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : DishCategory.MAIN;
    }

    public int getProgressWidth(int maxWidth) {
        int total = getTotalTicks();
        if (total <= 0) {
            return 0;
        }
        int elapsed = Math.max(0, total - getRemainingTicks());
        return Math.min(maxWidth, elapsed * maxWidth / total);
    }

    public Optional<CookingResult> getPreview() {
        if (isProcessing() || hasResult()) {
            ItemStack result = inventory.getStack(CookingPotBlockEntity.RESULT_SLOT);
            Optional<PreparedDishData.Dish> dish = PreparedDishData.read(result);
            if (dish.isPresent()) {
                PreparedDishData.Dish value = dish.get();
                CookingRecipe recipe = CookingRecipeRegistry.byId(value.recipeId())
                        .orElseGet(() -> CookingRecipeRegistry.byId("dubious_dish").orElseThrow());
                return Optional.of(new CookingResult(
                        recipe,
                        value.rarity(),
                        Math.max(1, getReadyPortions()),
                        0,
                        value.dubious()
                ));
            }
            return Optional.empty();
        }
        List<ItemStack> inputs = new ArrayList<>();
        for (int slot = 0; slot < getAllowedSlots(); slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!stack.isEmpty()) {
                inputs.add(stack);
            }
        }
        return isSignaturePrepared()
                ? CookingRecipeRegistry.resolveSignature(inputs)
                : CookingRecipeRegistry.resolve(inputs);
    }

    public boolean canStartClient() {
        Optional<CookingResult> preview = getPreview();
        return !isProcessing()
                && !hasResult()
                && hasHeat()
                && preview.isPresent()
                && (!isSignaturePrepared() || !preview.get().dubious());
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)
                || !(inventory instanceof CookingPotBlockEntity cookingPot)) {
            return false;
        }
        if (id == 1) {
            if (SignatureDishManager.preparePot(serverPlayer, cookingPot)) {
                sendContentUpdates();
                return true;
            }
            return false;
        }
        if (id != 0 || !EatingPerks.canCook(serverPlayer)) {
            return false;
        }
        boolean started = cookingPot.isSignaturePrepared()
                ? SignatureDishManager.startPreparedCooking(serverPlayer, cookingPot)
                : cookingPot.startCooking(serverPlayer, EatingPerks.maxIngredients(serverPlayer));
        if (started) {
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

        if (slotIndex == CookingPotBlockEntity.RESULT_SLOT) {
            if (!(inventory instanceof CookingPotBlockEntity cookingPot)
                    || !hasBowlAvailable(player)
                    || !insertSingleResult(player, copy)) {
                return ItemStack.EMPTY;
            }
            consumeBowl(player);
            cookingPot.claimPortion(player);
            return copy.copyWithCount(1);
        }

        if (slotIndex < MACHINE_SLOT_COUNT) {
            if (!insertItem(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (CulinaryIngredientRegistry.isCulinaryIngredient(stack)) {
            if (!insertItem(stack, 0, getAllowedSlots(), false)) {
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

    private boolean insertSingleResult(PlayerEntity player, ItemStack source) {
        ItemStack one = source.copyWithCount(1);
        return insertItem(one, PLAYER_INVENTORY_START, HOTBAR_END, true) && one.isEmpty();
    }

    public static boolean hasBowlAvailable(PlayerEntity player) {
        if (player.getAbilities().creativeMode) {
            return true;
        }
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).isOf(Items.BOWL)) {
                return true;
            }
        }
        return false;
    }

    private static boolean consumeBowl(PlayerEntity player) {
        if (player.getAbilities().creativeMode) {
            return true;
        }
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(Items.BOWL)) {
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        inventory.onClose(player);
    }

    private void addPlayerInventorySlots(PlayerInventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    private final class IngredientSlot extends Slot {
        private IngredientSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return getIndex() < getAllowedSlots()
                    && !isProcessing()
                    && !hasResult()
                    && CulinaryIngredientRegistry.isCulinaryIngredient(stack);
        }

        @Override
        public int getMaxItemCount(ItemStack stack) {
            return 1;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return !isProcessing() && !hasResult();
        }
    }

    private final class ResultSlot extends Slot {
        private ResultSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return hasResult() && hasBowlAvailable(player);
        }

        @Override
        public void onTakeItem(PlayerEntity player, ItemStack stack) {
            if (inventory instanceof CookingPotBlockEntity cookingPot && consumeBowl(player)) {
                cookingPot.claimPortion(player);
            }
            super.onTakeItem(player, stack);
        }
    }
}
