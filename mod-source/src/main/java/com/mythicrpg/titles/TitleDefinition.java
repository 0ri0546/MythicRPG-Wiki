package com.mythicrpg.titles;

import com.mythicrpg.core.MythicLanguageResolver;
import com.mythicrpg.core.SkillType;

import java.util.Objects;

public record TitleDefinition(
        String id,
        String translationKey,
        TitleCategory category,
        int globalLevelRequirement,
        SkillType skillRequirement,
        int skillLevelRequirement,
        boolean selectable
) {
    public TitleDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(translationKey, "translationKey");
        Objects.requireNonNull(category, "category");
        if (id.isBlank()) {
            throw new IllegalArgumentException("A title id cannot be blank");
        }
    }

    public boolean isAutomaticallyUnlocked() {
        return globalLevelRequirement > 0 || skillRequirement != null;
    }

    public String localizedLiteral(String languageCode) {
        return MythicLanguageResolver.resolve(languageCode, translationKey);
    }
}
