package com.mythicrpg.titles;

import java.util.Arrays;
import java.util.Optional;

public enum TitleColor {
    WHITE("white", 0xFFFFFF, "title_color.mythicrpg.white"),
    RED("red", 0xFF4D4D, "title_color.mythicrpg.red"),
    ORANGE("orange", 0xFF9F1C, "title_color.mythicrpg.orange"),
    YELLOW("yellow", 0xFFD93D, "title_color.mythicrpg.yellow"),
    GREEN("green", 0x55E06F, "title_color.mythicrpg.green"),
    CYAN("cyan", 0x40DFFF, "title_color.mythicrpg.cyan"),
    BLUE("blue", 0x4D7CFE, "title_color.mythicrpg.blue"),
    VIOLET("violet", 0xB65CFF, "title_color.mythicrpg.violet");

    private final String id;
    private final int rgb;
    private final String translationKey;

    TitleColor(String id, int rgb, String translationKey) {
        this.id = id;
        this.rgb = rgb;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public int rgb() {
        return rgb;
    }

    public String translationKey() {
        return translationKey;
    }

    public static Optional<TitleColor> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(color -> color.id.equalsIgnoreCase(id))
                .findFirst();
    }
}
