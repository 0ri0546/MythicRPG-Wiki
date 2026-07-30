package com.mythicrpg.building;

import net.minecraft.nbt.NbtCompound;

/** Player-attached, bounded persistence for Building XP anti-farm state. */
public interface BuildingXpDataHolder {
    NbtCompound mythicrpg$getBuildingXpData();

    void mythicrpg$setBuildingXpData(NbtCompound data);
}
