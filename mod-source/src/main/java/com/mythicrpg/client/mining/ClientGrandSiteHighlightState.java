package com.mythicrpg.client.mining;

import com.mythicrpg.network.GrandSiteHighlightPayload;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

public final class ClientGrandSiteHighlightState {

    private static Set<BlockPos> positions = Set.of();

    private ClientGrandSiteHighlightState() {
    }

    public static void update(GrandSiteHighlightPayload payload) {
        HashSet<BlockPos> unpacked = new HashSet<>(payload.positions().size());
        for (long packed : payload.positions()) {
            unpacked.add(BlockPos.fromLong(packed));
        }
        positions = Set.copyOf(unpacked);
    }

    public static Set<BlockPos> positions() {
        return positions;
    }

    public static void clear() {
        positions = Set.of();
    }
}
