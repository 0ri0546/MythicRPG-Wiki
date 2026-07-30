package com.mythicrpg.titles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum TitleFinish {
    NONE("none", "title_finish.mythicrpg.none"),
    BOLD("bold", "title_finish.mythicrpg.bold"),
    ITALIC("italic", "title_finish.mythicrpg.italic"),
    BORDER("border", "title_finish.mythicrpg.border"),
    UNIFORM_FONT("uniform_font", "title_finish.mythicrpg.uniform_font"),
    UPPERCASE("uppercase", "title_finish.mythicrpg.uppercase");

    public static final List<TitleFinish> DISPLAY_ORDER = List.of(
            NONE,
            BOLD,
            ITALIC,
            BORDER,
            UNIFORM_FONT,
            UPPERCASE
    );

    private final String id;
    private final String translationKey;

    TitleFinish(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public static Optional<TitleFinish> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(finish -> finish.id.equalsIgnoreCase(id))
                .findFirst();
    }
}
