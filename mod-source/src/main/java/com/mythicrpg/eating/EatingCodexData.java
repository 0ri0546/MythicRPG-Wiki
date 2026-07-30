package com.mythicrpg.eating;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.HashMap;
import java.util.Map;

public record EatingCodexData(Map<String, EatingCodexEntry> entries) {
    public static final Codec<EatingCodexData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, EatingCodexEntry.CODEC)
                    .optionalFieldOf("entries", Map.of())
                    .forGetter(EatingCodexData::entries)
    ).apply(instance, EatingCodexData::new));

    public EatingCodexData() {
        this(Map.of());
    }

    public EatingCodexData {
        entries = Map.copyOf(entries);
    }

    public EatingCodexEntry get(String recipeId) {
        return entries.getOrDefault(recipeId, new EatingCodexEntry(0, 0, 0L, 0, 0));
    }

    public boolean isDiscovered(String recipeId) {
        return get(recipeId).preparations() > 0;
    }

    public EatingCodexData withPreparation(
            String recipeId,
            DishRarity rarity,
            long day,
            int portions,
            int shelfLifeDays
    ) {
        Map<String, EatingCodexEntry> updated = new HashMap<>(entries);
        EatingCodexEntry previous = get(recipeId);
        updated.put(recipeId, previous.increment(rarity, day, portions, shelfLifeDays));
        return new EatingCodexData(updated);
    }
}
