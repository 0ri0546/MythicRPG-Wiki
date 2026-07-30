package com.mythicrpg.farming;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Keeps one authoritative live inventory for a Food Backpack while its screen is open. */
public final class FoodBackpackSessionManager {
    public interface FlushableSessionInventory extends Inventory {
        void mythicrpg$flushIfDirty();
        void mythicrpg$flushNow();
    }
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private FoodBackpackSessionManager() {
    }

    public static void open(ServerPlayerEntity player, ItemStack backpack, Inventory inventory) {
        String backpackId = FoodBackpackDeathData.getBackpackId(backpack);
        if (backpackId.isBlank()) {
            return;
        }
        SESSIONS.put(player.getUuid(), new Session(backpackId, inventory));
    }

    public static void close(ServerPlayerEntity player, Inventory inventory) {
        Session session = SESSIONS.get(player.getUuid());
        if (session != null && session.inventory() == inventory) {
            SESSIONS.remove(player.getUuid());
        }
    }

    public static Optional<Inventory> inventory(ServerPlayerEntity player, ItemStack backpack) {
        Session session = SESSIONS.get(player.getUuid());
        if (session == null) {
            return Optional.empty();
        }
        String backpackId = FoodBackpackDeathData.getBackpackId(backpack);
        return !backpackId.isBlank() && backpackId.equals(session.backpackId())
                ? Optional.of(session.inventory())
                : Optional.empty();
    }

    public static boolean isOpen(ServerPlayerEntity player, ItemStack backpack) {
        return inventory(player, backpack).isPresent();
    }

    public static void tick() {
        for (Session session : SESSIONS.values()) {
            if (session.inventory() instanceof FlushableSessionInventory flushable) {
                flushable.mythicrpg$flushIfDirty();
            }
        }
    }

    public static void clear(ServerPlayerEntity player) {
        Session session = SESSIONS.remove(player.getUuid());
        if (session != null && session.inventory() instanceof FlushableSessionInventory flushable) {
            flushable.mythicrpg$flushNow();
        }
    }

    public static void clearAll() {
        for (Session session : SESSIONS.values()) {
            if (session.inventory() instanceof FlushableSessionInventory flushable) {
                flushable.mythicrpg$flushNow();
            }
        }
        SESSIONS.clear();
    }

    private record Session(String backpackId, Inventory inventory) {
    }
}
