package com.mythicrpg.mining.archaeology;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record FossilCodexEntry(
        int reconstructedCount,
        long firstReconstructedDay,
        int analyzedCount
) {
    public static final Codec<FossilCodexEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("reconstructed_count").forGetter(FossilCodexEntry::reconstructedCount),
            Codec.LONG.fieldOf("first_reconstructed_day").forGetter(FossilCodexEntry::firstReconstructedDay),
            Codec.INT.optionalFieldOf("analyzed_count", 0).forGetter(FossilCodexEntry::analyzedCount)
    ).apply(instance, FossilCodexEntry::new));

    public FossilCodexEntry incrementReconstructed(long day) {
        long firstDay = reconstructedCount <= 0 ? day : firstReconstructedDay;
        return new FossilCodexEntry(reconstructedCount + 1, firstDay, analyzedCount);
    }

    public FossilCodexEntry incrementAnalyzed() {
        return new FossilCodexEntry(reconstructedCount, firstReconstructedDay, analyzedCount + 1);
    }
}
