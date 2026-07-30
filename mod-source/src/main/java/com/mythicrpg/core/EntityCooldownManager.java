package com.mythicrpg.core;

import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class EntityCooldownManager {
    private static final long NEVER_USED_TICK = -999999L;
    private static final Map<UUID, Map<String, Long>> LAST_USE_TICKS_BY_ENTITY = new HashMap<>();

    private EntityCooldownManager() {
    }

    public static boolean tryUse(Entity entity, String cooldownId, int cooldownTicks) {
        long now = entity.getWorld().getTime();
        Map<String, Long> cooldowns = LAST_USE_TICKS_BY_ENTITY.computeIfAbsent(
                entity.getUuid(),
                ignored -> new HashMap<>()
        );
        long lastTick = cooldowns.getOrDefault(cooldownId, NEVER_USED_TICK);

        if (now - lastTick < cooldownTicks) {
            return false;
        }

        cooldowns.put(cooldownId, now);
        return true;
    }

    public static boolean isOnCooldown(Entity entity, String cooldownId, int cooldownTicks) {
        long now = entity.getWorld().getTime();
        return now - getLastTick(entity, cooldownId) < cooldownTicks;
    }

    public static long getLastTick(Entity entity, String cooldownId) {
        Map<String, Long> cooldowns = LAST_USE_TICKS_BY_ENTITY.get(entity.getUuid());

        if (cooldowns == null) {
            return NEVER_USED_TICK;
        }

        return cooldowns.getOrDefault(cooldownId, NEVER_USED_TICK);
    }

    public static void markUsed(Entity entity, String cooldownId) {
        markUsed(entity, cooldownId, entity.getWorld().getTime());
    }

    public static void markUsed(Entity entity, String cooldownId, long tick) {
        LAST_USE_TICKS_BY_ENTITY
                .computeIfAbsent(entity.getUuid(), ignored -> new HashMap<>())
                .put(cooldownId, tick);
    }

    public static void clearEntity(UUID entityUuid) {
        LAST_USE_TICKS_BY_ENTITY.remove(entityUuid);
    }

    public static void clearAll() {
        LAST_USE_TICKS_BY_ENTITY.clear();
    }

    public static void cleanupOldEntries(long currentTick, long maxAgeTicks) {
        Iterator<Map.Entry<UUID, Map<String, Long>>> entities =
                LAST_USE_TICKS_BY_ENTITY.entrySet().iterator();

        while (entities.hasNext()) {
            Map<String, Long> cooldowns = entities.next().getValue();
            cooldowns.entrySet().removeIf(entry -> currentTick - entry.getValue() > maxAgeTicks);

            if (cooldowns.isEmpty()) {
                entities.remove();
            }
        }
    }
}
