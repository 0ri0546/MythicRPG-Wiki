package com.mythicrpg.woodcutting.chest;

import com.mythicrpg.woodcutting.ChestModuleItem;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * One live inventory authority for a single or double vanilla chest.
 *
 * <p>Each physical half owns its normal 27 slots, 27 persistent expansion
 * slots and exactly one module. Logical storage remains contiguous for screen
 * handlers, hoppers and comparator calculations.</p>
 */
public final class ModularChestInventory implements SidedInventory {

    public static final int BASE_SLOTS_PER_CHEST = 27;
    public static final int MAX_SLOTS_PER_CHEST = 54;
    public static final int MAX_CHESTS = 2;
    public static final int MAX_TOTAL_STORAGE = MAX_SLOTS_PER_CHEST * MAX_CHESTS;

    private static final int[][] AVAILABLE_SLOTS_BY_CAPACITY = createAvailableSlotCache();

    private final Inventory vanillaInventory;
    private final ChestBlockEntity[] chests;
    private final ChestModuleStorage[] moduleStorage;
    private RepackPlan pendingRepackPlan;

    public ModularChestInventory(Inventory vanillaInventory, ChestBlockEntity first) {
        this(vanillaInventory, new ChestBlockEntity[]{first});
    }

    public ModularChestInventory(
            Inventory vanillaInventory,
            ChestBlockEntity first,
            ChestBlockEntity second
    ) {
        this(vanillaInventory, new ChestBlockEntity[]{first, second});
    }

    private ModularChestInventory(Inventory vanillaInventory, ChestBlockEntity[] chests) {
        this.vanillaInventory = vanillaInventory;
        this.chests = chests;
        this.moduleStorage = new ChestModuleStorage[chests.length];

        for (int index = 0; index < chests.length; index++) {
            if (!(chests[index] instanceof ChestModuleStorage storage)) {
                throw new IllegalStateException("Vanilla chest is missing MythicRPG module storage");
            }
            moduleStorage[index] = storage;
        }
    }

    public int chestCount() {
        return chests.length;
    }

