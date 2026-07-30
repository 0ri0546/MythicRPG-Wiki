package com.mythicrpg.mining;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.Perk;
import com.mythicrpg.core.SkillTreeNode;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

public class MiningSkillTree {
    public static Map<Integer, SkillTreeNode> buildTree() {
        Map<Integer, SkillTreeNode> nodes = new HashMap<>();

        nodes.put(1, new SkillTreeNode(1, "skill_tree.mythicrpg.mining.1.name", "skill_tree.mythicrpg.mining.1.description", 112, 10, List.of(), -1, -1, stub(), bonus(BonusType.VEIN_MINING, 1.0)));

        nodes.put(2, new SkillTreeNode(2, "skill_tree.mythicrpg.mining.2.name", "skill_tree.mythicrpg.mining.2.description", 22, 45, List.of(1), 1, 0, stub(), bonus(BonusType.FOSSIL_EXCAVATION, 1.0)));
        nodes.put(3, new SkillTreeNode(3, "skill_tree.mythicrpg.mining.3.name", "skill_tree.mythicrpg.mining.3.description", 22, 75, List.of(2), 1, 0, stub(), bonus(BonusType.FOSSIL_INCUBATION, 1.0)));
        nodes.put(4, new SkillTreeNode(4, "skill_tree.mythicrpg.mining.4.name", "skill_tree.mythicrpg.mining.4.description", 22, 105, List.of(3), 1, 0, stub(), bonus(BonusType.FOSSIL_ARCHAEOLOGIST, 1.0)));

        nodes.put(5, new SkillTreeNode(5, "skill_tree.mythicrpg.mining.5.name", "skill_tree.mythicrpg.mining.5.description", 202, 45, List.of(1), 1, 1, stub(), bonus(BonusType.ORE_HIGHLIGHT_RADIUS, 1.0)));
        nodes.put(6, new SkillTreeNode(6, "skill_tree.mythicrpg.mining.6.name", "skill_tree.mythicrpg.mining.6.description", 202, 75, List.of(5), 1, 1, stub(), bonus(BonusType.ORE_HIGHLIGHT_RADIUS, 2.0)));
        nodes.put(7, new SkillTreeNode(7, "skill_tree.mythicrpg.mining.7.name", "skill_tree.mythicrpg.mining.7.description", 202, 105, List.of(6), 1, 1, stub(), bonus(BonusType.ORE_HIGHLIGHT_RADIUS, 3.0)));

        nodes.put(8, new SkillTreeNode(8, "skill_tree.mythicrpg.mining.8.name", "skill_tree.mythicrpg.mining.8.description", 112, 140, List.of(4, 7), -1, -1, stub(), Map.of(), effect(StatusEffects.HASTE, 0)));

        nodes.put(9, new SkillTreeNode(9, "skill_tree.mythicrpg.mining.9.name", "skill_tree.mythicrpg.mining.9.description", 2, 175, List.of(8), 2, 0, stub(), bonus(BonusType.DROP_MULTIPLIER, 0.10)));
        nodes.put(10, new SkillTreeNode(10, "skill_tree.mythicrpg.mining.10.name", "skill_tree.mythicrpg.mining.10.description", 2, 205, List.of(9), 2, 0, stub(), bonus(BonusType.DROP_MULTIPLIER, 0.20)));
        nodes.put(11, new SkillTreeNode(11, "skill_tree.mythicrpg.mining.11.name", "skill_tree.mythicrpg.mining.11.description", 2, 235, List.of(10), 2, 0, stub(), bonus(BonusType.DROP_MULTIPLIER, 0.30)));

        nodes.put(12, new SkillTreeNode(12, "skill_tree.mythicrpg.mining.12.name", "skill_tree.mythicrpg.mining.12.description", 112, 175, List.of(8), 2, 1, stub(), bonus(BonusType.XP_MULTIPLIER, 0.20)));
        nodes.put(13, new SkillTreeNode(13, "skill_tree.mythicrpg.mining.13.name", "skill_tree.mythicrpg.mining.13.description", 112, 205, List.of(12), 2, 1, stub(), bonus(BonusType.XP_MULTIPLIER, 0.35)));
        nodes.put(14, new SkillTreeNode(14, "skill_tree.mythicrpg.mining.14.name", "skill_tree.mythicrpg.mining.14.description", 112, 235, List.of(13), 2, 1, stub(), bonus(BonusType.XP_MULTIPLIER, 0.45)));

        nodes.put(15, new SkillTreeNode(15, "skill_tree.mythicrpg.mining.15.name", "skill_tree.mythicrpg.mining.15.description", 222, 175, List.of(8), 2, 2, stub(), Map.of(), effect(StatusEffects.FIRE_RESISTANCE, 0)));
        nodes.put(16, new SkillTreeNode(16, "skill_tree.mythicrpg.mining.16.name", "skill_tree.mythicrpg.mining.16.description", 222, 205, List.of(15), 2, 2, stub(), bonus(BonusType.NO_FALL_DAMAGE, 1.0)));
        nodes.put(17, new SkillTreeNode(17, "skill_tree.mythicrpg.mining.17.name", "skill_tree.mythicrpg.mining.17.description", 222, 235, List.of(16), 2, 2, stub(), Map.of(), effect(StatusEffects.WATER_BREATHING, 0)));

        nodes.put(18, new SkillTreeNode(18, "skill_tree.mythicrpg.mining.18.name", "skill_tree.mythicrpg.mining.18.description", 112, 265, List.of(11, 14, 17), -1, -1, stub(), bonus(BonusType.MINING_3X3, 1.0)));

        nodes.put(19, new SkillTreeNode(19, "skill_tree.mythicrpg.mining.19.name", "skill_tree.mythicrpg.mining.19.description", 62, 295, List.of(18), 3, 0, stub(), Map.of(), effect(StatusEffects.HASTE, 1)));

        nodes.put(20, new SkillTreeNode(20, "skill_tree.mythicrpg.mining.20.name", "skill_tree.mythicrpg.mining.20.description", 162, 295, List.of(18), 3, 1, stub(), bonus(BonusType.NO_DURABILITY_LOSS, 1.0)));

        return nodes;
    }

    private static Perk stub() {
        return player -> {
            // Intentionally empty.
            // Actual behavior is handled by events / managers.
        };
    }

    private static Map<BonusType, Double> bonus(BonusType type, double value) {
        return Map.of(type, value);
    }

    private static Map<RegistryEntry<StatusEffect>, Integer> effect(RegistryEntry<StatusEffect> type, int amplifier) {
        return Map.of(type, amplifier);
    }
}