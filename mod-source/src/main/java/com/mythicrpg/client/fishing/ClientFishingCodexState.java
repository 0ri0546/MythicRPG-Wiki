
package com.mythicrpg.client.fishing;

import com.mythicrpg.fishing.FishingCodexEntry;
import com.mythicrpg.fishing.FishingFamily;
import com.mythicrpg.fishing.FishingRarity;
import com.mythicrpg.network.FishingCodexStatePayload;

import java.util.EnumMap;

public final class ClientFishingCodexState {
    private static final EnumMap<FishingFamily, EnumMap<FishingRarity, FishingCodexEntry>> ENTRIES =
            new EnumMap<>(FishingFamily.class);

    private ClientFishingCodexState() {
    }

    public static void update(FishingCodexStatePayload payload) {
        ENTRIES.clear();
        int size = payload.familyIds().size();
        for (int index = 0; index < size; index++) {
            FishingFamily family = FishingFamily.byId(payload.familyIds().get(index)).orElse(null);
            FishingRarity rarity = FishingRarity.byRank(payload.rarityRanks().get(index));
            if (family == null) {
                continue;
            }

            ENTRIES.computeIfAbsent(family, ignored -> new EnumMap<>(FishingRarity.class))
                    .put(rarity, new FishingCodexEntry(
                            Math.max(0, payload.captureCounts().get(index)),
                            rarity.rank(),
                            Math.max(0L, payload.firstDiscoveryDays().get(index)),
                            payload.firstBiomes().get(index),
                            payload.firstDimensions().get(index),
                            payload.lastSources().get(index)
                    ));
        }
    }

    public static FishingCodexEntry get(FishingFamily family, FishingRarity rarity) {
        EnumMap<FishingRarity, FishingCodexEntry> byRarity = ENTRIES.get(family);
        return byRarity == null
                ? FishingCodexEntry.EMPTY
                : byRarity.getOrDefault(rarity, FishingCodexEntry.EMPTY);
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
