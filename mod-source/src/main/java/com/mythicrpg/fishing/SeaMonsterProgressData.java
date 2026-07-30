package com.mythicrpg.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent personal gauges and victory history for the legendary Fishing Codex page. */
public record SeaMonsterProgressData(Map<String, SeaMonsterProgressEntry> entries) {
    public static final int MAX_GAUGE = 1000;
    private static final Codec<Map<String, SeaMonsterProgressEntry>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, SeaMonsterProgressEntry.CODEC);

    public static final Codec<SeaMonsterProgressData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MAP_CODEC.optionalFieldOf("entries", Map.of()).forGetter(SeaMonsterProgressData::entries)
    ).apply(instance, SeaMonsterProgressData::new));

    public SeaMonsterProgressData() {
        this(Map.of());
    }

    public SeaMonsterProgressData {
        LinkedHashMap<String, SeaMonsterProgressEntry> normalized = new LinkedHashMap<>();
        if (entries != null) {
            for (SeaMonsterType type : SeaMonsterType.values()) {
                SeaMonsterProgressEntry entry = entries.get(type.id());
                if (entry != null) normalized.put(type.id(), entry);
            }
        }
        entries = Map.copyOf(normalized);
    }

    public SeaMonsterProgressEntry get(SeaMonsterType type) {
        return entries.getOrDefault(type.id(), SeaMonsterProgressEntry.EMPTY);
    }

    public SeaMonsterProgressData addGauge(SeaMonsterType type, int amount) {
        LinkedHashMap<String, SeaMonsterProgressEntry> updated = new LinkedHashMap<>(entries);
        updated.put(type.id(), get(type).addGauge(amount));
        return new SeaMonsterProgressData(updated);
    }

    public SeaMonsterProgressData recordVictory(SeaMonsterType type, long day, String dimension) {
        LinkedHashMap<String, SeaMonsterProgressEntry> updated = new LinkedHashMap<>(entries);
        updated.put(type.id(), get(type).recordVictory(day, dimension));
        return new SeaMonsterProgressData(updated);
    }
}
