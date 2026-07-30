package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight cache for the frozen Traveling V1 tree.
 *
 * <p>Skill unlock lists are replaced, rather than mutated in place, whenever a
 * node is bought or the tree is reset. Keeping the list identity therefore lets
 * hot runtime paths use a compact bit mask without repeatedly walking all
 * unlocked nodes.</p>
 */
public final class TravelingBonusCache {
    private static final Map<UUID, Snapshot> CACHE = new HashMap<>();

    private TravelingBonusCache() {
    }

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                clearPlayer(handler.player.getUuid())
        );
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                clearPlayer(newPlayer.getUuid())
        );
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CACHE.clear());
    }

    public static boolean hasBonus(ServerPlayerEntity player, BonusType bonusType) {
        int nodeId = switch (bonusType) {
            case TRAVEL_DOUBLE_JUMP -> 1;
            case TRAVEL_SOUL_WALKER -> 2;
            case TRAVEL_DOLPHINS_GRACE -> 3;
            case TRAVEL_POWDER_WALKER -> 4;
            case TRAVEL_MINIATURIZATION -> 8;
            case MONUMENTAL_COMPASS_CRAFT -> 9;
            case STRUCTURE_MODULES_OVERWORLD -> 10;
            case STRUCTURE_MODULES_NETHER_END -> 11;
            case TRAVEL_DEATH_RECALL -> 12;
            case TRAVEL_BOOTS_NO_DURABILITY -> 13;
            case TRAVEL_BIOME_SPEED -> 14;
            case FAST_MINECART_CRAFT -> 15;
            case FAST_BOAT_CRAFT -> 16;
            case LAND_MOUNTS -> 17;
            case TREASURE_VANILLA_XP -> 18;
            case FLYING_MOUNTS -> 19;
            case GRAPPLING_HOOK_CRAFT -> 20;
            default -> -1;
        };

        return nodeId > 0 && isNodeUnlocked(player, nodeId);
    }

    public static double getXpMultiplier(ServerPlayerEntity player) {
        int mask = snapshot(player).unlockedMask;
        double multiplier = 0.0D;

        if (contains(mask, 5)) {
            multiplier += 0.10D;
        }
        if (contains(mask, 6)) {
            multiplier += 0.15D;
        }

        return multiplier;
    }

    public static double getDiscoveryXpMultiplier(ServerPlayerEntity player) {
        return isNodeUnlocked(player, 7) ? 0.25D : 0.0D;
    }

    public static boolean isNodeUnlocked(ServerPlayerEntity player, int nodeId) {
        return nodeId >= 1 && nodeId <= 20
                && contains(snapshot(player).unlockedMask, nodeId);
    }

    public static void clearPlayer(UUID playerUuid) {
        CACHE.remove(playerUuid);
    }

    private static Snapshot snapshot(ServerPlayerEntity player) {
        List<Integer> unlocks = ModAttachments.getUnlocks(player, SkillType.TRAVELING);
        UUID playerUuid = player.getUuid();
        Snapshot cached = CACHE.get(playerUuid);

        if (cached != null && cached.unlocks == unlocks) {
            return cached;
        }

        int mask = 0;
        for (int nodeId : unlocks) {
            if (nodeId >= 1 && nodeId <= 20) {
                mask |= 1 << (nodeId - 1);
            }
        }

        Snapshot rebuilt = new Snapshot(unlocks, mask);
        CACHE.put(playerUuid, rebuilt);
        return rebuilt;
    }

    private static boolean contains(int mask, int nodeId) {
        return (mask & (1 << (nodeId - 1))) != 0;
    }

    private static final class Snapshot {
        private final List<Integer> unlocks;
        private final int unlockedMask;

        private Snapshot(List<Integer> unlocks, int unlockedMask) {
            this.unlocks = unlocks;
            this.unlockedMask = unlockedMask;
        }
    }
}
