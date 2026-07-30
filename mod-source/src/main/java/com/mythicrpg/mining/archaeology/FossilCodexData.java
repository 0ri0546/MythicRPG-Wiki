package com.mythicrpg.mining.archaeology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record FossilCodexData(
        Map<String, FossilCodexEntry> entries,
        Set<String> registeredSpecimenIds,
        Set<String> analyzedSpecimenIds
) {
    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            FossilCodexData::immutableSet,
            List::copyOf
    );
    public static final Codec<FossilCodexData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, FossilCodexEntry.CODEC)
                    .optionalFieldOf("entries", Map.of())
                    .forGetter(FossilCodexData::entries),
            STRING_SET_CODEC
                    .optionalFieldOf("registered_specimen_ids", Set.of())
                    .forGetter(FossilCodexData::registeredSpecimenIds),
            STRING_SET_CODEC
                    .optionalFieldOf("analyzed_specimen_ids", Set.of())
                    .forGetter(FossilCodexData::analyzedSpecimenIds)
    ).apply(instance, FossilCodexData::new));

    public FossilCodexData() {
        this(Map.of(), Set.of(), Set.of());
    }

    public FossilCodexData {
        entries = Map.copyOf(entries);
        registeredSpecimenIds = immutableSet(registeredSpecimenIds);
        analyzedSpecimenIds = immutableSet(analyzedSpecimenIds);
    }

    public FossilCodexEntry get(FossilFamily family, FossilRarity rarity) {
        return entries.getOrDefault(key(family, rarity), new FossilCodexEntry(0, 0L, 0));
    }

    public boolean containsSpecimen(String specimenId) {
        return registeredSpecimenIds.contains(specimenId);
    }

    public boolean containsAnalyzedSpecimen(String specimenId) {
        return analyzedSpecimenIds.contains(specimenId);
    }

    public FossilCodexData withReconstruction(
            FossilFamily family,
            FossilRarity rarity,
            String specimenId,
            long day
    ) {
        if (containsSpecimen(specimenId)) {
            return this;
        }

        Map<String, FossilCodexEntry> newEntries = new HashMap<>(entries);
        String key = key(family, rarity);
        FossilCodexEntry previous = newEntries.getOrDefault(key, new FossilCodexEntry(0, 0L, 0));
        newEntries.put(key, previous.incrementReconstructed(day));

        LinkedHashSet<String> newSpecimenIds = new LinkedHashSet<>(registeredSpecimenIds);
        newSpecimenIds.add(specimenId);

        return new FossilCodexData(
                newEntries,
                newSpecimenIds,
                analyzedSpecimenIds
        );
    }

    public FossilCodexData withAnalysis(
            FossilFamily family,
            FossilRarity rarity,
            String specimenId
    ) {
        if (!containsSpecimen(specimenId) || containsAnalyzedSpecimen(specimenId)) {
            return this;
        }

        Map<String, FossilCodexEntry> newEntries = new HashMap<>(entries);
        String key = key(family, rarity);
        FossilCodexEntry previous = newEntries.getOrDefault(key, new FossilCodexEntry(0, 0L, 0));
        newEntries.put(key, previous.incrementAnalyzed());

        LinkedHashSet<String> newAnalyzedIds = new LinkedHashSet<>(analyzedSpecimenIds);
        newAnalyzedIds.add(specimenId);
        return new FossilCodexData(
                newEntries,
                registeredSpecimenIds,
                newAnalyzedIds
        );
    }


    public FossilCodexData withAnalyses(Collection<AnalysisInput> analyses) {
        if (analyses.isEmpty() || registeredSpecimenIds.isEmpty()) {
            return this;
        }

        Set<String> registered = registeredSpecimenIds;
        LinkedHashSet<String> analyzed = new LinkedHashSet<>(analyzedSpecimenIds);
        Map<String, FossilCodexEntry> newEntries = new HashMap<>(entries);
        boolean changed = false;

        for (AnalysisInput analysis : analyses) {
            if (!registered.contains(analysis.specimenId())
                    || !analyzed.add(analysis.specimenId())) {
                continue;
            }
            String key = key(analysis.family(), analysis.rarity());
            FossilCodexEntry previous = newEntries.getOrDefault(
                    key,
                    new FossilCodexEntry(0, 0L, 0)
            );
            newEntries.put(key, previous.incrementAnalyzed());
            changed = true;
        }

        if (!changed) {
            return this;
        }
        return new FossilCodexData(
                newEntries,
                registeredSpecimenIds,
                analyzed
        );
    }


    private static Set<String> immutableSet(Collection<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    public record AnalysisInput(
            FossilFamily family,
            FossilRarity rarity,
            String specimenId
    ) {
    }

    public static String key(FossilFamily family, FossilRarity rarity) {
        return family.id() + ":" + rarity.id();
    }
}