    /** Returns whether this live modular inventory contains the physical chest. */
    public boolean containsChest(ChestBlockEntity chest) {
        for (ChestBlockEntity candidate : chests) {
            if (candidate == chest) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getModule(int half) {
        if (!validHalf(half)) {
            return ItemStack.EMPTY;
        }
        return moduleStorage[half].mythicrpg$getModule();
    }

    public int extraSlots(int half) {
        return validHalf(half) ? ChestModuleItem.extraSlots(getModule(half)) : 0;
    }

    public int capacity(int half) {
        return validHalf(half) ? BASE_SLOTS_PER_CHEST + extraSlots(half) : 0;
    }

    /** Dynamic usable capacity. The Inventory size itself remains constant. */
    public int activeSize() {
        int total = 0;
        for (int half = 0; half < chestCount(); half++) {
            total += capacity(half);
        }
        return total;
    }

    @Override
    public int size() {
        return chestCount() * MAX_SLOTS_PER_CHEST;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return AVAILABLE_SLOTS_BY_CAPACITY[activeSize()];
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction direction) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction direction) {
        return address(slot) != null;
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < activeSize(); slot++) {
            if (!getStack(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        SlotAddress address = address(slot);
        return address == null ? ItemStack.EMPTY : getPhysicalStack(address.half(), address.physicalSlot());
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        SlotAddress address = address(slot);
        if (address == null || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack removed;
        if (address.physicalSlot() < BASE_SLOTS_PER_CHEST) {
            removed = chests[address.half()].removeStack(address.physicalSlot(), amount);
        } else {
            int extraIndex = address.physicalSlot() - BASE_SLOTS_PER_CHEST;
            removed = Inventories.splitStack(
                    moduleStorage[address.half()].mythicrpg$getExtraStacks(),
                    extraIndex,
                    amount
            );
            if (!removed.isEmpty()) {
                markHalfDirty(address.half());
            }
        }
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        SlotAddress address = address(slot);
        if (address == null) {
            return ItemStack.EMPTY;
        }

        ItemStack removed;
        if (address.physicalSlot() < BASE_SLOTS_PER_CHEST) {
            removed = chests[address.half()].removeStack(address.physicalSlot());
        } else {
            int extraIndex = address.physicalSlot() - BASE_SLOTS_PER_CHEST;
            removed = Inventories.removeStack(
                    moduleStorage[address.half()].mythicrpg$getExtraStacks(),
                    extraIndex
            );
            if (!removed.isEmpty()) {
                markHalfDirty(address.half());
            }
        }
        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        SlotAddress address = address(slot);
        if (address == null) {
            return;
        }

        ItemStack stored = stack;
        if (!stored.isEmpty()) {
            int maximum = Math.min(getMaxCountPerStack(), stored.getMaxCount());
            if (stored.getCount() > maximum) {
                stored.setCount(maximum);
            }
        }

        if (address.physicalSlot() < BASE_SLOTS_PER_CHEST) {
            chests[address.half()].setStack(address.physicalSlot(), stored);
        } else {
            int extraIndex = address.physicalSlot() - BASE_SLOTS_PER_CHEST;
            moduleStorage[address.half()].mythicrpg$getExtraStacks().set(extraIndex, stored);
            markHalfDirty(address.half());
        }
    }

    @Override
    public int getMaxCountPerStack() {
        return vanillaInventory.getMaxCountPerStack();
    }

    @Override
    public void markDirty() {
        for (int half = 0; half < chestCount(); half++) {
            markHalfDirty(half);
        }
    }

    /** Reproduces vanilla lock and loot-table preparation before opening. */
    public boolean prepareForPlayer(PlayerEntity player) {
        for (ChestBlockEntity chest : chests) {
            if (!chest.checkUnlocked(player)) {
                return false;
            }
        }
        for (ChestBlockEntity chest : chests) {
            chest.generateLoot(player);
        }
        return true;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return vanillaInventory.canPlayerUse(player);
    }

    @Override
    public void onOpen(PlayerEntity player) {
        vanillaInventory.onOpen(player);
        for (ChestBlockEntity chest : chests) {
            ChestModuleManager.registerViewer(chest, player);
        }
    }

    @Override
    public void onClose(PlayerEntity player) {
        for (ChestBlockEntity chest : chests) {
            ChestModuleManager.unregisterViewer(chest, player);
        }
        vanillaInventory.onClose(player);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return address(slot) != null;
    }

    @Override
    public void clear() {
        for (int half = 0; half < chestCount(); half++) {
            for (int physicalSlot = 0; physicalSlot < MAX_SLOTS_PER_CHEST; physicalSlot++) {
                setPhysicalStack(half, physicalSlot, ItemStack.EMPTY);
            }
            markHalfDirty(half);
        }
    }

    /** Fixed-size view used by the custom screen handler's 108 stable slots. */
    public Inventory screenView() {
        return new ScreenView(this);
    }

    /** Ephemeral active-size view used for vanilla comparator fullness. */
    public Inventory activeView() {
        return new ActiveView(this);
    }

    /** Returns whether the module change can be committed without losing an item. */
    public boolean canChangeModule(int half, ItemStack requestedModule) {
        if (!validHalf(half) || !validModuleOrEmpty(requestedModule)) {
            return false;
        }

        int oldExtra = extraSlots(half);
        int newExtra = ChestModuleItem.extraSlots(requestedModule);
        if (newExtra >= oldExtra) {
            pendingRepackPlan = null;
            return true;
        }
        RepackPlan plan = createRepackPlan(half, requestedModule, targetCapacities(half, newExtra));
        pendingRepackPlan = plan;
        return plan != null;
    }

    /**
     * Atomically installs, replaces or removes one physical half's module.
     * A shrinking operation first compacts compatible stacks in memory and is
     * only applied when every item fits the target capacity.
     */
    public boolean tryChangeModule(int half, ItemStack requestedModule) {
        if (!validHalf(half) || !validModuleOrEmpty(requestedModule)) {
            return false;
        }

        ItemStack normalized = normalizeModule(requestedModule);
        ItemStack oldModule = getModule(half);
        if (sameModule(oldModule, normalized)) {
            return true;
        }

        int oldExtra = ChestModuleItem.extraSlots(oldModule);
        int newExtra = ChestModuleItem.extraSlots(normalized);

        if (newExtra >= oldExtra) {
            moduleStorage[half].mythicrpg$setModuleDirect(normalized);
            markDirty();
            return true;
        }

        int[] targetCapacities = targetCapacities(half, newExtra);
        RepackPlan plan = pendingRepackPlan;
        pendingRepackPlan = null;
        if (plan == null
                || !plan.matchesRequest(half, normalized)
                || !plan.matchesCurrent(this)) {
            plan = createRepackPlan(half, normalized, targetCapacities);
        }
        if (plan == null || !plan.matchesCurrent(this)) {
            return false;
        }

        clearAllPhysicalStorage();
        moduleStorage[half].mythicrpg$setModuleDirect(normalized);

        int packedIndex = 0;
        for (int targetHalf = 0; targetHalf < chestCount(); targetHalf++) {
            for (int physicalSlot = 0;
                 physicalSlot < plan.targetCapacities()[targetHalf] && packedIndex < plan.packed().size();
                 physicalSlot++) {
                setPhysicalStack(targetHalf, physicalSlot, plan.packed().get(packedIndex++).copy());
            }
        }

        markDirty();
        return true;
    }

    private RepackPlan createRepackPlan(int half, ItemStack requestedModule, int[] targetCapacities) {
        List<ItemStack> packed = packForCapacities(targetCapacities);
        if (packed == null) {
            return null;
        }
        ArrayList<ItemStack> snapshot = new ArrayList<>(chestCount() * MAX_SLOTS_PER_CHEST);
        for (int targetHalf = 0; targetHalf < chestCount(); targetHalf++) {
            for (int physicalSlot = 0; physicalSlot < MAX_SLOTS_PER_CHEST; physicalSlot++) {
                snapshot.add(getPhysicalStack(targetHalf, physicalSlot).copy());
            }
        }
        ArrayList<ItemStack> moduleSnapshot = new ArrayList<>(chestCount());
        for (int targetHalf = 0; targetHalf < chestCount(); targetHalf++) {
            moduleSnapshot.add(getModule(targetHalf).copy());
        }
        return new RepackPlan(
                half,
                normalizeModule(requestedModule),
                targetCapacities.clone(),
                packed.stream().map(ItemStack::copy).toList(),
                snapshot,
                moduleSnapshot
        );
    }

    private List<ItemStack> packForCapacities(int[] targetCapacities) {
        int targetTotal = 0;
        for (int capacity : targetCapacities) {
            targetTotal += capacity;
        }

        List<ItemStack> packed = new ArrayList<>(targetTotal);
        for (int half = 0; half < chestCount(); half++) {
            for (int physicalSlot = 0; physicalSlot < MAX_SLOTS_PER_CHEST; physicalSlot++) {
                ItemStack source = getPhysicalStack(half, physicalSlot);
                if (source.isEmpty()) {
                    continue;
                }

                ItemStack remaining = source.copy();
                for (ItemStack target : packed) {
                    if (remaining.isEmpty()) {
                        break;
                    }
                    if (!ItemStack.areItemsAndComponentsEqual(target, remaining)) {
                        continue;
                    }
                    int maximum = Math.min(getMaxCountPerStack(), target.getMaxCount());
                    int room = maximum - target.getCount();
                    if (room <= 0) {
                        continue;
                    }
                    int moved = Math.min(room, remaining.getCount());
                    target.increment(moved);
                    remaining.decrement(moved);
                }

                while (!remaining.isEmpty()) {
                    if (packed.size() >= targetTotal) {
                        return null;
                    }
                    int maximum = Math.min(getMaxCountPerStack(), remaining.getMaxCount());
                    int moved = Math.min(maximum, remaining.getCount());
                    ItemStack newStack = remaining.copyWithCount(moved);
                    packed.add(newStack);
                    remaining.decrement(moved);
                }
            }
        }
        return packed;
    }

    private int[] targetCapacities(int changedHalf, int changedExtra) {
        int[] capacities = new int[chestCount()];
        for (int half = 0; half < chestCount(); half++) {
            capacities[half] = BASE_SLOTS_PER_CHEST
                    + (half == changedHalf ? changedExtra : extraSlots(half));
        }
        return capacities;
    }

    private void clearAllPhysicalStorage() {
        for (int half = 0; half < chestCount(); half++) {
            for (int physicalSlot = 0; physicalSlot < MAX_SLOTS_PER_CHEST; physicalSlot++) {
                setPhysicalStack(half, physicalSlot, ItemStack.EMPTY);
            }
        }
    }

    private ItemStack getPhysicalStack(int half, int physicalSlot) {
        if (physicalSlot < BASE_SLOTS_PER_CHEST) {
            return chests[half].getStack(physicalSlot);
        }
        return moduleStorage[half]
                .mythicrpg$getExtraStacks()
                .get(physicalSlot - BASE_SLOTS_PER_CHEST);
    }

    private void setPhysicalStack(int half, int physicalSlot, ItemStack stack) {
        if (physicalSlot < BASE_SLOTS_PER_CHEST) {
            chests[half].setStack(physicalSlot, stack);
        } else {
            moduleStorage[half]
                    .mythicrpg$getExtraStacks()
                    .set(physicalSlot - BASE_SLOTS_PER_CHEST, stack);
        }
    }

    private void markHalfDirty(int half) {
        moduleStorage[half].mythicrpg$markModuleStorageDirty();
    }

    private SlotAddress address(int logicalSlot) {
        if (logicalSlot < 0) {
            return null;
        }
        int remaining = logicalSlot;
        for (int half = 0; half < chestCount(); half++) {
            int capacity = capacity(half);
            if (remaining < capacity) {
                return new SlotAddress(half, remaining);
            }
            remaining -= capacity;
        }
        return null;
    }

    private boolean validHalf(int half) {
        return half >= 0 && half < chestCount();
    }

    private static boolean validModuleOrEmpty(ItemStack stack) {
        return stack.isEmpty() || ChestModuleItem.isModule(stack);
    }

    private static ItemStack normalizeModule(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(1);
    }

    private static boolean sameModule(ItemStack first, ItemStack second) {
        return first.isEmpty() && second.isEmpty()
                || !first.isEmpty()
                && !second.isEmpty()
                && ItemStack.areItemsAndComponentsEqual(first, second);
    }

    private static int[][] createAvailableSlotCache() {
        int[][] cache = new int[MAX_TOTAL_STORAGE + 1][];
        for (int capacity = 0; capacity <= MAX_TOTAL_STORAGE; capacity++) {
            int[] slots = new int[capacity];
            for (int slot = 0; slot < capacity; slot++) {
                slots[slot] = slot;
            }
            cache[capacity] = slots;
        }
        return cache;
    }

    private record RepackPlan(
            int half,
            ItemStack requestedModule,
            int[] targetCapacities,
            List<ItemStack> packed,
            List<ItemStack> snapshot,
            List<ItemStack> moduleSnapshot
    ) {
        private boolean matchesRequest(int requestedHalf, ItemStack requested) {
            return half == requestedHalf && sameModule(requestedModule, requested);
        }

        private boolean matchesCurrent(ModularChestInventory inventory) {
            for (int halfIndex = 0; halfIndex < inventory.chestCount(); halfIndex++) {
                if (!sameModule(inventory.getModule(halfIndex), moduleSnapshot.get(halfIndex))) {
                    return false;
                }
            }
            int index = 0;
            for (int halfIndex = 0; halfIndex < inventory.chestCount(); halfIndex++) {
                for (int physicalSlot = 0; physicalSlot < MAX_SLOTS_PER_CHEST; physicalSlot++) {
                    ItemStack current = inventory.getPhysicalStack(halfIndex, physicalSlot);
                    ItemStack expected = snapshot.get(index++);
                    if (current.getCount() != expected.getCount()
                            || !(current.isEmpty() && expected.isEmpty()
                            || !current.isEmpty() && !expected.isEmpty()
                            && ItemStack.areItemsAndComponentsEqual(current, expected))) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    private static final class ScreenView implements Inventory {
        private final ModularChestInventory delegate;

        private ScreenView(ModularChestInventory delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public ItemStack getStack(int slot) {
            return delegate.getStack(slot);
        }

        @Override
        public ItemStack removeStack(int slot, int amount) {
            return delegate.removeStack(slot, amount);
        }

        @Override
        public ItemStack removeStack(int slot) {
            return delegate.removeStack(slot);
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            delegate.setStack(slot, stack);
        }

        @Override
        public int getMaxCountPerStack() {
            return delegate.getMaxCountPerStack();
        }

        @Override
        public void markDirty() {
            delegate.markDirty();
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return delegate.canPlayerUse(player);
        }

        @Override
        public void onOpen(PlayerEntity player) {
            delegate.onOpen(player);
        }

        @Override
        public void onClose(PlayerEntity player) {
            delegate.onClose(player);
        }

        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return delegate.isValid(slot, stack);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    private static final class ActiveView implements Inventory {
        private final ModularChestInventory delegate;

        private ActiveView(ModularChestInventory delegate) {
            this.delegate = delegate;
        }

        @Override
        public int size() {
            return delegate.activeSize();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public ItemStack getStack(int slot) {
            return delegate.getStack(slot);
        }

        @Override
        public ItemStack removeStack(int slot, int amount) {
            return delegate.removeStack(slot, amount);
        }

        @Override
        public ItemStack removeStack(int slot) {
            return delegate.removeStack(slot);
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            delegate.setStack(slot, stack);
        }

        @Override
        public int getMaxCountPerStack() {
            return delegate.getMaxCountPerStack();
        }

        @Override
        public void markDirty() {
            delegate.markDirty();
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return delegate.canPlayerUse(player);
        }

        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return delegate.isValid(slot, stack);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    private record SlotAddress(int half, int physicalSlot) {
    }
}
