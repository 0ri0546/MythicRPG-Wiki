package com.mythicrpg.woodcutting.chest;

import com.mythicrpg.mixin.DoubleInventoryAccessor;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Resolves vanilla single/double chest inventories into the expanded live view. */
public final class ChestModuleManager {

    /** Server-thread-only viewer registry; weak keys cannot retain unloaded chests. */
    private static final Map<ChestBlockEntity, Set<UUID>> MODULAR_VIEWERS = new WeakHashMap<>();

    private ChestModuleManager() {
    }

    /** Returns the expanded view only when at least one physical chest has an active module. */
    @Nullable
    public static Inventory wrapIfActive(@Nullable Inventory inventory) {
        if (!hasActiveModule(inventory)) {
            return inventory;
        }
        return wrap(inventory);
    }

    public static boolean hasActiveModule(@Nullable Inventory inventory) {
        if (inventory instanceof ModularChestInventory modular) {
            for (int half = 0; half < modular.chestCount(); half++) {
                if (!modular.getModule(half).isEmpty()) {
                    return true;
                }
            }
            return false;
        }
        if (inventory instanceof ChestBlockEntity chest && isSupportedChest(chest)) {
            return chest instanceof ChestModuleStorage storage
                    && !storage.mythicrpg$getModule().isEmpty();
        }
        if (inventory instanceof DoubleInventory doubleInventory) {
            DoubleInventoryAccessor accessor = (DoubleInventoryAccessor) doubleInventory;
            Inventory first = accessor.mythicrpg$getFirst();
            Inventory second = accessor.mythicrpg$getSecond();
            return hasActiveModule(first) || hasActiveModule(second);
        }
        return false;
    }

    @Nullable
    public static Inventory wrap(@Nullable Inventory inventory) {
        if (inventory == null || inventory instanceof ModularChestInventory) {
            return inventory;
        }

        if (inventory instanceof ChestBlockEntity chest && isSupportedChest(chest)) {
            return new ModularChestInventory(inventory, chest);
        }

        if (inventory instanceof DoubleInventory doubleInventory) {
            DoubleInventoryAccessor accessor = (DoubleInventoryAccessor) doubleInventory;
            Inventory first = accessor.mythicrpg$getFirst();
            Inventory second = accessor.mythicrpg$getSecond();
            if (first instanceof ChestBlockEntity firstChest
                    && second instanceof ChestBlockEntity secondChest
                    && isSupportedChest(firstChest)
                    && isSupportedChest(secondChest)) {
                return new ModularChestInventory(inventory, firstChest, secondChest);
            }
        }

        return inventory;
    }

    private static boolean isSupportedChest(ChestBlockEntity chest) {
        return chest.getCachedState().isOf(Blocks.CHEST)
                || chest.getCachedState().isOf(Blocks.TRAPPED_CHEST);
    }

    /** Registers one server-side custom chest viewer. */
    public static void registerViewer(ChestBlockEntity chest, PlayerEntity player) {
        if (player.isSpectator()
                || chest.getWorld() == null
                || chest.getWorld().isClient) {
            return;
        }
        MODULAR_VIEWERS
                .computeIfAbsent(chest, ignored -> new HashSet<>())
                .add(player.getUuid());
    }

    /** Removes one server-side custom chest viewer. */
    public static void unregisterViewer(ChestBlockEntity chest, PlayerEntity player) {
        Set<UUID> viewers = MODULAR_VIEWERS.get(chest);
        if (viewers == null) {
            return;
        }
        viewers.remove(player.getUuid());
        if (viewers.isEmpty()) {
            MODULAR_VIEWERS.remove(chest);
        }
    }

    /**
     * Vanilla's chest viewer recount only recognizes GenericContainerScreenHandler.
     * Validate the small per-chest custom viewer set and keep the normal lid count
     * alive only while a matching modular handler is genuinely open.
     */
    public static boolean hasActiveModularViewer(ChestBlockEntity chest) {
        if (chest.getWorld() == null || chest.getWorld().isClient) {
            return false;
        }

        Set<UUID> viewers = MODULAR_VIEWERS.get(chest);
        if (viewers == null || viewers.isEmpty()) {
            return false;
        }

        boolean active = false;
        Iterator<UUID> iterator = viewers.iterator();
        while (iterator.hasNext()) {
            PlayerEntity player = chest.getWorld().getPlayerByUuid(iterator.next());
            if (player == null
                    || player.isSpectator()
                    || !(player.currentScreenHandler instanceof ModularChestScreenHandler handler)
                    || !handler.isViewing(chest)) {
                iterator.remove();
                continue;
            }
            active = true;
        }

        if (viewers.isEmpty()) {
            MODULAR_VIEWERS.remove(chest);
        }
        return active;
    }

    @Nullable
    public static ModularChestInventory modular(@Nullable Inventory inventory) {
        Inventory wrapped = wrap(inventory);
        return wrapped instanceof ModularChestInventory modular ? modular : null;
    }
}
