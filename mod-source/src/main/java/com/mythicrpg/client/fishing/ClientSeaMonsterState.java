package com.mythicrpg.client.fishing;

import com.mythicrpg.fishing.SeaMonsterProgressEntry;
import com.mythicrpg.fishing.SeaMonsterType;
import com.mythicrpg.network.SeaMonsterStatePayload;

import java.util.EnumMap;

/** Client-only immutable snapshot for the legendary Fishing Codex page. */
public final class ClientSeaMonsterState {
    private static final EnumMap<SeaMonsterType, SeaMonsterProgressEntry> ENTRIES = new EnumMap<>(SeaMonsterType.class);

    private ClientSeaMonsterState() {
    }

    public static void update(SeaMonsterStatePayload payload) {
        ENTRIES.clear();
        SeaMonsterType[] types = SeaMonsterType.values();
        for (int index = 0; index < types.length; index++) {
            ENTRIES.put(types[index], new SeaMonsterProgressEntry(
                    payload.gauges().get(index),
                    payload.victories().get(index),
                    payload.firstVictoryDays().get(index),
                    payload.firstVictoryDimensions().get(index)
            ));
        }
    }

    public static SeaMonsterProgressEntry get(SeaMonsterType type) {
        return ENTRIES.getOrDefault(type, SeaMonsterProgressEntry.EMPTY);
    }

    public static void clear() {
        ENTRIES.clear();
    }
}
