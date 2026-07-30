package com.mythicrpg.eating;

import com.mythicrpg.core.ItemContainerUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

import java.util.Optional;

public final class ServingPlateData {
    private static final String ROOT = "mythicrpg_serving_plate";
    private static final String SELECTED = "selected";

    private ServingPlateData() {
    }

    public static int capacity(ItemStack plate) {
        return plate.getItem() instanceof ServingPlateItem item ? item.capacity() : 0;
    }

    public static int count(ItemStack plate) {
        int count = 0;
        for (ItemStack stack : contents(plate)) {
            if (!stack.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public static boolean hasSpace(ItemStack plate) {
        return count(plate) < capacity(plate);
    }

    public static boolean addPortion(ItemStack plate, ItemStack portion) {
        int capacity = capacity(plate);
        if (capacity <= 0 || PreparedDishData.read(portion).isEmpty()) {
            return false;
        }

        DefaultedList<ItemStack> contents = ItemContainerUtils.read(plate, capacity);
        for (int index = 0; index < contents.size(); index++) {
            if (!contents.get(index).isEmpty()) {
                continue;
            }
            contents.set(index, portion.copyWithCount(1));
            ItemContainerUtils.write(plate, contents);
            if (count(plate) == 1) {
                setSelectedIndex(plate, index);
            }
            return true;
        }
        return false;
    }

    public static Optional<ItemStack> selectedPortion(ItemStack plate) {
        DefaultedList<ItemStack> contents = contents(plate);
        int selected = normalizeSelectedIndex(plate, contents);
        if (selected < 0) {
            return Optional.empty();
        }
        return Optional.of(contents.get(selected));
    }

    public static Optional<ItemStack> removeSelectedPortion(ItemStack plate) {
        int capacity = capacity(plate);
        if (capacity <= 0) {
            return Optional.empty();
        }

        DefaultedList<ItemStack> contents = ItemContainerUtils.read(plate, capacity);
        int selected = normalizeSelectedIndex(plate, contents);
        if (selected < 0) {
            return Optional.empty();
        }

        ItemStack removed = contents.get(selected).copyWithCount(1);
        contents.set(selected, ItemStack.EMPTY);
        ItemContainerUtils.write(plate, contents);
        normalizeSelectedIndex(plate, contents);
        return Optional.of(removed);
    }

    public static int cycle(ItemStack plate) {
        DefaultedList<ItemStack> contents = contents(plate);
        if (contents.isEmpty()) {
            return -1;
        }

        int current = normalizeSelectedIndex(plate, contents);
        if (current < 0) {
            return -1;
        }

        for (int offset = 1; offset <= contents.size(); offset++) {
            int candidate = (current + offset) % contents.size();
            if (!contents.get(candidate).isEmpty()) {
                setSelectedIndex(plate, candidate);
                return candidate;
            }
        }
        return current;
    }

    public static int selectedIndex(ItemStack plate) {
        DefaultedList<ItemStack> contents = contents(plate);
        int selected = readSelectedIndex(plate);
        if (selected >= 0 && selected < contents.size() && !contents.get(selected).isEmpty()) {
            return selected;
        }
        for (int index = 0; index < contents.size(); index++) {
            if (!contents.get(index).isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    public static DefaultedList<ItemStack> contents(ItemStack plate) {
        int capacity = capacity(plate);
        return capacity <= 0
                ? DefaultedList.ofSize(0, ItemStack.EMPTY)
                : ItemContainerUtils.read(plate, capacity);
    }

    public static boolean updatePreservation(
            ItemStack plate,
            long gameTime,
            EatingPreservationManager.PreservationMode mode
    ) {
        int capacity = capacity(plate);
        if (capacity <= 0) {
            return false;
        }

        DefaultedList<ItemStack> contents = ItemContainerUtils.read(plate, capacity);
        boolean changed = false;
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                changed |= PreparedDishData.updatePreservation(stack, gameTime, mode);
            }
        }
        if (changed) {
            ItemContainerUtils.write(plate, contents);
        }
        return changed;
    }

    private static int normalizeSelectedIndex(ItemStack plate, DefaultedList<ItemStack> contents) {
        if (contents.isEmpty()) {
            return -1;
        }

        int selected = readSelectedIndex(plate);
        if (selected >= 0 && selected < contents.size() && !contents.get(selected).isEmpty()) {
            return selected;
        }

        for (int index = 0; index < contents.size(); index++) {
            if (!contents.get(index).isEmpty()) {
                setSelectedIndex(plate, index);
                return index;
            }
        }
        setSelectedIndex(plate, 0);
        return -1;
    }

    private static int readSelectedIndex(ItemStack plate) {
        NbtCompound custom = plate.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();
        if (!custom.contains(ROOT)) {
            return 0;
        }
        return custom.getCompound(ROOT).getInt(SELECTED);
    }

    private static void setSelectedIndex(ItemStack plate, int index) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, plate, custom -> {
            NbtCompound data = custom.contains(ROOT)
                    ? custom.getCompound(ROOT).copy()
                    : new NbtCompound();
            data.putInt(SELECTED, Math.max(0, index));
            custom.put(ROOT, data);
        });
    }
}
