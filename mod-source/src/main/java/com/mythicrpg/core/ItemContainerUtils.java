package com.mythicrpg.core;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.function.Predicate;

public final class ItemContainerUtils {
    private ItemContainerUtils() {
    }

    public static DefaultedList<ItemStack> read(ItemStack containerStack, int size) {
        DefaultedList<ItemStack> contents = DefaultedList.ofSize(size, ItemStack.EMPTY);

        ContainerComponent component = containerStack.getOrDefault(
                DataComponentTypes.CONTAINER,
                ContainerComponent.DEFAULT
        );

        component.copyTo(contents);
        return contents;
    }

    public static void write(ItemStack containerStack, DefaultedList<ItemStack> contents) {
        containerStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(contents));
    }

    public static int countItems(ItemStack containerStack, int size) {
        int total = 0;

        for (ItemStack stack : read(containerStack, size)) {
            if (!stack.isEmpty()) {
                total += stack.getCount();
            }
        }

        return total;
    }

    public static int countUsedSlots(ItemStack containerStack, int size) {
        int usedSlots = 0;

        for (ItemStack stack : read(containerStack, size)) {
            if (!stack.isEmpty()) {
                usedSlots++;
            }
        }

        return usedSlots;
    }

    public static int countMatching(ItemStack containerStack, int size, Predicate<ItemStack> predicate) {
        int total = 0;

        for (ItemStack stack : read(containerStack, size)) {
            if (predicate.test(stack)) {
                total += stack.getCount();
            }
        }

        return total;
    }

    public static int removeMatching(
            ItemStack containerStack,
            int size,
            Predicate<ItemStack> predicate,
            int amount
    ) {
        if (amount <= 0) {
            return 0;
        }

        DefaultedList<ItemStack> contents = read(containerStack, size);
        int remaining = amount;
        boolean changed = false;

        for (ItemStack stack : contents) {
            if (remaining <= 0) {
                break;
            }

            if (!predicate.test(stack)) {
                continue;
            }

            int removed = Math.min(stack.getCount(), remaining);
            stack.decrement(removed);
            remaining -= removed;
            changed = true;
        }

        if (changed) {
            write(containerStack, contents);
        }

        return amount - remaining;
    }

    public static boolean insert(ItemStack containerStack, int size, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return false;
        }

        DefaultedList<ItemStack> contents = read(containerStack, size);
        boolean changed = false;

        for (int i = 0; i < contents.size(); i++) {
            if (incoming.isEmpty()) {
                break;
            }

            ItemStack existing = contents.get(i);

            if (existing.isEmpty()) {
                continue;
            }

            if (!ItemStack.areItemsAndComponentsEqual(existing, incoming)) {
                continue;
            }

            int space = existing.getMaxCount() - existing.getCount();

            if (space <= 0) {
                continue;
            }

            int moved = Math.min(space, incoming.getCount());
            existing.increment(moved);
            incoming.decrement(moved);
            changed = true;
        }

        for (int i = 0; i < contents.size(); i++) {
            if (incoming.isEmpty()) {
                break;
            }

            ItemStack existing = contents.get(i);

            if (!existing.isEmpty()) {
                continue;
            }

            int moved = Math.min(incoming.getMaxCount(), incoming.getCount());
            ItemStack stored = incoming.copy();
            stored.setCount(moved);

            contents.set(i, stored);
            incoming.decrement(moved);
            changed = true;
        }

        if (changed) {
            write(containerStack, contents);
        }

        return changed;
    }
}
