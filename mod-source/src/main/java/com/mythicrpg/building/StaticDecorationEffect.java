package com.mythicrpg.building;

import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/** Closed V1 catalog of 32 safe vanilla particles for 1x1x1 decorations. */
public enum StaticDecorationEffect {
    FLAME("flame", ParticleTypes.FLAME),
    SMALL_FLAME("small_flame", ParticleTypes.SMALL_FLAME),
    SOUL_FIRE_FLAME("soul_fire_flame", ParticleTypes.SOUL_FIRE_FLAME),
    SMOKE("smoke", ParticleTypes.SMOKE),
    WHITE_SMOKE("white_smoke", ParticleTypes.WHITE_SMOKE),
    CAMPFIRE_COSY_SMOKE("campfire_cosy_smoke", ParticleTypes.CAMPFIRE_COSY_SMOKE),
    ASH("ash", ParticleTypes.ASH),
    WHITE_ASH("white_ash", ParticleTypes.WHITE_ASH),

    ENCHANT("enchant", ParticleTypes.ENCHANT),
    ENCHANTED_HIT("enchanted_hit", ParticleTypes.ENCHANTED_HIT),
    END_ROD("end_rod", ParticleTypes.END_ROD),
    PORTAL("portal", ParticleTypes.PORTAL),
    REVERSE_PORTAL("reverse_portal", ParticleTypes.REVERSE_PORTAL),
    WITCH("witch", ParticleTypes.WITCH),
    EFFECT("effect", ParticleTypes.EFFECT),
    INSTANT_EFFECT("instant_effect", ParticleTypes.INSTANT_EFFECT),

    ELECTRIC_SPARK("electric_spark", ParticleTypes.ELECTRIC_SPARK),
    FIREWORK("firework", ParticleTypes.FIREWORK),
    GLOW("glow", ParticleTypes.GLOW),
    TOTEM_OF_UNDYING("totem_of_undying", ParticleTypes.TOTEM_OF_UNDYING),
    SOUL("soul", ParticleTypes.SOUL),
    SCULK_SOUL("sculk_soul", ParticleTypes.SCULK_SOUL),
    CRIT("crit", ParticleTypes.CRIT),

    CHERRY_LEAVES("cherry_leaves", ParticleTypes.CHERRY_LEAVES),
    SPORE_BLOSSOM_AIR("spore_blossom_air", ParticleTypes.SPORE_BLOSSOM_AIR),
    CRIMSON_SPORE("crimson_spore", ParticleTypes.CRIMSON_SPORE),
    WARPED_SPORE("warped_spore", ParticleTypes.WARPED_SPORE),
    SNOWFLAKE("snowflake", ParticleTypes.SNOWFLAKE),
    MYCELIUM("mycelium", ParticleTypes.MYCELIUM),

    HEART("heart", ParticleTypes.HEART),
    HAPPY_VILLAGER("happy_villager", ParticleTypes.HAPPY_VILLAGER),
    NOTE("note", ParticleTypes.NOTE);

    private static final Map<String, StaticDecorationEffect> LEGACY_ALIASES = Map.ofEntries(
            Map.entry("spark", ELECTRIC_SPARK),
            Map.entry("soul_flame", SOUL_FIRE_FLAME),
            Map.entry("bubble", SPORE_BLOSSOM_AIR),
            Map.entry("star", END_ROD),
            Map.entry("rune", ENCHANT),
            Map.entry("leaf", CHERRY_LEAVES),
            Map.entry("drop", GLOW)
    );

    private final String id;
    private final SimpleParticleType particle;
    private final Identifier particleJson;

    StaticDecorationEffect(String id, SimpleParticleType particle) {
        this.id = id;
        this.particle = particle;
        this.particleJson = Identifier.of("minecraft", "particles/" + id + ".json");
    }

    public String id() {
        return id;
    }

    public SimpleParticleType particle() {
        return particle;
    }

    public Identifier particleJson() {
        return particleJson;
    }

    public String translationKey() {
        return "static_decoration.mythicrpg." + id;
    }

    /** Balanced client emission interval; larger vanilla effects emit less often. */
    public int intervalTicks() {
        return switch (this) {
            case CAMPFIRE_COSY_SMOKE -> 12;
            case HEART, NOTE, TOTEM_OF_UNDYING, FIREWORK -> 8;
            case PORTAL, REVERSE_PORTAL, ENCHANT, SPORE_BLOSSOM_AIR -> 4;
            default -> 6;
        };
    }

    public int particlesPerEmission() {
        return 1;
    }

    public StaticDecorationEffect next() {
        StaticDecorationEffect[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public StaticDecorationEffect previous() {
        StaticDecorationEffect[] values = values();
        return values[Math.floorMod(ordinal() - 1, values.length)];
    }

    public static Optional<StaticDecorationEffect> byId(String id) {
        if (id == null || id.isBlank()) return Optional.empty();
        StaticDecorationEffect legacy = LEGACY_ALIASES.get(id);
        if (legacy != null) return Optional.of(legacy);
        return Arrays.stream(values()).filter(value -> value.id.equals(id)).findFirst();
    }

    public static StaticDecorationEffect byIndex(int index) {
        StaticDecorationEffect[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
