package com.mythicrpg.farming;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.Perk;
import com.mythicrpg.core.SkillTreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FarmingSkillTree {

    private FarmingSkillTree() {
    }

    public static Map<Integer, SkillTreeNode> buildTree() {
        Map<Integer, SkillTreeNode> nodes = new HashMap<>();

        nodes.put(1, new SkillTreeNode(1, "skill_tree.mythicrpg.farming.1.name", "skill_tree.mythicrpg.farming.1.description",
                112,
                10,
                List.of(),
                -1,
                -1,
                stub(),
                bonus(BonusType.ENCHANTED_SEED_CHANCE, 0.001)
        ));

        nodes.put(2, new SkillTreeNode(2, "skill_tree.mythicrpg.farming.2.name", "skill_tree.mythicrpg.farming.2.description",
                22,
                45,
                List.of(1),
                1,
                0,
                stub(),
                bonus(BonusType.AUTO_REPLANT, 1.0)
        ));

        nodes.put(3, new SkillTreeNode(3, "skill_tree.mythicrpg.farming.3.name", "skill_tree.mythicrpg.farming.3.description",
                22,
                75,
                List.of(2),
                1,
                0,
                stub(),
                bonus(BonusType.FARMING_DOUBLE_DROP_CHANCE, 0.15)
        ));

        nodes.put(4, new SkillTreeNode(4, "skill_tree.mythicrpg.farming.4.name", "skill_tree.mythicrpg.farming.4.description",
                22,
                105,
                List.of(3),
                1,
                0,
                stub(),
                bonus(BonusType.FARMING_DOUBLE_DROP_CHANCE, 0.25)
        ));

        nodes.put(5, new SkillTreeNode(5, "skill_tree.mythicrpg.farming.5.name", "skill_tree.mythicrpg.farming.5.description",
                202,
                45,
                List.of(1),
                1,
                1,
                stub(),
                bonus(BonusType.COMPOST_RARE_DROP_CHANCE, 0.01)
        ));

        nodes.put(6, new SkillTreeNode(6, "skill_tree.mythicrpg.farming.6.name", "skill_tree.mythicrpg.farming.6.description",
                202,
                75,
                List.of(5),
                1,
                1,
                stub(),
                bonus(BonusType.COMPOST_RARE_DROP_CHANCE, 0.02)
        ));

        nodes.put(7, new SkillTreeNode(7, "skill_tree.mythicrpg.farming.7.name", "skill_tree.mythicrpg.farming.7.description",
                202,
                105,
                List.of(6),
                1,
                1,
                stub(),
                bonus(BonusType.COMPOST_RARE_DROP_CHANCE, 0.03)
        ));

        nodes.put(8, new SkillTreeNode(8, "skill_tree.mythicrpg.farming.8.name", "skill_tree.mythicrpg.farming.8.description",
                112,
                140,
                List.of(4, 7),
                -1,
                -1,
                stub(),
                bonus(BonusType.FOOD_BACKPACK_CRAFT, 1.0)
        ));

        nodes.put(9, new SkillTreeNode(9, "skill_tree.mythicrpg.farming.9.name", "skill_tree.mythicrpg.farming.9.description",
                2,
                175,
                List.of(8),
                2,
                0,
                stub(),
                bonus(BonusType.FARMING_VANILLA_XP, 1.0)
        ));

        nodes.put(10, new SkillTreeNode(10, "skill_tree.mythicrpg.farming.10.name", "skill_tree.mythicrpg.farming.10.description",
                2,
                205,
                List.of(9),
                2,
                0,
                stub(),
                bonus(BonusType.ENCHANTED_SEED_CHANCE, 0.002)
        ));

        nodes.put(11, new SkillTreeNode(11, "skill_tree.mythicrpg.farming.11.name", "skill_tree.mythicrpg.farming.11.description",
                2,
                235,
                List.of(10),
                2,
                0,
                stub(),
                bonus(BonusType.ENCHANTED_SEED_CHANCE, 0.003)
        ));

        nodes.put(12, new SkillTreeNode(12, "skill_tree.mythicrpg.farming.12.name", "skill_tree.mythicrpg.farming.12.description",
                112,
                175,
                List.of(8),
                2,
                1,
                stub(),
                bonus(BonusType.BONE_MEAL_REGEN, 1.0)
        ));

        nodes.put(13, new SkillTreeNode(13, "skill_tree.mythicrpg.farming.13.name", "skill_tree.mythicrpg.farming.13.description",
                112,
                205,
                List.of(12),
                2,
                1,
                stub(),
                bonus(BonusType.IRRIGATED_STEP, 1.0)
        ));

        nodes.put(14, new SkillTreeNode(14, "skill_tree.mythicrpg.farming.14.name", "skill_tree.mythicrpg.farming.14.description",
                112,
                235,
                List.of(13),
                2,
                1,
                stub(),
                bonus(BonusType.CULTIVATED_SHIELD, 1.0)
        ));

        nodes.put(15, new SkillTreeNode(15, "skill_tree.mythicrpg.farming.15.name", "skill_tree.mythicrpg.farming.15.description",
                222,
                175,
                List.of(8),
                2,
                2,
                stub(),
                bonus(BonusType.FARMER_REACH_RADIUS, 3.0)
        ));

        nodes.put(16, new SkillTreeNode(16, "skill_tree.mythicrpg.farming.16.name", "skill_tree.mythicrpg.farming.16.description",
                222,
                205,
                List.of(15),
                2,
                2,
                stub(),
                bonus(BonusType.FARMER_REACH_RADIUS, 5.0)
        ));

        nodes.put(17, new SkillTreeNode(17, "skill_tree.mythicrpg.farming.17.name", "skill_tree.mythicrpg.farming.17.description",
                222,
                235,
                List.of(16),
                2,
                2,
                stub(),
                bonus(BonusType.FARMER_REACH_RADIUS, 7.0)
        ));

        nodes.put(18, new SkillTreeNode(18, "skill_tree.mythicrpg.farming.18.name", "skill_tree.mythicrpg.farming.18.description",
                112,
                265,
                List.of(11, 14, 17),
                -1,
                -1,
                stub(),
                bonus(BonusType.ENCHANTED_FLOWER_CRAFT, 1.0)
        ));

        nodes.put(19, new SkillTreeNode(19, "skill_tree.mythicrpg.farming.19.name", "skill_tree.mythicrpg.farming.19.description",
                62,
                295,
                List.of(18),
                3,
                0,
                stub(),
                bonus(BonusType.PRESERVED_FARMER, 1.0)
        ));

        nodes.put(20, new SkillTreeNode(20, "skill_tree.mythicrpg.farming.20.name", "skill_tree.mythicrpg.farming.20.description",
                162,
                295,
                List.of(18),
                3,
                1,
                stub(),
                bonus(BonusType.LIVING_FIELD, 1.0)
        ));

        return nodes;
    }

    private static Perk stub() {
        return player -> {
            // Intentionally empty.
            // Actual behavior will be implemented in FarmingEvents / managers.
        };
    }

    private static Map<BonusType, Double> bonus(BonusType type, double value) {
        return Map.of(type, value);
    }
}