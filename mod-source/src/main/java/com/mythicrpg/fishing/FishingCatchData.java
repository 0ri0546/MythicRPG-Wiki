package com.mythicrpg.fishing;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Optional;

public final class FishingCatchData {
    private static final String ROOT = "mythicrpg_fishing_catch";
    private static final String FAMILY = "family";
    private static final String RARITY = "rarity";
    private static final String BIOME = "biome";
    private static final String DIMENSION = "dimension";
    private static final String SOURCE = "source";

    private FishingCatchData() {
    }

    public static void write(
            ItemStack stack,
            FishingFamily family,
            FishingRarity rarity,
            String biome,
            String dimension,
            String source
    ) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, outer -> {
            NbtCompound data = new NbtCompound();
            data.putString(FAMILY, family.id());
            data.putInt(RARITY, rarity.rank());
            data.putString(BIOME, bounded(biome, 128));
            data.putString(DIMENSION, bounded(dimension, 128));
            data.putString(SOURCE, normalizedSource(source));
            outer.put(ROOT, data);
        });
    }

    public static Optional<Catch> read(ItemStack stack) {
        NbtCompound outer = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        ).copyNbt();
        if (!outer.contains(ROOT)) {
            return Optional.empty();
        }

        NbtCompound data = outer.getCompound(ROOT);
        Optional<FishingFamily> family = FishingFamily.byId(data.getString(FAMILY));
        if (family.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Catch(
                family.get(),
                FishingRarity.byRank(data.getInt(RARITY)),
                bounded(data.getString(BIOME), 128),
                bounded(data.getString(DIMENSION), 128),
                normalizedSource(data.getString(SOURCE))
        ));
    }

    private static String normalizedSource(String source) {
        return switch (source == null ? "" : source) {
            case "net" -> "net";
            case "boat" -> "boat";
            default -> "rod";
        };
    }

    private static String bounded(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public record Catch(
            FishingFamily family,
            FishingRarity rarity,
            String biome,
            String dimension,
            String source
    ) {
    }
}
