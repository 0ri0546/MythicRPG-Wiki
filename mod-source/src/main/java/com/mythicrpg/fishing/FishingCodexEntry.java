
package com.mythicrpg.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Persistent data for one exact family/rarity Codex card. */
public record FishingCodexEntry(
        int captures,
        int bestRarityRank,
        long firstDiscoveryDay,
        String firstBiome,
        String firstDimension,
        String lastSource
) {
    public static final FishingCodexEntry EMPTY =
            new FishingCodexEntry(0, 0, 0L, "", "", "rod");

    public FishingCodexEntry {
        captures = Math.max(0, Math.min(1_000_000, captures));
        bestRarityRank = Math.max(0, Math.min(FishingRarity.MYTHIC.rank(), bestRarityRank));
        firstDiscoveryDay = Math.max(0L, firstDiscoveryDay);
        firstBiome = bounded(firstBiome, 128);
        firstDimension = bounded(firstDimension, 128);
        lastSource = switch (lastSource == null ? "" : lastSource) {
            case "net" -> "net";
            case "boat" -> "boat";
            default -> "rod";
        };
    }

    public static final Codec<FishingCodexEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("captures", 0).forGetter(FishingCodexEntry::captures),
            Codec.INT.optionalFieldOf("best_rarity", 0).forGetter(FishingCodexEntry::bestRarityRank),
            Codec.LONG.optionalFieldOf("first_day", 0L).forGetter(FishingCodexEntry::firstDiscoveryDay),
            Codec.STRING.optionalFieldOf("first_biome", "").forGetter(FishingCodexEntry::firstBiome),
            Codec.STRING.optionalFieldOf("first_dimension", "").forGetter(FishingCodexEntry::firstDimension),
            Codec.STRING.optionalFieldOf("last_source", "rod").forGetter(FishingCodexEntry::lastSource)
    ).apply(instance, FishingCodexEntry::new));

    public FishingCodexEntry increment(
            FishingRarity rarity,
            long day,
            String biome,
            String dimension,
            String source
    ) {
        boolean first = captures <= 0;
        return new FishingCodexEntry(
                Math.min(1_000_000, captures + 1),
                rarity.rank(),
                first ? day : firstDiscoveryDay,
                first ? biome : firstBiome,
                first ? dimension : firstDimension,
                source
        );
    }

    private static String bounded(String value, int maxLength) {
        if (value == null) return "";
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
