package com.mythicrpg.building;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

/** Compact ItemStack component for the selected static decoration effect. */
public final class StaticDecorationItemData {
    private static final String ROOT_KEY = "mythicrpg_static_decoration";
    private static final String VERSION_KEY = "version";
    private static final String EFFECT_KEY = "effect";
    private static final int VERSION = 1;

    private StaticDecorationItemData() {}

    public static StaticDecorationEffect read(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        if (component.getSize() > 8_192) return StaticDecorationEffect.ELECTRIC_SPARK;
        NbtCompound custom = component.copyNbt();
        if (!custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) return StaticDecorationEffect.ELECTRIC_SPARK;
        NbtCompound root = custom.getCompound(ROOT_KEY);
        if (root.getInt(VERSION_KEY) != VERSION) return StaticDecorationEffect.ELECTRIC_SPARK;
        return StaticDecorationEffect.byId(root.getString(EFFECT_KEY)).orElse(StaticDecorationEffect.ELECTRIC_SPARK);
    }

    public static void write(ItemStack stack, StaticDecorationEffect effect) {
        StaticDecorationEffect safe = effect == null ? StaticDecorationEffect.ELECTRIC_SPARK : effect;
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, custom -> {
            NbtCompound root = new NbtCompound();
            root.putInt(VERSION_KEY, VERSION);
            root.putString(EFFECT_KEY, safe.id());
            custom.put(ROOT_KEY, root);
        });
    }
}
