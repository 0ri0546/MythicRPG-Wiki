package com.mythicrpg.titles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record TitleProfile(
        String activeTitleId,
        String primaryColorId,
        String secondaryColorId,
        boolean gradient,
        String finishId,
        Set<String> unlockedTitleIds
) {
    private static final Codec<Set<String>> STRING_SET_CODEC = Codec.STRING.listOf().xmap(
            values -> Set.copyOf(new LinkedHashSet<>(values)),
            values -> List.copyOf(values)
    );

    public static final Codec<TitleProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("active_title", "").forGetter(TitleProfile::activeTitleId),
            Codec.STRING.optionalFieldOf("primary_color", TitleColor.WHITE.id()).forGetter(TitleProfile::primaryColorId),
            Codec.STRING.optionalFieldOf("secondary_color", TitleColor.WHITE.id()).forGetter(TitleProfile::secondaryColorId),
            Codec.BOOL.optionalFieldOf("gradient", false).forGetter(TitleProfile::gradient),
            Codec.STRING.optionalFieldOf("finish", TitleFinish.NONE.id()).forGetter(TitleProfile::finishId),
            STRING_SET_CODEC.optionalFieldOf("unlocked_titles", Set.of()).forGetter(TitleProfile::unlockedTitleIds)
    ).apply(instance, TitleProfile::new));

    public TitleProfile {
        activeTitleId = activeTitleId == null ? "" : activeTitleId;
        primaryColorId = primaryColorId == null ? TitleColor.WHITE.id() : primaryColorId;
        secondaryColorId = secondaryColorId == null ? TitleColor.WHITE.id() : secondaryColorId;
        finishId = finishId == null ? TitleFinish.NONE.id() : finishId;
        unlockedTitleIds = Set.copyOf(unlockedTitleIds == null ? Set.of() : unlockedTitleIds);
    }

    public static TitleProfile defaults() {
        return new TitleProfile(
                "",
                TitleColor.WHITE.id(),
                TitleColor.WHITE.id(),
                false,
                TitleFinish.NONE.id(),
                Set.of()
        );
    }

    public TitleProfile withUnlockedTitles(Set<String> unlocked) {
        return new TitleProfile(
                activeTitleId,
                primaryColorId,
                secondaryColorId,
                gradient,
                finishId,
                unlocked
        );
    }

    public TitleProfile withSelection(
            String titleId,
            TitleColor primary,
            TitleColor secondary,
            boolean useGradient,
            TitleFinish finish
    ) {
        return new TitleProfile(
                titleId,
                primary.id(),
                useGradient ? secondary.id() : primary.id(),
                useGradient,
                finish.id(),
                unlockedTitleIds
        );
    }
}
