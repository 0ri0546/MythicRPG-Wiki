package com.mythicrpg.mining.archaeology.relic;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

public final class RelicItemData {
    private static final String LEVEL_KEY = "MythicRelicLevel";

    private RelicItemData() {}

    public static void setLevel(ItemStack stack, RelicLevel level) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(LEVEL_KEY, level.value()));
    }

    public static RelicLevel getLevel(ItemStack stack) {
        int value = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .getInt(LEVEL_KEY);
        return RelicLevel.fromValue(value <= 0 ? 1 : value);
    }
}
