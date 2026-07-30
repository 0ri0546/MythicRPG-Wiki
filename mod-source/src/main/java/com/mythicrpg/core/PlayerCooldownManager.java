package com.mythicrpg.core;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerCooldownManager {
    private static final Map<String, Long> LAST_USE_TICK = new HashMap<>();

    private PlayerCooldownManager() {
    }

    public static boolean tryUse(ServerPlayerEntity player, String cooldownId, int cooldownTicks) {
        long now = player.getWorld().getTime();
        String key = player.getUuidAsString() + ":" + cooldownId;

        long lastTick = LAST_USE_TICK.getOrDefault(key, -999999L);

        if (now - lastTick < cooldownTicks) {
            return false;
        }

        LAST_USE_TICK.put(key, now);
        return true;
    }

    public static boolean isOnCooldown(ServerPlayerEntity player, String cooldownId, int cooldownTicks) {
        long now = player.getWorld().getTime();
        String key = player.getUuidAsString() + ":" + cooldownId;

        long lastTick = LAST_USE_TICK.getOrDefault(key, -999999L);
        return now - lastTick < cooldownTicks;
    }

    public static void clearPlayer(UUID playerUuid) {
        String prefix = playerUuid.toString() + ":";
        LAST_USE_TICK.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public static void cleanupOldEntries(long currentTick, long maxAgeTicks) {
        LAST_USE_TICK.entrySet().removeIf(entry -> currentTick - entry.getValue() > maxAgeTicks);
    }
}
