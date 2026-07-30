package com.mythicrpg.fishing;

import com.mythicrpg.core.ModItems;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.item.Item;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.Optional;

/** The three personal legendary sea-monster hunts driven by Weather Wand microclimates. */
public enum SeaMonsterType {
    NESSIE(FishingWeatherManager.Mode.RAIN, 180.0F, 8.0F, 6.0D, 80, 4, 1.10D, 0.55D, BossBar.Color.GREEN),
    MEGALODON(FishingWeatherManager.Mode.STORM, 240.0F, 12.0F, 7.0D, 60, 6, 1.50D, 0.45D, BossBar.Color.RED),
    WHALE(FishingWeatherManager.Mode.SUN, 210.0F, 10.0F, 7.0D, 100, 5, 0.50D, 1.35D, BossBar.Color.BLUE);

    private final FishingWeatherManager.Mode weatherMode;
    private final float maxHealth;
    private final float attackDamage;
    private final double attackRadius;
    private final int attackIntervalTicks;
    private final int slimeSize;
    private final double horizontalKnockback;
    private final double verticalKnockback;
    private final BossBar.Color bossBarColor;

    SeaMonsterType(
            FishingWeatherManager.Mode weatherMode,
            float maxHealth,
            float attackDamage,
            double attackRadius,
            int attackIntervalTicks,
            int slimeSize,
            double horizontalKnockback,
            double verticalKnockback,
            BossBar.Color bossBarColor
    ) {
        this.weatherMode = weatherMode;
        this.maxHealth = maxHealth;
        this.attackDamage = attackDamage;
        this.attackRadius = attackRadius;
        this.attackIntervalTicks = attackIntervalTicks;
        this.slimeSize = slimeSize;
        this.horizontalKnockback = horizontalKnockback;
        this.verticalKnockback = verticalKnockback;
        this.bossBarColor = bossBarColor;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public FishingWeatherManager.Mode weatherMode() {
        return weatherMode;
    }

    public float maxHealth() {
        return maxHealth;
    }

    public float attackDamage() {
        return attackDamage;
    }

    public double attackRadius() {
        return attackRadius;
    }

    public int attackIntervalTicks() {
        return attackIntervalTicks;
    }

    public int slimeSize() {
        return slimeSize;
    }

    public double horizontalKnockback() {
        return horizontalKnockback;
    }

    public double verticalKnockback() {
        return verticalKnockback;
    }

    public BossBar.Color bossBarColor() {
        return bossBarColor;
    }

    public Text displayName() {
        return Text.translatable("sea_monster.mythicrpg." + id());
    }

    public String titleId() {
        return "special_sea_hunter_" + id();
    }

    public Item material() {
        return switch (this) {
            case NESSIE -> ModItems.NESSIE_SCALE;
            case MEGALODON -> ModItems.MEGALODON_TOOTH;
            case WHALE -> ModItems.WHALE_AMBERGRIS;
        };
    }

    public Item charm() {
        return switch (this) {
            case NESSIE -> ModItems.NESSIE_CHARM;
            case MEGALODON -> ModItems.MEGALODON_CHARM;
            case WHALE -> ModItems.WHALE_CHARM;
        };
    }

    public static Optional<SeaMonsterType> byId(String id) {
        if (id == null) return Optional.empty();
        for (SeaMonsterType type : values()) {
            if (type.id().equalsIgnoreCase(id)) return Optional.of(type);
        }
        return Optional.empty();
    }

    public static Optional<SeaMonsterType> forWeather(FishingWeatherManager.Mode mode) {
        if (mode == null) return Optional.empty();
        for (SeaMonsterType type : values()) {
            if (type.weatherMode == mode) return Optional.of(type);
        }
        return Optional.empty();
    }
}
