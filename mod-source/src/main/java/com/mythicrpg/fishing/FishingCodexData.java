
package com.mythicrpg.fishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fishing Codex persistence.
 *
 * <p>Entries are keyed by {@code family/rarity}, producing the same card-based,
 * paginated structure as the Eating Codex. The constructor also migrates the
 * short-lived family-only format from the first Fishing implementation.</p>
 */
public record FishingCodexData(Map<String, FishingCodexEntry> entries) {
    private static final Codec<Map<String, FishingCodexEntry>> MAP_CODEC =
            Codec.unboundedMap(Codec.STRING, FishingCodexEntry.CODEC);

    public static final Codec<FishingCodexData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MAP_CODEC.optionalFieldOf("entries", Map.of()).forGetter(FishingCodexData::entries)
    ).apply(instance, FishingCodexData::new));

    public FishingCodexData() {
        this(Map.of());
    }

    public FishingCodexData {
        entries = normalize(entries);
    }

    public FishingCodexEntry get(FishingFamily family, FishingRarity rarity) {
        return entries.getOrDefault(key(family, rarity), FishingCodexEntry.EMPTY);
    }

    public FishingCodexData withCatch(
            FishingFamily family,
            FishingRarity rarity,
            long day,
            String biome,
            String dimension,
            String source
    ) {
        LinkedHashMap<String, FishingCodexEntry> updated = new LinkedHashMap<>(entries);
        String key = key(family, rarity);
        updated.put(key, get(family, rarity).increment(rarity, day, biome, dimension, source));
        return new FishingCodexData(updated);
    }

    public static String key(FishingFamily family, FishingRarity rarity) {
        return family.id() + "/" + rarity.id();
    }

    private static Map<String, FishingCodexEntry> normalize(Map<String, FishingCodexEntry> raw) {
        LinkedHashMap<String, FishingCodexEntry> normalized = new LinkedHashMap<>();
        if (raw == null) {
            return Map.of();
        }

        for (Map.Entry<String, FishingCodexEntry> entry : raw.entrySet()) {
            String rawKey = entry.getKey();
            FishingCodexEntry value = entry.getValue();
            if (rawKey == null || value == null) {
                continue;
            }

            String[] parts = rawKey.toLowerCase(java.util.Locale.ROOT).split("/", 2);
            if (parts.length == 2) {
                FishingFamily family = FishingFamily.byId(parts[0]).orElse(null);
                FishingRarity rarity = FishingRarity.byId(parts[1]).orElse(null);
                if (family != null && rarity != null) {
                    normalized.put(key(family, rarity), value);
                }
                continue;
            }

            // Migration from the former family-only Codex format.
            FishingFamily family = FishingFamily.byId(rawKey).orElseGet(() -> {
                try {
                    return FishingFamily.valueOf(rawKey.toUpperCase(java.util.Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            });
            if (family != null && value.captures() > 0) {
                FishingRarity rarity = FishingRarity.byRank(value.bestRarityRank());
                normalized.put(key(family, rarity), value);
            }
        }
        return Map.copyOf(normalized);
    }
}
