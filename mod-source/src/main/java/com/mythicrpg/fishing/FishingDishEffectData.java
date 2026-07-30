package com.mythicrpg.fishing;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

public final class FishingDishEffectData {
    private static final String KEY = "mythicrpg_fishing_dish_effect";

    private FishingDishEffectData() {}

    public static void write(ItemStack stack, FishingFamily family) {
        if (family != FishingFamily.INFERNAL && family != FishingFamily.VOID) return;
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putString(KEY, family.id()));
    }

    public static FishingFamily read(ItemStack stack) {
        String id = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getString(KEY);
        return FishingFamily.byId(id).orElse(null);
    }
}
