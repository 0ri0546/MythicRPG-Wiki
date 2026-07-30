
package com.mythicrpg.fishing;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;
import java.util.UUID;

/** Stable identity used to bind an open rod screen to the exact ItemStack instance. */
public final class FishingRodData {
    private static final String ROOT = "mythicrpg_fishing_rod";
    private static final String ID = "id";

    private FishingRodData() {
    }

    public static UUID ensureId(ItemStack stack) {
        Optional<UUID> existing = id(stack);
        if (existing.isPresent()) {
            return existing.get();
        }
        UUID created = UUID.randomUUID();
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, outer -> {
            NbtCompound data = outer.contains(ROOT) ? outer.getCompound(ROOT) : new NbtCompound();
            data.putUuid(ID, created);
            outer.put(ROOT, data);
        });
        return created;
    }

    public static Optional<UUID> id(ItemStack stack) {
        NbtCompound outer = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
        if (!outer.contains(ROOT)) {
            return Optional.empty();
        }
        NbtCompound data = outer.getCompound(ROOT);
        return data.containsUuid(ID) ? Optional.of(data.getUuid(ID)) : Optional.empty();
    }

    public static boolean matches(ItemStack stack, UUID expected) {
        return expected != null && id(stack).filter(expected::equals).isPresent();
    }
}
