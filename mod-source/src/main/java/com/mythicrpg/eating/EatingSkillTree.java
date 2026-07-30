package com.mythicrpg.eating;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.Perk;
import com.mythicrpg.core.SkillTreeNode;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

public final class EatingSkillTree {
    private EatingSkillTree() {
    }

    public static Map<Integer, SkillTreeNode> buildTree() {
        Map<Integer, SkillTreeNode> nodes = new HashMap<>();

        nodes.put(1, new SkillTreeNode(
                1,
                "skill_tree.mythicrpg.eating.1.name",
                "skill_tree.mythicrpg.eating.1.description",
                112,
                10,
                List.of(),
                -1,
                -1,
                stub(),
                bonus(BonusType.EATING_COOKING, 1.0)
        ));

        nodes.put(2, new SkillTreeNode(
                2,
                "skill_tree.mythicrpg.eating.2.name",
                "skill_tree.mythicrpg.eating.2.description",
                22,
                45,
                List.of(1),
                1,
                0,
                stub(),
                bonus(BonusType.EATING_POT_SLOTS, 1.0)
        ));

        nodes.put(3, new SkillTreeNode(
                3,
                "skill_tree.mythicrpg.eating.3.name",
                "skill_tree.mythicrpg.eating.3.description",
                22,
                75,
                List.of(2),
                1,
                0,
                stub(),
                bonus(BonusType.EATING_POT_SLOTS, 1.0)
        ));

        nodes.put(4, new SkillTreeNode(
                4,
                "skill_tree.mythicrpg.eating.4.name",
                "skill_tree.mythicrpg.eating.4.description",
                22,
                105,
                List.of(3),
                1,
                0,
                stub(),
                bonus(BonusType.EATING_POT_SLOTS, 1.0)
        ));

        nodes.put(5, new SkillTreeNode(
                5,
                "skill_tree.mythicrpg.eating.5.name",
                "skill_tree.mythicrpg.eating.5.description",
                202,
                45,
                List.of(1),
                1,
                1,
                stub(),
                bonus(BonusType.EATING_SMALL_PLATE, 1.0)
        ));

        nodes.put(6, new SkillTreeNode(
                6,
                "skill_tree.mythicrpg.eating.6.name",
                "skill_tree.mythicrpg.eating.6.description",
                202,
                75,
                List.of(5),
                1,
                1,
                stub(),
                bonus(BonusType.EATING_MEDIUM_PLATE, 1.0)
        ));

        nodes.put(7, new SkillTreeNode(
                7,
                "skill_tree.mythicrpg.eating.7.name",
                "skill_tree.mythicrpg.eating.7.description",
                202,
                105,
                List.of(6),
                1,
                1,
                stub(),
                bonus(BonusType.EATING_LARGE_PLATE, 1.0)
        ));

        nodes.put(8, new SkillTreeNode(
                8,
                "skill_tree.mythicrpg.eating.8.name",
                "skill_tree.mythicrpg.eating.8.description",
                112,
                140,
                List.of(4, 7),
                -1,
                -1,
                stub(),
                bonus(BonusType.EATING_FRIDGE, 1.0)
        ));

        nodes.put(9, new SkillTreeNode(
                9,
                "skill_tree.mythicrpg.eating.9.name",
                "skill_tree.mythicrpg.eating.9.description",
                2,
                175,
                List.of(8),
                2,
                0,
                stub(),
                bonus(BonusType.EATING_WHEN_FULL, 1.0)
        ));

        nodes.put(10, new SkillTreeNode(
                10,
                "skill_tree.mythicrpg.eating.10.name",
                "skill_tree.mythicrpg.eating.10.description",
                2,
                205,
                List.of(9),
                2,
                0,
                stub(),
                bonus(BonusType.EATING_CHEF_AURA, 1.0)
        ));

        nodes.put(11, new SkillTreeNode(
                11,
                "skill_tree.mythicrpg.eating.11.name",
                "skill_tree.mythicrpg.eating.11.description",
                2,
                235,
                List.of(10),
                2,
                0,
                stub(),
                bonus(BonusType.EATING_COMPLETE_MEAL, 1.0)
        ));

        nodes.put(12, new SkillTreeNode(
                12,
                "skill_tree.mythicrpg.eating.12.name",
                "skill_tree.mythicrpg.eating.12.description",
                112,
                175,
                List.of(8),
                2,
                1,
                stub(),
                bonus(BonusType.EATING_RISK_TASTE, 1.0)
        ));

        nodes.put(13, new SkillTreeNode(
                13,
                "skill_tree.mythicrpg.eating.13.name",
                "skill_tree.mythicrpg.eating.13.description",
                112,
                205,
                List.of(12),
                2,
                1,
                stub(),
                bonus(BonusType.EATING_DELIVERY, 1.0)
        ));

        nodes.put(14, new SkillTreeNode(
                14,
                "skill_tree.mythicrpg.eating.14.name",
                "skill_tree.mythicrpg.eating.14.description",
                112,
                235,
                List.of(13),
                2,
                1,
                stub(),
                bonus(BonusType.EATING_INTERNATIONAL_GASTRONOMY, 1.0)
        ));

        nodes.put(15, new SkillTreeNode(
                15,
                "skill_tree.mythicrpg.eating.15.name",
                "skill_tree.mythicrpg.eating.15.description",
                222,
                175,
                List.of(8),
                2,
                2,
                stub(),
                bonus(BonusType.EATING_DUBIOUS_COMPOST, 1.0)
        ));

        nodes.put(16, new SkillTreeNode(
                16,
                "skill_tree.mythicrpg.eating.16.name",
                "skill_tree.mythicrpg.eating.16.description",
                222,
                205,
                List.of(15),
                2,
                2,
                stub(),
                bonus(BonusType.EATING_CHEF_RENOWN, 1.0)
        ));

        nodes.put(17, new SkillTreeNode(
                17,
                "skill_tree.mythicrpg.eating.17.name",
                "skill_tree.mythicrpg.eating.17.description",
                222,
                235,
                List.of(16),
                2,
                2,
                stub(),
                bonus(BonusType.EATING_RARITY_UP, 1.0)
        ));

        nodes.put(18, new SkillTreeNode(
                18,
                "skill_tree.mythicrpg.eating.18.name",
                "skill_tree.mythicrpg.eating.18.description",
                112,
                265,
                List.of(11, 14, 17),
                -1,
                -1,
                stub(),
                bonus(BonusType.EATING_SIGNATURE_DISH, 1.0)
        ));

        nodes.put(19, new SkillTreeNode(
                19,
                "skill_tree.mythicrpg.eating.19.name",
                "skill_tree.mythicrpg.eating.19.description",
                62,
                295,
                List.of(18),
                3,
                0,
                stub(),
                bonus(BonusType.EATING_PORTABLE_FRIDGE, 1.0)
        ));

        nodes.put(20, new SkillTreeNode(
                20,
                "skill_tree.mythicrpg.eating.20.name",
                "skill_tree.mythicrpg.eating.20.description",
                162,
                295,
                List.of(18),
                3,
                1,
                stub(),
                bonus(BonusType.EATING_AUTO_FEED, 1.0)
        ));

        return nodes;
    }

    private static SkillTreeNode node(
            int id,
            int x,
            int y,
            List<Integer> parents,
            int fork,
            int branch,
            BonusType bonus,
            double value
    ) {
        return new SkillTreeNode(
                id,
                "skill_tree.mythicrpg.eating." + id + ".name",
                "skill_tree.mythicrpg.eating." + id + ".description",
                x,
                y,
                parents,
                fork,
                branch,
                stub(),
                Map.of(bonus, value)
        );
    }

    private static Perk stub() {
        return player -> { };
    }

    private static Map<BonusType, Double> bonus(BonusType type, double value) {
        return Map.of(type, value);
    }
}
