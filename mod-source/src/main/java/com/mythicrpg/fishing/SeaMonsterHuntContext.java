package com.mythicrpg.fishing;

import net.minecraft.util.math.BlockPos;

/** Immutable server-side context captured when the fish bites inside the owner's local microclimate. */
public record SeaMonsterHuntContext(SeaMonsterType type, int gaugeGain, BlockPos spawnPos) {
    public SeaMonsterHuntContext {
        gaugeGain = Math.max(0, Math.min(SeaMonsterProgressData.MAX_GAUGE, gaugeGain));
        spawnPos = spawnPos == null ? BlockPos.ORIGIN : spawnPos.toImmutable();
    }
}
