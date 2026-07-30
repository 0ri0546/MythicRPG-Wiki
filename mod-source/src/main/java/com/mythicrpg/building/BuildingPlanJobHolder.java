package com.mythicrpg.building;

import net.minecraft.nbt.NbtCompound;

/**
 * Player-attached storage for a persistent Building plan job.
 *
 * <p>The job and the player's inventory are serialized into the same player
 * data file. This keeps the material escrow and the inventory change in one
 * save unit instead of splitting them across unrelated world files.</p>
 */
public interface BuildingPlanJobHolder {
    NbtCompound mythicrpg$getBuildingPlanJobData();

    void mythicrpg$setBuildingPlanJobData(NbtCompound data);

    /** Updates only the small mutable portion of an already attached receipt. */
    boolean mythicrpg$updateBuildingPlanJobProgress(NbtCompound progress);
}
