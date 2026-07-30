package com.mythicrpg.eating;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

public final class DeliveryPhoneData {
    private static final String ROOT = "mythicrpg_delivery_phone";
    private static final String SOURCE = "source";
    private static final String COUNT = "count";

    private DeliveryPhoneData() {
    }

    public static Settings read(ItemStack stack) {
        NbtCompound custom = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!custom.contains(ROOT)) {
            return new Settings(DeliverySource.COOKING_POT, 1);
        }
        NbtCompound data = custom.getCompound(ROOT);
        return new Settings(
                DeliverySource.byOrdinal(data.getInt(SOURCE)),
                Math.max(1, Math.min(9, data.getInt(COUNT)))
        );
    }

    public static void write(ItemStack stack, DeliverySource source, int count) {
        NbtCompound data = new NbtCompound();
        data.putInt(SOURCE, source.ordinal());
        data.putInt(COUNT, Math.max(1, Math.min(9, count)));
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(ROOT, data));
    }

    public record Settings(DeliverySource source, int count) {
    }
}
