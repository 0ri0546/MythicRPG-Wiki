package com.mythicrpg.building;

import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.UUID;

/** A non-ticking, owner-bound 27-slot inventory dedicated to Building blocks. */
public final class BuildingReserveChestBlockEntity extends LockableContainerBlockEntity implements SidedInventory {
    public static final int INVENTORY_SIZE = 27;
    private static final int[] ALL_SLOTS = createSlots();

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private UUID owner;

    public BuildingReserveChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUILDING_RESERVE_CHEST, pos, state);
    }

    public UUID owner() {
        return owner;
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public boolean isOwner(PlayerEntity player) {
        return owner != null && owner.equals(player.getUuid());
    }

    public void setOwner(UUID owner) {
        if (owner != null && !owner.equals(this.owner)) {
            this.owner = owner;
            markDirty();
        }
    }

    public boolean containsMatching(ItemStack template) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty() && ItemStack.areItemsAndComponentsEqual(stack, template)) {
                return true;
            }
        }
        return false;
    }

    /** Inserts at most one full stack into the player's normal inventory and returns the moved count. */
    public int transferOneStackTo(PlayerEntity player, ItemStack template) {
        int remaining = template.getMaxCount();
        int moved = 0;

        for (int slot = 0; slot < items.size() && remaining > 0; slot++) {
            ItemStack source = items.get(slot);
            if (source.isEmpty() || !ItemStack.areItemsAndComponentsEqual(source, template)) {
                continue;
            }

            int requested = Math.min(remaining, source.getCount());
            ItemStack transfer = source.copyWithCount(requested);
            player.getInventory().insertStack(transfer);
            int inserted = requested - transfer.getCount();

            if (inserted <= 0) {
                break;
            }

            source.decrement(inserted);
            remaining -= inserted;
            moved += inserted;
        }

        if (moved > 0) {
            markDirty();
            if (world != null) {
                world.updateComparators(pos, getCachedState().getBlock());
            }
        }
        return moved;
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.mythicrpg.building_reserve_chest");
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return items;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> stacks) {
        items = stacks;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, this);
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this || !isOwner(player)) {
            return false;
        }
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && BuildingBlockCatalog.isEligible(blockItem.getBlock());
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return ALL_SLOTS.clone();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return isValid(slot, stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items, registryLookup);
        owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, items, registryLookup);
        if (owner != null) {
            nbt.putUuid("Owner", owner);
        }
    }

    private static int[] createSlots() {
        int[] slots = new int[INVENTORY_SIZE];
        for (int index = 0; index < slots.length; index++) {
            slots[index] = index;
        }
        return slots;
    }
}
