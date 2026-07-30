package com.mythicrpg.eating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record EatingCodexEntry(
        int preparations,
        int bestRarityRank,
        long firstDiscoveryDay,
        int lastPortions,
        int lastShelfLifeDays
) {
    public static final Codec<EatingCodexEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("preparations", 0).forGetter(EatingCodexEntry::preparations),
            Codec.INT.optionalFieldOf("best_rarity", 0).forGetter(EatingCodexEntry::bestRarityRank),
            Codec.LONG.optionalFieldOf("first_day", 0L).forGetter(EatingCodexEntry::firstDiscoveryDay),
            Codec.INT.optionalFieldOf("last_portions", 0).forGetter(EatingCodexEntry::lastPortions),
            Codec.INT.optionalFieldOf("last_shelf_life_days", 0).forGetter(EatingCodexEntry::lastShelfLifeDays)
    ).apply(instance, EatingCodexEntry::new));

    public EatingCodexEntry increment(
            DishRarity rarity,
            long day,
            int portions,
            int shelfLifeDays
    ) {
        return new EatingCodexEntry(
                preparations + 1,
                Math.max(bestRarityRank, rarity.rank()),
                preparations == 0 ? day : firstDiscoveryDay,
                Math.max(1, portions),
                Math.max(1, shelfLifeDays)
        );
    }
}
