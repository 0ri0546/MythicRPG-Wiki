package com.mythicrpg.fishing;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;

/** Stable custom-data format for Weather Wand mode and permanent seals. */
public final class WeatherWandData {
    private static final String MODE_KEY = "fishing_weather_mode";
    private static final String SEAL_MASK_KEY = "fishing_weather_seals";

    private WeatherWandData() {
    }

    public static FishingWeatherManager.Mode mode(ItemStack stack) {
        int value = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .getInt(MODE_KEY);
        FishingWeatherManager.Mode[] modes = FishingWeatherManager.Mode.values();
        return modes[Math.floorMod(value, modes.length)];
    }

    public static void setMode(ItemStack stack, FishingWeatherManager.Mode mode) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(MODE_KEY, mode.ordinal()));
    }

    public static FishingWeatherManager.Mode nextMode(ItemStack stack) {
        FishingWeatherManager.Mode[] modes = FishingWeatherManager.Mode.values();
        FishingWeatherManager.Mode next = modes[(mode(stack).ordinal() + 1) % modes.length];
        setMode(stack, next);
        return next;
    }

    public static boolean hasSeal(ItemStack stack, SeaMonsterType type) {
        return (sealMask(stack) & bit(type)) != 0;
    }

    public static boolean hasAnySeal(ItemStack stack) {
        return sealMask(stack) != 0;
    }

    public static boolean isHarmonized(ItemStack stack) {
        int all = (1 << SeaMonsterType.values().length) - 1;
        return (sealMask(stack) & all) == all;
    }

    public static boolean applySeal(ItemStack stack, SeaMonsterType type) {
        int current = sealMask(stack);
        int bit = bit(type);
        if ((current & bit) != 0) return false;
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(SEAL_MASK_KEY, current | bit));
        return true;
    }

    public static float modePredicate(ItemStack stack) {
        return switch (mode(stack)) {
            case RAIN -> 0.0F;
            case SUN -> 0.5F;
            case STORM -> 1.0F;
        };
    }

    private static int sealMask(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
                .copyNbt()
                .getInt(SEAL_MASK_KEY);
    }

    private static int bit(SeaMonsterType type) {
        return 1 << type.ordinal();
    }
}
