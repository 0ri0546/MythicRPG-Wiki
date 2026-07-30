package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.Perk;
import com.mythicrpg.core.SkillTreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CraftingSkillTree {

    private CraftingSkillTree() {
    }

    public static Map<Integer, SkillTreeNode> buildTree() {
        Map<Integer, SkillTreeNode> nodes = new HashMap<>();

        nodes.put(1, new SkillTreeNode(1, "skill_tree.mythicrpg.crafting.1.name", "skill_tree.mythicrpg.crafting.1.description",
                112,
                10,
                List.of(),
                -1,
                -1,
                stub(),
                bonus(BonusType.CRAFT_PORTABLE_TABLE, 1.0)
        ));

        nodes.put(2, new SkillTreeNode(2, "skill_tree.mythicrpg.crafting.2.name", "skill_tree.mythicrpg.crafting.2.description",
                22,
                45,
                List.of(1),
                1,
                0,
                stub(),
                bonus(BonusType.CRAFT_RESOURCE_SAVE_CHANCE, 0.02)
        ));

        nodes.put(3, new SkillTreeNode(3, "skill_tree.mythicrpg.crafting.3.name", "skill_tree.mythicrpg.crafting.3.description",
                22,
                75,
                List.of(2),
                1,
                0,
                stub(),
                bonus(BonusType.CRAFT_RESOURCE_SAVE_CHANCE, 0.02)
        ));

        nodes.put(4, new SkillTreeNode(4, "skill_tree.mythicrpg.crafting.4.name", "skill_tree.mythicrpg.crafting.4.description",
                22,
                105,
                List.of(3),
                1,
                0,
                stub(),
                bonus(BonusType.CRAFT_RESOURCE_SAVE_CHANCE, 0.02)
        ));

        nodes.put(5, new SkillTreeNode(5, "skill_tree.mythicrpg.crafting.5.name", "skill_tree.mythicrpg.crafting.5.description",
                202,
                45,
                List.of(1),
                1,
                1,
                stub(),
                bonus(BonusType.REPAIR_KIT_CRAFT, 1.0)
        ));

        nodes.put(6, new SkillTreeNode(6, "skill_tree.mythicrpg.crafting.6.name", "skill_tree.mythicrpg.crafting.6.description",
                202,
                75,
                List.of(5),
                1,
                1,
                stub(),
                bonus(BonusType.REPAIR_KIT_POWER, 0.10)
        ));

        nodes.put(7, new SkillTreeNode(7, "skill_tree.mythicrpg.crafting.7.name", "skill_tree.mythicrpg.crafting.7.description",
                202,
                105,
                List.of(6),
                1,
                1,
                stub(),
                bonus(BonusType.REPAIR_KIT_POWER, 0.10)
        ));

        nodes.put(8, new SkillTreeNode(8, "skill_tree.mythicrpg.crafting.8.name", "skill_tree.mythicrpg.crafting.8.description",
                112,
                140,
                List.of(4, 7),
                -1,
                -1,
                stub(),
                bonus(BonusType.LUCKY_BLOCK_CRAFT, 1.0)
        ));

        nodes.put(9, new SkillTreeNode(9, "skill_tree.mythicrpg.crafting.9.name", "skill_tree.mythicrpg.crafting.9.description",
                2,
                175,
                List.of(8),
                2,
                0,
                stub(),
                bonus(BonusType.INFINITE_CRAFTING_TABLE_CRAFT, 1.0)
        ));

        nodes.put(10, new SkillTreeNode(10, "skill_tree.mythicrpg.crafting.10.name", "skill_tree.mythicrpg.crafting.10.description",
                2,
                205,
                List.of(9),
                2,
                0,
                stub(),
                bonus(BonusType.EXP_CHARM_CRAFT, 1.0)
        ));

        nodes.put(11, new SkillTreeNode(11, "skill_tree.mythicrpg.crafting.11.name", "skill_tree.mythicrpg.crafting.11.description",
                2,
                235,
                List.of(10),
                2,
                0,
                stub(),
                bonus(BonusType.CRAFT_CHARGE, 1.0)
        ));

        nodes.put(12, new SkillTreeNode(12, "skill_tree.mythicrpg.crafting.12.name", "skill_tree.mythicrpg.crafting.12.description",
                112,
                175,
                List.of(8),
                2,
                1,
                stub(),
                bonus(BonusType.CRAFT_VANILLA_XP, 1.0)
        ));

        nodes.put(13, new SkillTreeNode(13, "skill_tree.mythicrpg.crafting.13.name", "skill_tree.mythicrpg.crafting.13.description",
                112,
                205,
                List.of(12),
                2,
                1,
                stub(),
                bonus(BonusType.MYTHIC_INSPIRATION, 1.0)
        ));

        nodes.put(14, new SkillTreeNode(14, "skill_tree.mythicrpg.crafting.14.name", "skill_tree.mythicrpg.crafting.14.description",
                112,
                235,
                List.of(13),
                2,
                1,
                stub(),
                bonus(BonusType.FIRST_CRAFT_BONUS, 1.0)
        ));

        nodes.put(15, new SkillTreeNode(15, "skill_tree.mythicrpg.crafting.15.name", "skill_tree.mythicrpg.crafting.15.description",
                222,
                175,
                List.of(8),
                2,
                2,
                stub(),
                bonus(BonusType.RECYCLE_CRAFTS, 1.0)
        ));

        nodes.put(16, new SkillTreeNode(16, "skill_tree.mythicrpg.crafting.16.name", "skill_tree.mythicrpg.crafting.16.description",
                222,
                205,
                List.of(15),
                2,
                2,
                stub(),
                bonus(BonusType.MIDNIGHT_CRAFTING, 1.0)
        ));

        nodes.put(17, new SkillTreeNode(17, "skill_tree.mythicrpg.crafting.17.name", "skill_tree.mythicrpg.crafting.17.description",
                222,
                235,
                List.of(16),
                2,
                2,
                stub(),
                bonus(BonusType.REINFORCED_CRAFT_CHANCE, 0.30)
        ));

        nodes.put(18, new SkillTreeNode(18, "skill_tree.mythicrpg.crafting.18.name", "skill_tree.mythicrpg.crafting.18.description",
                112,
                265,
                List.of(11, 14, 17),
                -1,
                -1,
                stub(),
                bonus(BonusType.LUCKY_INFUSION, 1.0)
        ));

        nodes.put(19, new SkillTreeNode(19, "skill_tree.mythicrpg.crafting.19.name", "skill_tree.mythicrpg.crafting.19.description",
                62,
                295,
                List.of(18),
                3,
                0,
                stub(),
                bonus(BonusType.TRANSFORMATION_SLOT, 1.0)
        ));

        nodes.put(20, new SkillTreeNode(20, "skill_tree.mythicrpg.crafting.20.name", "skill_tree.mythicrpg.crafting.20.description",
                162,
                295,
                List.of(18),
                3,
                1,
                stub(),
                bonus(BonusType.CRAFT_MASTERY, 1.0)
        ));

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
}