package com.mythicrpg.titles;

import com.mythicrpg.core.SkillType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TitleRegistry {
    public static final String FOOD_BACKPACK_TEN_DEATHS_ID = "special_food_backpack_10_deaths";
    public static final String PSYCHOPATH_ID = "special_psychopath";
    public static final String SEA_HUNTER_NESSIE_ID = "special_sea_hunter_nessie";
    public static final String SEA_HUNTER_MEGALODON_ID = "special_sea_hunter_megalodon";
    public static final String SEA_HUNTER_WHALE_ID = "special_sea_hunter_whale";

    private static final Map<String, TitleDefinition> BY_ID = new LinkedHashMap<>();
    private static final List<TitleDefinition> ORDERED;

    static {
        registerGlobal(100);
        registerGlobal(200);
        registerGlobal(300);
        registerGlobal(400);
        registerGlobal(500);
        registerGlobal(600);
        registerGlobal(700);
        registerGlobal(800);
        registerGlobal(900);

        registerSkill(SkillType.MINING);
        registerSkill(SkillType.FIGHTING);
        registerSkill(SkillType.WOODCUTTING);
        registerSkill(SkillType.FARMING);
        registerSkill(SkillType.CRAFTING);
        registerSkill(SkillType.TRAVELING);
        registerSkill(SkillType.BUILDING);
        registerSkill(SkillType.FISHING);
        registerSkill(SkillType.EATING);

        // Stable, selectable placeholder. Its public wording remains temporary until
        // the dedicated special-title design pass.
        register(new TitleDefinition(
                FOOD_BACKPACK_TEN_DEATHS_ID,
                "title.mythicrpg.special.food_backpack_10_deaths",
                TitleCategory.SPECIAL,
                0,
                null,
                0,
                true
        ));

        register(new TitleDefinition(
                PSYCHOPATH_ID,
                "title.mythicrpg.special.psychopath",
                TitleCategory.SPECIAL,
                0,
                null,
                0,
                true
        ));

        registerSeaHunter(SEA_HUNTER_NESSIE_ID, "title.mythicrpg.special.sea_hunter_nessie");
        registerSeaHunter(SEA_HUNTER_MEGALODON_ID, "title.mythicrpg.special.sea_hunter_megalodon");
        registerSeaHunter(SEA_HUNTER_WHALE_ID, "title.mythicrpg.special.sea_hunter_whale");

        ORDERED = Collections.unmodifiableList(new ArrayList<>(BY_ID.values()));
    }

    private TitleRegistry() {
    }

    private static void registerSeaHunter(String id, String translationKey) {
        register(new TitleDefinition(
                id,
                translationKey,
                TitleCategory.SPECIAL,
                0,
                null,
                0,
                true
        ));
    }

    private static void registerGlobal(int level) {
        register(new TitleDefinition(
                "global_" + level,
                "title.mythicrpg.global." + level,
                TitleCategory.GLOBAL,
                level,
                null,
                0,
                true
        ));
    }

    private static void registerSkill(SkillType skill) {
        String id = "skill_" + skill.name().toLowerCase(java.util.Locale.ROOT);
        register(new TitleDefinition(
                id,
                "title.mythicrpg.skill." + skill.name().toLowerCase(java.util.Locale.ROOT),
                TitleCategory.SKILL,
                0,
                skill,
                100,
                true
        ));
    }

    private static void register(TitleDefinition definition) {
        if (BY_ID.putIfAbsent(definition.id(), definition) != null) {
            throw new IllegalStateException("Duplicate title id: " + definition.id());
        }
    }

    public static Optional<TitleDefinition> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static List<TitleDefinition> all() {
        return ORDERED;
    }

    public static List<TitleDefinition> selectableUnlocked(Set<String> unlockedIds) {
        return ORDERED.stream()
                .filter(TitleDefinition::selectable)
                .filter(definition -> unlockedIds.contains(definition.id()))
                .toList();
    }

    public static int selectableCount() {
        return (int) ORDERED.stream().filter(TitleDefinition::selectable).count();
    }
}
