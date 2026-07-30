package com.mythicrpg.client.mining;

import com.mythicrpg.mining.archaeology.FossilCodexEntry;
import com.mythicrpg.mining.archaeology.FossilFamily;
import com.mythicrpg.mining.archaeology.FossilRarity;
import com.mythicrpg.network.FossilCodexStatePayload;

import java.util.HashMap;
import java.util.Map;

public final class ClientFossilCodexState {

    private static Map<String, FossilCodexEntry> entries = Map.of();

    private ClientFossilCodexState() {
    }

    public static void update(FossilCodexStatePayload payload) {
        HashMap<String, FossilCodexEntry> updated = new HashMap<>();
        int size = Math.min(
                Math.min(payload.keys().size(), payload.reconstructedCounts().size()),
                Math.min(payload.firstReconstructedDays().size(), payload.analyzedCounts().size())
        );

        for (int index = 0; index < size; index++) {
            updated.put(payload.keys().get(index), new FossilCodexEntry(
                    payload.reconstructedCounts().get(index),
                    payload.firstReconstructedDays().get(index),
                    payload.analyzedCounts().get(index)
            ));
        }
        entries = Map.copyOf(updated);
    }

    public static void clear() {
        entries = Map.of();
    }

    public static FossilCodexEntry get(FossilFamily family, FossilRarity rarity) {
        return entries.getOrDefault(
                family.id() + ":" + rarity.id(),
                new FossilCodexEntry(0, 0L, 0)
        );
    }
}
