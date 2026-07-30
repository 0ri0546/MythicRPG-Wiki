package com.mythicrpg.mixin;

import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DoubleInventory.class)
public interface DoubleInventoryAccessor {

    @Accessor("first")
    Inventory mythicrpg$getFirst();

    @Accessor("second")
    Inventory mythicrpg$getSecond();
}
