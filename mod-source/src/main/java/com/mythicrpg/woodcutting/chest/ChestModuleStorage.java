package com.mythicrpg.woodcutting.chest;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/** Extra persistent state attached to each physical vanilla chest block entity. */
public interface ChestModuleStorage {

    int EXTRA_STORAGE_SIZE = 27;

    DefaultedList<ItemStack> mythicrpg$getExtraStacks();

    ItemStack mythicrpg$getModule();

    void mythicrpg$setModuleDirect(ItemStack stack);

    void mythicrpg$markModuleStorageDirty();
}
