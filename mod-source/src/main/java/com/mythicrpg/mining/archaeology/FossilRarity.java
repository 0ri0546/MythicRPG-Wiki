package com.mythicrpg.mining.archaeology;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public enum FossilRarity {
    COMMON("common", 1, 40, 5 * 20, 15 * 60 * 20, Formatting.WHITE, new Vector3f(0.82F, 0.82F, 0.82F)),
    RARE("rare", 2, 30, 8 * 20, 25 * 60 * 20, Formatting.AQUA, new Vector3f(0.20F, 0.65F, 1.00F)),
    EPIC("epic", 3, 17, 12 * 20, 40 * 60 * 20, Formatting.LIGHT_PURPLE, new Vector3f(0.72F, 0.28F, 0.95F)),
    LEGENDARY("legendary", 4, 9, 18 * 20, 60 * 60 * 20, Formatting.GOLD, new Vector3f(1.00F, 0.63F, 0.10F)),
    MYTHIC("mythic", 5, 4, 25 * 20, 90 * 60 * 20, Formatting.RED, new Vector3f(1.00F, 0.18F, 0.30F));

    private static final FossilRarity[] VALUES = values();
    private static final String[] IDS = Arrays.stream(VALUES).map(FossilRarity::id).toArray(String[]::new);
    private static final int TOTAL_GENERATION_WEIGHT = Arrays.stream(VALUES)
            .mapToInt(FossilRarity::generationWeight)
            .sum();

    private final String id;
    private final int rank;
    private final int generationWeight;
    private final int cleaningTicks;
    private final int incubationTicks;
    private final Formatting formatting;
    private final Vector3f particleColor;

    FossilRarity(
            String id,
            int rank,
            int generationWeight,
            int cleaningTicks,
            int incubationTicks,
            Formatting formatting,
            Vector3f particleColor
    ) {
        this.id = id;
        this.rank = rank;
        this.generationWeight = generationWeight;
        this.cleaningTicks = cleaningTicks;
        this.incubationTicks = incubationTicks;
        this.formatting = formatting;
        this.particleColor = particleColor;
    }

    public String id() {
        return id;
    }

    public int rank() {
        return rank;
    }

    public int generationWeight() {
        return generationWeight;
    }

    public int cleaningTicks() {
        return cleaningTicks;
    }

    /** Final production duration validated for the archaeology loop. */
    public int incubationTicks() {
        return incubationTicks;
    }

    public Formatting formatting() {
        return formatting;
    }

    public Vector3f particleColor() {
        return new Vector3f(particleColor);
    }

    public Text displayName() {
        return Text.translatable("rarity.mythicrpg.fossil." + id).formatted(formatting);
    }

    /** Uses the single global fossil generation distribution: 40 / 30 / 17 / 9 / 4. */
    public static FossilRarity rollGeneration(Random random) {
        int roll = random.nextInt(TOTAL_GENERATION_WEIGHT);
        for (FossilRarity rarity : VALUES) {
            roll -= rarity.generationWeight;
            if (roll < 0) {
                return rarity;
            }
        }
        return COMMON;
    }

    public static String[] ids() {
        return IDS.clone();
    }

    public static Optional<FossilRarity> byId(String id) {
        return Arrays.stream(VALUES)
                .filter(value -> value.id.equalsIgnoreCase(id))
                .findFirst();
    }

    public static FossilRarity median(List<FossilRarity> values) {
        if (values.size() != 9) {
            throw new IllegalArgumentException("Exactly 9 fossil rarities are required");
        }

        return values.stream()
                .sorted(Comparator.comparingInt(FossilRarity::rank))
                .skip(4)
                .findFirst()
                .orElse(COMMON);
    }
}
