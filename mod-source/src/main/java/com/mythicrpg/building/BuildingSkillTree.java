package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.Perk;
import com.mythicrpg.core.SkillTreeNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Building's validated 20-node progression path. Runtime effects are added phase by phase. */
public final class BuildingSkillTree {
    private BuildingSkillTree() {}

    public static Map<Integer, SkillTreeNode> buildTree() {
        Map<Integer, SkillTreeNode> nodes = new LinkedHashMap<>();

        // Racine
        nodes.put(1, new SkillTreeNode(
                1,
                "skill_tree.mythicrpg.building.1.name",
                "skill_tree.mythicrpg.building.1.description",
                112, 10,
                List.of(),
                -1, -1,
                stub(),
                Map.of(BonusType.BUILD_QUICK_REPLACE, 1.0)
        ));

        // Branche supérieure gauche — Plans
        nodes.put(2, new SkillTreeNode(
                2,
                "skill_tree.mythicrpg.building.2.name",
                "skill_tree.mythicrpg.building.2.description",
                22, 45,
                List.of(1),
                1, 0,
                stub(),
                Map.of(BonusType.BUILD_PLAN_2D_8, 1.0)
        ));

        nodes.put(3, new SkillTreeNode(
                3,
                "skill_tree.mythicrpg.building.3.name",
                "skill_tree.mythicrpg.building.3.description",
                22, 75,
                List.of(2),
                1, 0,
                stub(),
                Map.of(BonusType.BUILD_PLAN_2D_12, 1.0)
        ));

        nodes.put(4, new SkillTreeNode(
                4,
                "skill_tree.mythicrpg.building.4.name",
                "skill_tree.mythicrpg.building.4.description",
                22, 105,
                List.of(3),
                1, 0,
                stub(),
                Map.of(BonusType.BUILD_PLAN_3D, 1.0)
        ));

        // Branche supérieure droite — Confort de chantier
        nodes.put(5, new SkillTreeNode(
                5,
                "skill_tree.mythicrpg.building.5.name",
                "skill_tree.mythicrpg.building.5.description",
                202, 45,
                List.of(1),
                1, 1,
                stub(),
                Map.of(BonusType.BUILD_AUTO_RESTOCK, 1.0)
        ));

        nodes.put(6, new SkillTreeNode(
                6,
                "skill_tree.mythicrpg.building.6.name",
                "skill_tree.mythicrpg.building.6.description",
                202, 75,
                List.of(5),
                1, 1,
                stub(),
                Map.of(BonusType.BUILD_DECORATIVE_MAGNET, 1.0)
        ));

        nodes.put(7, new SkillTreeNode(
                7,
                "skill_tree.mythicrpg.building.7.name",
                "skill_tree.mythicrpg.building.7.description",
                202, 105,
                List.of(6),
                1, 1,
                stub(),
                Map.of(BonusType.BUILD_NO_TOOL_DURABILITY, 1.0)
        ));

        // Première convergence
        nodes.put(8, new SkillTreeNode(
                8,
                "skill_tree.mythicrpg.building.8.name",
                "skill_tree.mythicrpg.building.8.description",
                112, 140,
                List.of(4, 7),
                -1, -1,
                stub(),
                Map.of(BonusType.BUILD_VERTICAL_SLABS, 1.0)
        ));

        // Branche inférieure gauche — Portée
        nodes.put(9, new SkillTreeNode(
                9,
                "skill_tree.mythicrpg.building.9.name",
                "skill_tree.mythicrpg.building.9.description",
                2, 175,
                List.of(8),
                2, 0,
                stub(),
                Map.of(BonusType.BUILD_REACH, 1.0)
        ));

        nodes.put(10, new SkillTreeNode(
                10,
                "skill_tree.mythicrpg.building.10.name",
                "skill_tree.mythicrpg.building.10.description",
                2, 205,
                List.of(9),
                2, 0,
                stub(),
                Map.of(BonusType.BUILD_REACH, 2.0)
        ));

        nodes.put(11, new SkillTreeNode(
                11,
                "skill_tree.mythicrpg.building.11.name",
                "skill_tree.mythicrpg.building.11.description",
                2, 235,
                List.of(10),
                2, 0,
                stub(),
                Map.of(BonusType.BUILD_REACH, 3.0)
        ));

        // Branche inférieure centrale — Architecture
        nodes.put(12, new SkillTreeNode(
                12,
                "skill_tree.mythicrpg.building.12.name",
                "skill_tree.mythicrpg.building.12.description",
                112, 175,
                List.of(8),
                2, 1,
                stub(),
                Map.of(BonusType.BUILD_SCAFFOLDING_RANGE, 32.0)
        ));

        nodes.put(13, new SkillTreeNode(
                13,
                "skill_tree.mythicrpg.building.13.name",
                "skill_tree.mythicrpg.building.13.description",
                112, 205,
                List.of(12),
                2, 1,
                stub(),
                Map.of(BonusType.BUILD_BLANK_BLOCK, 1.0)
        ));

        nodes.put(14, new SkillTreeNode(
                14,
                "skill_tree.mythicrpg.building.14.name",
                "skill_tree.mythicrpg.building.14.description",
                112, 235,
                List.of(13),
                2, 1,
                stub(),
                Map.of(BonusType.BUILD_ARCHITECT_COMPASS, 1.0)
        ));

        // Branche inférieure droite — Réserve de chantier
        nodes.put(15, new SkillTreeNode(
                15,
                "skill_tree.mythicrpg.building.15.name",
                "skill_tree.mythicrpg.building.15.description",
                222, 175,
                List.of(8),
                2, 2,
                stub(),
                Map.of(BonusType.BUILD_RESERVE_RANGE, 5.0)
        ));

        nodes.put(16, new SkillTreeNode(
                16,
                "skill_tree.mythicrpg.building.16.name",
                "skill_tree.mythicrpg.building.16.description",
                222, 205,
                List.of(15),
                2, 2,
                stub(),
                Map.of(BonusType.BUILD_RESERVE_RANGE, 10.0)
        ));

        nodes.put(17, new SkillTreeNode(
                17,
                "skill_tree.mythicrpg.building.17.name",
                "skill_tree.mythicrpg.building.17.description",
                222, 235,
                List.of(16),
                2, 2,
                stub(),
                Map.of(BonusType.BUILD_RESERVE_RANGE, 15.0)
        ));

        // Seconde convergence
        nodes.put(18, new SkillTreeNode(
                18,
                "skill_tree.mythicrpg.building.18.name",
                "skill_tree.mythicrpg.building.18.description",
                112, 265,
                List.of(11, 14, 17),
                -1, -1,
                stub(),
                Map.of(BonusType.BUILD_MINIATURE, 1.0)
        ));

        // Perks finaux
        nodes.put(19, new SkillTreeNode(
                19,
                "skill_tree.mythicrpg.building.19.name",
                "skill_tree.mythicrpg.building.19.description",
                62, 295,
                List.of(18),
                3, 0,
                stub(),
                Map.of(BonusType.BUILD_STATIC_DECORATION, 1.0)
        ));

        nodes.put(20, new SkillTreeNode(
                20,
                "skill_tree.mythicrpg.building.20.name",
                "skill_tree.mythicrpg.building.20.description",
                162, 295,
                List.of(18),
                3, 1,
                stub(),
                Map.of(BonusType.BUILD_WAND, 1.0)
        ));

        return nodes;
    }

    private static SkillTreeNode node(int id, int x, int y, List<Integer> parents,
                                      BonusType bonusType, double value) {
        return new SkillTreeNode(
                id,
                "skill_tree.mythicrpg.building." + id + ".name",
                "skill_tree.mythicrpg.building." + id + ".description",
                x, y, parents, -1, -1, stub(), Map.of(bonusType, value)
        );
    }

    private static Perk stub() {
        return player -> {
            // Building perk effects are implemented by their dedicated phase managers.
        };
    }
}
