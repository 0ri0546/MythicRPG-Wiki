package com.mythicrpg.fishing;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.Perk;
import com.mythicrpg.core.SkillTreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FishingSkillTree {
    private FishingSkillTree() {}

    public static Map<Integer, SkillTreeNode> buildTree() {
        Map<Integer, SkillTreeNode> nodes = new HashMap<>();

        nodes.put(1, new SkillTreeNode(
                1,
                "skill_tree.mythicrpg.fishing.1.name",
                "skill_tree.mythicrpg.fishing.1.description",
                112, 10,
                List.of(),
                -1, -1,
                stub(),
                Map.of(BonusType.FISHING_CUSTOM_ROD, 1.0D)
        ));

        nodes.put(2, new SkillTreeNode(
                2,
                "skill_tree.mythicrpg.fishing.2.name",
                "skill_tree.mythicrpg.fishing.2.description",
                22, 45,
                List.of(1),
                1, 0,
                stub(),
                Map.of(BonusType.FISHING_WEATHER_RAIN, 1.0D)
        ));

        nodes.put(3, new SkillTreeNode(
                3,
                "skill_tree.mythicrpg.fishing.3.name",
                "skill_tree.mythicrpg.fishing.3.description",
                22, 75,
                List.of(2),
                1, 0,
                stub(),
                Map.of(BonusType.FISHING_WEATHER_SUN, 1.0D)
        ));

        nodes.put(4, new SkillTreeNode(
                4,
                "skill_tree.mythicrpg.fishing.4.name",
                "skill_tree.mythicrpg.fishing.4.description",
                22, 105,
                List.of(3),
                1, 0,
                stub(),
                Map.of(BonusType.FISHING_WEATHER_STORM, 1.0D)
        ));

        nodes.put(5, new SkillTreeNode(
                5,
                "skill_tree.mythicrpg.fishing.5.name",
                "skill_tree.mythicrpg.fishing.5.description",
                202, 45,
                List.of(1),
                1, 1,
                stub(),
                Map.of(BonusType.FISHING_BAIT_I, 1.0D)
        ));

        nodes.put(6, new SkillTreeNode(
                6,
                "skill_tree.mythicrpg.fishing.6.name",
                "skill_tree.mythicrpg.fishing.6.description",
                202, 75,
                List.of(5),
                1, 1,
                stub(),
                Map.of(BonusType.FISHING_BAIT_II, 1.0D)
        ));

        nodes.put(7, new SkillTreeNode(
                7,
                "skill_tree.mythicrpg.fishing.7.name",
                "skill_tree.mythicrpg.fishing.7.description",
                202, 105,
                List.of(6),
                1, 1,
                stub(),
                Map.of(BonusType.FISHING_BAIT_III, 1.0D)
        ));

        nodes.put(8, new SkillTreeNode(
                8,
                "skill_tree.mythicrpg.fishing.8.name",
                "skill_tree.mythicrpg.fishing.8.description",
                112, 140,
                List.of(4, 7),
                -1, -1,
                stub(),
                Map.of(BonusType.FISHING_RUNE_SLOTS, 1.0D)
        ));

        nodes.put(9, new SkillTreeNode(
                9,
                "skill_tree.mythicrpg.fishing.9.name",
                "skill_tree.mythicrpg.fishing.9.description",
                2, 175,
                List.of(8),
                2, 0,
                stub(),
                Map.of(BonusType.FISHING_RUNE_RARITY, 1.0D)
        ));

        nodes.put(10, new SkillTreeNode(
                10,
                "skill_tree.mythicrpg.fishing.10.name",
                "skill_tree.mythicrpg.fishing.10.description",
                2, 205,
                List.of(9),
                2, 0,
                stub(),
                Map.of(BonusType.FISHING_RUNE_SPEED, 1.0D)
        ));

        nodes.put(11, new SkillTreeNode(
                11,
                "skill_tree.mythicrpg.fishing.11.name",
                "skill_tree.mythicrpg.fishing.11.description",
                2, 235,
                List.of(10),
                2, 0,
                stub(),
                Map.of(BonusType.FISHING_RUNE_MASTERY, 1.0D)
        ));

        nodes.put(12, new SkillTreeNode(
                12,
                "skill_tree.mythicrpg.fishing.12.name",
                "skill_tree.mythicrpg.fishing.12.description",
                112, 175,
                List.of(8),
                2, 1,
                stub(),
                Map.of(BonusType.FISHING_NET_3, 1.0D)
        ));

        nodes.put(13, new SkillTreeNode(
                13,
                "skill_tree.mythicrpg.fishing.13.name",
                "skill_tree.mythicrpg.fishing.13.description",
                112, 205,
                List.of(12),
                2, 1,
                stub(),
                Map.of(BonusType.FISHING_NET_4, 1.0D)
        ));

        nodes.put(14, new SkillTreeNode(
                14,
                "skill_tree.mythicrpg.fishing.14.name",
                "skill_tree.mythicrpg.fishing.14.description",
                112, 235,
                List.of(13),
                2, 1,
                stub(),
                Map.of(BonusType.FISHING_NET_5, 1.0D)
        ));

        nodes.put(15, new SkillTreeNode(
                15,
                "skill_tree.mythicrpg.fishing.15.name",
                "skill_tree.mythicrpg.fishing.15.description",
                222, 175,
                List.of(8),
                2, 2,
                stub(),
                Map.of(BonusType.FISHING_FISHERY_TABLE, 1.0D)
        ));

        nodes.put(16, new SkillTreeNode(
                16,
                "skill_tree.mythicrpg.fishing.16.name",
                "skill_tree.mythicrpg.fishing.16.description",
                222, 205,
                List.of(15),
                2, 2,
                stub(),
                Map.of(BonusType.FISHING_SCALE_ARMOR, 1.0D)
        ));

        nodes.put(17, new SkillTreeNode(
                17,
                "skill_tree.mythicrpg.fishing.17.name",
                "skill_tree.mythicrpg.fishing.17.description",
                222, 235,
                List.of(16),
                2, 2,
                stub(),
                Map.of(BonusType.FISHING_BOAT, 1.0D)
        ));

        nodes.put(18, new SkillTreeNode(
                18,
                "skill_tree.mythicrpg.fishing.18.name",
                "skill_tree.mythicrpg.fishing.18.description",
                112, 265,
                List.of(11, 14, 17),
                -1, -1,
                stub(),
                Map.of(BonusType.FISHING_LEGENDARY_BAIT, 1.0D)
        ));

        nodes.put(19, new SkillTreeNode(
                19,
                "skill_tree.mythicrpg.fishing.19.name",
                "skill_tree.mythicrpg.fishing.19.description",
                62, 295,
                List.of(18),
                3, 0,
                stub(),
                Map.of(BonusType.FISHING_BASALT_ROD, 1.0D)
        ));

        nodes.put(20, new SkillTreeNode(
                20,
                "skill_tree.mythicrpg.fishing.20.name",
                "skill_tree.mythicrpg.fishing.20.description",
                162, 295,
                List.of(18),
                3, 1,
                stub(),
                Map.of(BonusType.FISHING_VOID_ROD, 1.0D)
        ));

        return nodes;
    }

    private static Perk stub() { return player -> {}; }
}
