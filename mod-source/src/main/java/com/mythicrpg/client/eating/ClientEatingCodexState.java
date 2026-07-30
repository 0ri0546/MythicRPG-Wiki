package com.mythicrpg.client.eating;

import com.mythicrpg.eating.EatingCodexEntry;
import com.mythicrpg.network.EatingCodexStatePayload;

import java.util.HashMap;
import java.util.Map;

public final class ClientEatingCodexState {
    private static Map<String, EatingCodexEntry> entries = Map.of();

    private ClientEatingCodexState() {
    }

    public static void update(EatingCodexStatePayload payload) {
        HashMap<String, EatingCodexEntry> updated = new HashMap<>();
        int size = Math.min(
                Math.min(payload.recipeIds().size(), payload.preparationCounts().size()),
                Math.min(
                        Math.min(payload.bestRarityRanks().size(), payload.firstDiscoveryDays().size()),
                        Math.min(payload.lastPortions().size(), payload.lastShelfLifeDays().size())
                )
        );
        for (int index = 0; index < size; index++) {
            updated.put(payload.recipeIds().get(index), new EatingCodexEntry(
                    payload.preparationCounts().get(index),
                    payload.bestRarityRanks().get(index),
                    payload.firstDiscoveryDays().get(index),
                    payload.lastPortions().get(index),
                    payload.lastShelfLifeDays().get(index)
            ));
        }
        entries = Map.copyOf(updated);
    }

    public static EatingCodexEntry get(String recipeId) {
        return entries.getOrDefault(recipeId, new EatingCodexEntry(0, 0, 0L, 0, 0));
    }

    public static boolean isDiscovered(String recipeId) {
        return get(recipeId).preparations() > 0;
    }

    public static void clear() {
        entries = Map.of();
    }
}
