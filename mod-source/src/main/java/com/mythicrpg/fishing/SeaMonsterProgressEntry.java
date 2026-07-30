package com.mythicrpg.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent personal progress for one legendary sea monster. Gauge units range from 0 to 1000. */
public record SeaMonsterProgressEntry(
        int gauge,
        int victories,
        long firstVictoryDay,
        String firstVictoryDimension
) {
    public static final SeaMonsterProgressEntry EMPTY = new SeaMonsterProgressEntry(0, 0, 0L, "");

    public static final Codec<SeaMonsterProgressEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("gauge", 0).forGetter(SeaMonsterProgressEntry::gauge),
            Codec.INT.optionalFieldOf("victories", 0).forGetter(SeaMonsterProgressEntry::victories),
            Codec.LONG.optionalFieldOf("first_victory_day", 0L).forGetter(SeaMonsterProgressEntry::firstVictoryDay),
            Codec.STRING.optionalFieldOf("first_victory_dimension", "").forGetter(SeaMonsterProgressEntry::firstVictoryDimension)
    ).apply(instance, SeaMonsterProgressEntry::new));

    public SeaMonsterProgressEntry {
        gauge = Math.max(0, Math.min(SeaMonsterProgressData.MAX_GAUGE, gauge));
        victories = Math.max(0, Math.min(1_000_000, victories));
        firstVictoryDay = Math.max(0L, firstVictoryDay);
        if (firstVictoryDimension == null) firstVictoryDimension = "";
        if (firstVictoryDimension.length() > 128) {
            firstVictoryDimension = firstVictoryDimension.substring(0, 128);
        }
    }

    public SeaMonsterProgressEntry addGauge(int amount) {
        return new SeaMonsterProgressEntry(
                Math.min(SeaMonsterProgressData.MAX_GAUGE, gauge + Math.max(0, amount)),
                victories,
                firstVictoryDay,
                firstVictoryDimension
        );
    }

    public SeaMonsterProgressEntry recordVictory(long day, String dimension) {
        boolean first = victories == 0;
        return new SeaMonsterProgressEntry(
                0,
                Math.min(1_000_000, victories + 1),
                first ? Math.max(0L, day) : firstVictoryDay,
                first ? dimension : firstVictoryDimension
        );
    }
}
