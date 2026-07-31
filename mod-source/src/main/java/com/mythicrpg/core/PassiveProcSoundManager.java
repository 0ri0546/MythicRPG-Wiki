package com.mythicrpg.core;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PassiveProcSoundManager {

    private static final Map<String, Long> LAST_PROC_SOUND_TICK = new HashMap<>();

    private PassiveProcSoundManager() {
    }

    public static void playForPlayer(
            ServerPlayerEntity player,
            String procId,
            SoundEvent sound,
            float volume,
            float pitch,
            int cooldownTicks
    ) {
        long now = player.getWorld().getTime();
        String key = player.getUuidAsString() + ":" + procId;

        long lastTick = LAST_PROC_SOUND_TICK.getOrDefault(key, -999999L);

        if (now - lastTick < cooldownTicks) {
            return;
        }

        LAST_PROC_SOUND_TICK.put(key, now);

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                sound,
                SoundCategory.PLAYERS,
                volume,
                pitch
        );
    }

    public static void clearPlayer(UUID playerUuid) {
        String prefix = playerUuid.toString() + ":";
        LAST_PROC_SOUND_TICK.keySet().removeIf(key -> key.startsWith(prefix));
    }

    public static void cleanupOldEntries(long currentTick, long maxAgeTicks) {
        LAST_PROC_SOUND_TICK.entrySet().removeIf(entry -> currentTick - entry.getValue() > maxAgeTicks);
    }
}