package com.mythicrpg.eating;

import com.mythicrpg.core.ItemContainerUtils;
import com.mythicrpg.core.ModEnchantments;
import com.mythicrpg.core.ModItems;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;

public final class EatingPreservationManager {
    public enum PreservationMode {
        NONE,
        PORTABLE,
        PORTABLE_CONTINUOUS,
        FRIDGE
    }

    private EatingPreservationManager() {
    }

    public static boolean hasPortableFridge(ServerPlayerEntity player) {
        if (!EatingPerks.hasPortableFridgePerk(player)) {
            return false;
        }
        return getPortableFridgeLevel(player, player.getEquippedStack(EquipmentSlot.CHEST)) > 0;
    }

    public static PreservationMode modeForPlayer(ServerPlayerEntity player) {
        return hasPortableFridge(player) ? PreservationMode.PORTABLE : PreservationMode.NONE;
    }

    public static int getPortableFridgeLevel(ServerPlayerEntity player, ItemStack stack) {
        return player.getServerWorld()
                .getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(ModEnchantments.PORTABLE_FRIDGE)
                .map(entry -> EnchantmentHelper.getLevel(entry, stack))
                .orElse(0);
    }

    public static boolean updateStack(ItemStack stack, long gameTime, PreservationMode mode) {
        if (stack.isEmpty()) {
            return false;
        }
        if (PreparedDishData.read(stack).isPresent()) {
            return PreparedDishData.updatePreservation(stack, gameTime, mode);
        }
        if (stack.getItem() instanceof ServingPlateItem) {
            return ServingPlateData.updatePreservation(stack, gameTime, mode);
        }
        return false;
    }

    public static boolean updateDroppedStorage(ItemStack stack, long gameTime) {
        if (stack.isOf(ModItems.FOOD_BACKPACK)) {
            return updateContainer(stack, 54, gameTime, PreservationMode.NONE);
        }
        return updateStack(stack, gameTime, PreservationMode.NONE);
    }

    public static boolean refreshPlayerStorage(ServerPlayerEntity player) {
        PreservationMode mode = modeForPlayer(player);
        boolean changed = false;
        long gameTime = player.getWorld().getTime();

        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            ItemStack stack = player.getInventory().getStack(slot);
            if (stack.isOf(ModItems.FOOD_BACKPACK)) {
                changed |= updateContainer(stack, 54, gameTime, mode);
            } else {
                changed |= updateStack(stack, gameTime, mode);
            }
        }
        return changed;
    }

    public static boolean updateContainer(
            ItemStack container,
            int size,
            long gameTime,
            PreservationMode mode
    ) {
        DefaultedList<ItemStack> contents = ItemContainerUtils.read(container, size);
        boolean changed = false;
        for (ItemStack stack : contents) {
            changed |= updateStack(stack, gameTime, mode);
        }
        if (changed) {
            ItemContainerUtils.write(container, contents);
        }
        return changed;
    }
}
