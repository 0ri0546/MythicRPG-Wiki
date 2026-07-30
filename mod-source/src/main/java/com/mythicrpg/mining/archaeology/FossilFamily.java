package com.mythicrpg.mining.archaeology;

import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

import java.util.Arrays;
import java.util.Optional;

public enum FossilFamily {
    SMALL_LAND("small_land", "family.mythicrpg.fossil.small_land"),
    MARINE("marine", "family.mythicrpg.fossil.marine"),
    FLYING("flying", "family.mythicrpg.fossil.flying"),
    INSECT("insect", "family.mythicrpg.fossil.insect"),
    LARGE_LAND("large_land", "family.mythicrpg.fossil.large_land");

    private static final FossilFamily[] VALUES = values();
    private static final String[] IDS = Arrays.stream(VALUES).map(FossilFamily::id).toArray(String[]::new);

    private final String id;
    private final String translationKey;

    FossilFamily(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public Text displayName() {
        return Text.translatable(translationKey);
    }

    public static FossilFamily random(Random random) {
        return VALUES[random.nextInt(VALUES.length)];
    }

    public static String[] ids() {
        return IDS.clone();
    }

    public static Optional<FossilFamily> byId(String id) {
        return Arrays.stream(VALUES)
                .filter(value -> value.id.equalsIgnoreCase(id))
                .findFirst();
    }
}
