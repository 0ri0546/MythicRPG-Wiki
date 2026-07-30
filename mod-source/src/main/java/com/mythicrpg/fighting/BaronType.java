package com.mythicrpg.fighting;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;

public enum BaronType {
    NORMAL("mythicrpg_baron_normal", Formatting.DARK_RED),

    DRUID("mythicrpg_baron_druid", Formatting.RED),
    BARRAGE("mythicrpg_baron_barrage", Formatting.GOLD),

    NUKE("mythicrpg_baron_nuke", Formatting.DARK_PURPLE),
    SURVIVOR("mythicrpg_baron_survivor", Formatting.DARK_GREEN),

    FUGITIVE("mythicrpg_baron_fugitive", Formatting.YELLOW),
    GOLDEN("mythicrpg_baron_golden", Formatting.GOLD),
    PANIC("mythicrpg_baron_panic", Formatting.LIGHT_PURPLE),

    GIANT("mythicrpg_baron_giant", Formatting.GREEN),
    DARKNIGHT("mythicrpg_baron_darknight", Formatting.DARK_GRAY),
    ALCHEMIST("mythicrpg_baron_alchemist", Formatting.DARK_PURPLE),
    HOTHEAD("mythicrpg_baron_hothead", Formatting.RED),
    SWIMMING("mythicrpg_baron_swimming", Formatting.AQUA),
    DROWNED_KING("mythicrpg_baron_drowned_king", Formatting.DARK_AQUA),
    BALLOON("mythicrpg_baron_balloon", Formatting.LIGHT_PURPLE),
    CHARGING("mythicrpg_baron_charging", Formatting.DARK_RED),

    DIAMOND("mythicrpg_baron_diamond", Formatting.AQUA),
    STALKER("mythicrpg_baron_stalker", Formatting.DARK_GRAY),
    HEAVY("mythicrpg_baron_heavy", Formatting.DARK_GREEN),
    MOLTEN("mythicrpg_baron_molten", Formatting.GOLD),

    RUNNER("mythicrpg_baron_runner", Formatting.GREEN),
    INK("mythicrpg_baron_ink", Formatting.DARK_AQUA),
    UNDYING_WOLF("mythicrpg_baron_undying_wolf", Formatting.BLUE),

    INFERNO("mythicrpg_baron_inferno", Formatting.RED),
    THROWER("mythicrpg_baron_thrower", Formatting.DARK_PURPLE);

    private static final Map<String, BaronType> BY_TAG = new HashMap<>();

    static {
        for (BaronType type : values()) {
            BY_TAG.put(type.tag, type);
        }

        // Backward compatibility for Barons spawned before the v0.3 rename.
        BY_TAG.put("mythicrpg_baron_guardian_wolf", UNDYING_WOLF);
    }

    private final String tag;
    private final Formatting color;

    BaronType(String tag, Formatting color) {
        this.tag = tag;
        this.color = color;
    }

    public String tag() {
        return tag;
    }

    public String translationKey() {
        return "baron.mythicrpg." + name().toLowerCase(java.util.Locale.ROOT);
    }

    public Text createName(Text mobName) {
        return Text.translatable("baron.mythicrpg.display", Text.translatable(translationKey()), mobName)
                .formatted(color, Formatting.BOLD);
    }

    public static BaronType fromEntityTags(Iterable<String> tags) {
        for (String tag : tags) {
            BaronType type = BY_TAG.get(tag);

            if (type != null) {
                return type;
            }
        }

        return NORMAL;
    }
}