package com.mythicrpg.mining.archaeology.relic;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;

public enum RelicLevel {
    I(1, 40, Formatting.WHITE),
    II(2, 25, Formatting.GREEN),
    III(3, 18, Formatting.AQUA),
    IV(4, 10, Formatting.LIGHT_PURPLE),
    V(5, 7, Formatting.GOLD);

    private static final int TOTAL_WEIGHT = 100;
    private final int value;
    private final int weight;
    private final Formatting formatting;

    RelicLevel(int value, int weight, Formatting formatting) {
        this.value = value;
        this.weight = weight;
        this.formatting = formatting;
    }

    public int value() { return value; }
    public int weight() { return weight; }
    public Formatting formatting() { return formatting; }

    public Text displayName() {
        return Text.translatable("relic_level.mythicrpg." + name().toLowerCase()).formatted(formatting);
    }

    public static RelicLevel roll(Random random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        for (RelicLevel level : values()) {
            roll -= level.weight;
            if (roll < 0) return level;
        }
        return I;
    }

    public static RelicLevel fromValue(int value) {
        for (RelicLevel level : values()) {
            if (level.value == value) return level;
        }
        return I;
    }
}
