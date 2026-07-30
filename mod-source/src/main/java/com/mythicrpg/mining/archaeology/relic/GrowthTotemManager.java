package com.mythicrpg.mining.archaeology.relic;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Tracks loaded Growth Totems so overlapping fields use one deterministic winner:
 * highest level first, then the lowest packed block position as a stable tie-breaker.
 *
 * <p>Totems are indexed by their own chunk. A target lookup therefore examines at
 * most the chunks intersecting the maximum twelve-block radius instead of walking
 * every loaded Totem in the dimension.</p>
 */
public final class GrowthTotemManager {

    private static final long ACTIVE_TIMEOUT_TICKS = 45L;
    private static final int MAX_RADIUS = 12;
    private static final WeakHashMap<ServerWorld, WorldIndex> ACTIVE = new WeakHashMap<>();

    private GrowthTotemManager() {
    }

    public static int radiusForLevel(int level) {
        return switch (Math.max(1, Math.min(5, level))) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            case 4 -> 9;
            default -> MAX_RADIUS;
        };
    }

    /** Called once per second by each loaded Totem, not once per server tick. */
    public static void touch(ServerWorld world, BlockPos pos, int level) {
        WorldIndex index = ACTIVE.computeIfAbsent(world, ignored -> new WorldIndex());
        long now = world.getTime();
        long packedPos = pos.asLong();
        int safeLevel = Math.max(1, Math.min(5, level));
        ActiveTotem existing = index.byPosition.get(packedPos);

        if (existing == null) {
            ActiveTotem created = new ActiveTotem(pos.toImmutable(), safeLevel, now);
            index.byPosition.put(packedPos, created);
            index.positionsByChunk
                    .computeIfAbsent(chunkKey(pos), ignored -> new HashSet<>())
                    .add(packedPos);
        } else {
            existing.level = safeLevel;
            existing.lastSeenAt = now;
        }

        if (Math.floorMod(now, 100L) == Math.floorMod(packedPos, 100L)) {
            pruneStale(index, now);
        }
    }

    public static boolean isDominantFor(
            ServerWorld world,
            BlockPos target,
            BlockPos currentPos,
            int currentLevel
    ) {
        WorldIndex index = ACTIVE.get(world);
        if (index == null || index.byPosition.isEmpty()) {
            return true;
        }

        long now = world.getTime();
        int winnerLevel = Math.max(1, Math.min(5, currentLevel));
        long winnerPos = currentPos.asLong();
        int minChunkX = (target.getX() - MAX_RADIUS) >> 4;
        int maxChunkX = (target.getX() + MAX_RADIUS) >> 4;
        int minChunkZ = (target.getZ() - MAX_RADIUS) >> 4;
        int maxChunkZ = (target.getZ() + MAX_RADIUS) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Set<Long> candidates = index.positionsByChunk.get(ChunkPos.toLong(chunkX, chunkZ));
                if (candidates == null) {
                    continue;
                }
                for (long candidatePos : candidates) {
                    ActiveTotem candidate = index.byPosition.get(candidatePos);
                    if (candidate == null
                            || now - candidate.lastSeenAt > ACTIVE_TIMEOUT_TICKS
                            || !covers(candidate, target)) {
                        continue;
                    }
                    if (candidate.level > winnerLevel
                            || candidate.level == winnerLevel && candidatePos < winnerPos) {
                        winnerLevel = candidate.level;
                        winnerPos = candidatePos;
                    }
                }
            }
        }

        return winnerLevel == Math.max(1, Math.min(5, currentLevel))
                && winnerPos == currentPos.asLong();
    }

    private static void pruneStale(WorldIndex index, long now) {
        Iterator<Map.Entry<Long, ActiveTotem>> iterator = index.byPosition.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ActiveTotem> entry = iterator.next();
            ActiveTotem totem = entry.getValue();
            if (now - totem.lastSeenAt <= ACTIVE_TIMEOUT_TICKS) {
                continue;
            }
            iterator.remove();
            Set<Long> bucket = index.positionsByChunk.get(chunkKey(totem.pos));
            if (bucket != null) {
                bucket.remove(entry.getKey());
                if (bucket.isEmpty()) {
                    index.positionsByChunk.remove(chunkKey(totem.pos));
                }
            }
        }
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static boolean covers(ActiveTotem totem, BlockPos target) {
        int dy = target.getY() - totem.pos.getY();
        if (dy < -2 || dy > 4) {
            return false;
        }
        int dx = target.getX() - totem.pos.getX();
        int dz = target.getZ() - totem.pos.getZ();
        int radius = radiusForLevel(totem.level);
        return dx * dx + dz * dz <= radius * radius;
    }

    private static final class WorldIndex {
        private final Map<Long, ActiveTotem> byPosition = new HashMap<>();
        private final Map<Long, Set<Long>> positionsByChunk = new HashMap<>();
    }

    private static final class ActiveTotem {
        private final BlockPos pos;
        private int level;
        private long lastSeenAt;

        private ActiveTotem(BlockPos pos, int level, long lastSeenAt) {
            this.pos = pos;
            this.level = level;
            this.lastSeenAt = lastSeenAt;
        }
    }
}
