package com.mythicrpg.fishing;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum FishingRarity {
    COMMON(0, 50, 8, Formatting.WHITE),
    RARE(1, 25, 18, Formatting.AQUA),
    EPIC(2, 15, 40, Formatting.LIGHT_PURPLE),
    LEGENDARY(3, 7, 90, Formatting.GOLD),
    MYTHIC(4, 3, 180, Formatting.RED);

    private final int rank;
    private final int baseWeight;
    private final int xp;
    private final Formatting formatting;

    FishingRarity(int rank, int baseWeight, int xp, Formatting formatting) {
        this.rank = rank;
        this.baseWeight = baseWeight;
        this.xp = xp;
        this.formatting = formatting;
    }

    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
    public int rank() { return rank; }
    public int baseWeight() { return baseWeight; }
    public int xp() { return xp; }
    public Formatting formatting() { return formatting; }
    public Text displayName() { return Text.translatable("rarity.mythicrpg." + name().toLowerCase()); }
    public static java.util.Optional<FishingRarity> byId(String id) {
        if (id == null) return java.util.Optional.empty();
        for (FishingRarity value : values()) if (value.id().equalsIgnoreCase(id)) return java.util.Optional.of(value);
        return java.util.Optional.empty();
    }
    public static FishingRarity byRank(int rank) {
        for (FishingRarity value : values()) if (value.rank == rank) return value;
        return COMMON;
    }
}
