package com.mythicrpg.building;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Short-lived server proof that a Building configuration screen was opened
 * from the currently held ItemStack. Also bounds C2S work without affecting
 * normal UI interaction.
 */
public final class BuildingUiSessionManager {
    private static final long SESSION_LIFETIME_NANOS = 5L * 60L * 1_000_000_000L;
    private static final long LIGHT_INTERVAL_NANOS = 50_000_000L;
    private static final long MUTATION_INTERVAL_NANOS = 100_000_000L;
    private static final long HEAVY_INTERVAL_NANOS = 400_000_000L;

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private BuildingUiSessionManager() {
    }

    public static void open(
            ServerPlayerEntity player,
            Tool tool,
            Hand hand,
            ItemStack stack,
            long targetPos
    ) {
        if (player == null || tool == null || hand == null || stack == null || stack.isEmpty()) {
            return;
        }
        long now = System.nanoTime();
        SESSIONS.put(player.getUuid(), new Session(
                tool,
                hand,
                stack,
                player.getServerWorld().getRegistryKey().getValue().toString(),
                targetPos,
                now + SESSION_LIFETIME_NANOS
        ));
    }

    public static boolean allow(
            ServerPlayerEntity player,
            Tool tool,
            Hand hand,
            ItemStack currentStack,
            ActionCost cost,
            int fingerprint,
            long targetPos
    ) {
        if (player == null || tool == null || hand == null || currentStack == null) {
            return false;
        }
        Session session = SESSIONS.get(player.getUuid());
        long now = System.nanoTime();
        if (session == null
                || session.expiresAtNanos < now
                || session.tool != tool
                || session.hand != hand
                || session.stack != currentStack
                || !session.dimensionId.equals(
                        player.getServerWorld().getRegistryKey().getValue().toString()
                )
                || (session.targetPos != Long.MIN_VALUE && session.targetPos != targetPos)) {
            SESSIONS.remove(player.getUuid());
            return false;
        }

        long interval = switch (cost) {
            case LIGHT -> LIGHT_INTERVAL_NANOS;
            case MUTATION -> MUTATION_INTERVAL_NANOS;
            case HEAVY -> HEAVY_INTERVAL_NANOS;
        };
        long lastForLane = session.lastAccepted(cost);
        if (lastForLane != 0L && now - lastForLane < interval) {
            return false;
        }
        if (session.lastFingerprint == fingerprint
                && now - session.lastFingerprintNanos < 1_000_000_000L) {
            return false;
        }

        session.accept(cost, now);
        session.lastFingerprint = fingerprint;
        session.lastFingerprintNanos = now;
        session.expiresAtNanos = now + SESSION_LIFETIME_NANOS;
        return true;
    }

    public static void close(ServerPlayerEntity player) {
        if (player != null) {
            SESSIONS.remove(player.getUuid());
        }
    }

    public static void clearPlayer(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    public enum Tool {
        PLAN_2D,
        PLAN_3D,
        MINIATURE,
        ARCHITECT_COMPASS,
        STATIC_DECORATION_ITEM,
        STATIC_DECORATION_BLOCK
    }

    public enum ActionCost {
        LIGHT,
        MUTATION,
        HEAVY
    }

    private static final class Session {
        private final Tool tool;
        private final Hand hand;
        private final ItemStack stack;
        private final String dimensionId;
        private final long targetPos;
        private long expiresAtNanos;
        private long lastLightNanos;
        private long lastMutationNanos;
        private long lastHeavyNanos;
        private int lastFingerprint = Integer.MIN_VALUE;
        private long lastFingerprintNanos;

        private Session(
                Tool tool,
                Hand hand,
                ItemStack stack,
                String dimensionId,
                long targetPos,
                long expiresAtNanos
        ) {
            this.tool = tool;
            this.hand = hand;
            this.stack = stack;
            this.dimensionId = dimensionId;
            this.targetPos = targetPos;
            this.expiresAtNanos = expiresAtNanos;
        }

        private long lastAccepted(ActionCost cost) {
            return switch (cost) {
                case LIGHT -> lastLightNanos;
                case MUTATION -> lastMutationNanos;
                case HEAVY -> lastHeavyNanos;
            };
        }

        private void accept(ActionCost cost, long now) {
            switch (cost) {
                case LIGHT -> lastLightNanos = now;
                case MUTATION -> lastMutationNanos = now;
                case HEAVY -> lastHeavyNanos = now;
            }
        }
    }
}
