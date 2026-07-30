package com.mythicrpg.eating;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Stable model-predicate indices for every V1 signature-dish icon. */
public final class SignatureDishIconRegistry {
    private static final List<Identifier> ICONS = List.of(
            vanilla("carrot"),
            vanilla("potato"),
            vanilla("baked_potato"),
            vanilla("poisonous_potato"),
            vanilla("beetroot"),
            vanilla("pumpkin"),
            vanilla("golden_carrot"),
            vanilla("apple"),
            vanilla("golden_apple"),
            vanilla("enchanted_golden_apple"),
            vanilla("melon_slice"),
            vanilla("sweet_berries"),
            vanilla("glow_berries"),
            vanilla("chorus_fruit"),
            vanilla("wheat"),
            vanilla("bread"),
            vanilla("cookie"),
            vanilla("sugar"),
            vanilla("cocoa_beans"),
            vanilla("honey_bottle"),
            vanilla("egg"),
            vanilla("milk_bucket"),
            vanilla("red_mushroom"),
            vanilla("brown_mushroom"),
            vanilla("warped_fungus"),
            vanilla("nether_wart"),
            vanilla("blaze_powder"),
            vanilla("magma_cream"),
            vanilla("beef"),
            vanilla("cooked_beef"),
            vanilla("chicken"),
            vanilla("cooked_chicken"),
            vanilla("porkchop"),
            vanilla("cooked_porkchop"),
            vanilla("rabbit"),
            vanilla("cooked_rabbit"),
            vanilla("mutton"),
            vanilla("cooked_mutton"),
            vanilla("rotten_flesh"),
            vanilla("spider_eye"),
            vanilla("cod"),
            vanilla("cooked_cod"),
            vanilla("salmon"),
            vanilla("cooked_salmon"),
            vanilla("tropical_fish"),
            vanilla("pufferfish"),
            vanilla("kelp"),
            vanilla("dried_kelp"),
            vanilla("potion")
    );

    private static final Map<Identifier, Integer> MODEL_INDICES = createIndices();

    private SignatureDishIconRegistry() {
    }

    public static float predicateValue(ItemStack stack) {
        return SignatureDishData.read(stack)
                .map(data -> MODEL_INDICES.getOrDefault(data.icon(), 0).floatValue())
                .orElse(0.0F);
    }

    public static List<Identifier> icons() {
        return ICONS;
    }

    private static Map<Identifier, Integer> createIndices() {
        LinkedHashMap<Identifier, Integer> indices = new LinkedHashMap<>();
        for (int index = 0; index < ICONS.size(); index++) {
            indices.put(ICONS.get(index), index + 1);
        }
        return Map.copyOf(indices);
    }

    private static Identifier vanilla(String path) {
        return Identifier.ofVanilla(path);
    }
}
