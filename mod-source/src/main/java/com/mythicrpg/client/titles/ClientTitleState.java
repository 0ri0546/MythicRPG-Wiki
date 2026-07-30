package com.mythicrpg.client.titles;

import com.mythicrpg.network.TitleStatePayload;

import java.util.List;

public final class ClientTitleState {
    private static Snapshot snapshot = Snapshot.empty();
    private static boolean received;

    private ClientTitleState() {
    }

    public static void update(TitleStatePayload payload) {
        snapshot = new Snapshot(
                List.copyOf(payload.unlockedTitleIds()),
                payload.activeTitleId(),
                payload.primaryColorId(),
                payload.secondaryColorId(),
                payload.gradient(),
                payload.finishId()
        );
        received = true;
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static boolean hasReceivedState() {
        return received;
    }

    public static void clear() {
        snapshot = Snapshot.empty();
        received = false;
    }

    public record Snapshot(
            List<String> unlockedTitleIds,
            String activeTitleId,
            String primaryColorId,
            String secondaryColorId,
            boolean gradient,
            String finishId
    ) {
        private static Snapshot empty() {
            return new Snapshot(List.of(), "", "white", "white", false, "none");
        }
    }
}
