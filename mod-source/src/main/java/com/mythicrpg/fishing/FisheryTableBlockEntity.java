package com.mythicrpg.fishing;

import com.mythicrpg.core.ModBlockEntities;
import com.mythicrpg.core.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

/** Fish material extraction is an explicit, server-authoritative transaction. */
public final class FisheryTableBlockEntity extends LockableContainerBlockEntity {
    public static final int INVENTORY_SIZE = 2;
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);

    public FisheryTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FISHERY_TABLE, pos, state);
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("container.mythicrpg.fishery_table");
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
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory inventory) {
        return new FisheryTableScreenHandler(syncId, inventory, this);
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return world != null
                && world.getBlockEntity(pos) == this
                && player.squaredDistanceTo(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot == INPUT_SLOT && FishingCatchData.read(stack).isPresent();
    }

    public ItemStack previewOutput() {
        FishingCatchData.Catch caught = FishingCatchData.read(items.get(INPUT_SLOT)).orElse(null);
        if (caught == null) return ItemStack.EMPTY;
        return new ItemStack(ModItems.fishingMaterial(
                caught.rarity(),
                caught.family() == FishingFamily.CRUSTACEAN
        ));
    }

    public boolean canTransform() {
        ItemStack preview = previewOutput();
        if (preview.isEmpty()) return false;
        ItemStack output = items.get(OUTPUT_SLOT);
        return output.isEmpty()
                || (ItemStack.areItemsAndComponentsEqual(output, preview)
                && output.getCount() < output.getMaxCount());
    }

    public boolean transformOne() {
        if (!canTransform()) return false;
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack preview = previewOutput();
        ItemStack output = items.get(OUTPUT_SLOT);

        input.decrement(1);
        if (input.isEmpty()) items.set(INPUT_SLOT, ItemStack.EMPTY);
        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, preview);
        } else {
            output.increment(1);
        }
        super.markDirty();
        return true;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.writeNbt(nbt, lookup);
        Inventories.writeNbt(nbt, items, lookup);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        super.readNbt(nbt, lookup);
        items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items, lookup);
    }
}
