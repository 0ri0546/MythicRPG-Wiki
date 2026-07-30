package com.mythicrpg.core;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.List;
import java.util.Map;

public class SkillTreeNode {

    private final int id;
    private final String nameTranslationKey;
    private final String descriptionTranslationKey;
    private final int x;
    private final int y;
    private final List<Integer> parentIds;
    private final int forkId;
    private final int branchId;
    private final Perk perk;

    private final Map<BonusType, Double> bonuses;
    private final Map<RegistryEntry<StatusEffect>, Integer> passiveEffects;

    // Nouveau
    private final PoisonOnHit poisonOnHit;

    public SkillTreeNode(
            int id,
            String nameTranslationKey,
            String descriptionTranslationKey,
            int x,
            int y,
            List<Integer> parentIds,
            int forkId,
            int branchId,
            Perk perk
    ) {
        this(
                id,
                nameTranslationKey,
                descriptionTranslationKey,
                x,
                y,
                parentIds,
                forkId,
                branchId,
                perk,
                Map.of(),
                Map.of(),
                null
        );
    }

    public SkillTreeNode(
            int id,
            String nameTranslationKey,
            String descriptionTranslationKey,
            int x,
            int y,
            List<Integer> parentIds,
            int forkId,
            int branchId,
            Perk perk,
            Map<BonusType, Double> bonuses
    ) {
        this(
                id,
                nameTranslationKey,
                descriptionTranslationKey,
                x,
                y,
                parentIds,
                forkId,
                branchId,
                perk,
                bonuses,
                Map.of(),
                null
        );
    }

    public SkillTreeNode(
            int id,
            String nameTranslationKey,
            String descriptionTranslationKey,
            int x,
            int y,
            List<Integer> parentIds,
            int forkId,
            int branchId,
            Perk perk,
            Map<BonusType, Double> bonuses,
            Map<RegistryEntry<StatusEffect>, Integer> passiveEffects
    ) {
        this(
                id,
                nameTranslationKey,
                descriptionTranslationKey,
                x,
                y,
                parentIds,
                forkId,
                branchId,
                perk,
                bonuses,
                passiveEffects,
                null
        );
    }

    // Nouveau constructeur complet
    public SkillTreeNode(
            int id,
            String nameTranslationKey,
            String descriptionTranslationKey,
            int x,
            int y,
            List<Integer> parentIds,
            int forkId,
            int branchId,
            Perk perk,
            Map<BonusType, Double> bonuses,
            Map<RegistryEntry<StatusEffect>, Integer> passiveEffects,
            PoisonOnHit poisonOnHit
    ) {
        this.id = id;
        this.nameTranslationKey = nameTranslationKey;
        this.descriptionTranslationKey = descriptionTranslationKey;
        this.x = x;
        this.y = y;
        this.parentIds = parentIds;
        this.forkId = forkId;
        this.branchId = branchId;
        this.perk = perk;
        this.bonuses = bonuses;
        this.passiveEffects = passiveEffects;
        this.poisonOnHit = poisonOnHit;
    }

    public int getId() {
        return id;
    }

    public String getNameTranslationKey() {
        return nameTranslationKey;
    }

    public String getDescriptionTranslationKey() {
        return descriptionTranslationKey;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public List<Integer> getParentIds() {
        return parentIds;
    }

    public int getForkId() {
        return forkId;
    }

    public int getBranchId() {
        return branchId;
    }

    public Perk getPerk() {
        return perk;
    }

    public Map<BonusType, Double> getBonuses() {
        return bonuses;
    }

    public Map<RegistryEntry<StatusEffect>, Integer> getPassiveEffects() {
        return passiveEffects;
    }

    public PoisonOnHit getPoisonOnHit() {
        return poisonOnHit;
    }

    public boolean isRoot() {
        return parentIds.isEmpty();
    }
}