package com.mythicrpg.traveling;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.Perk;
import com.mythicrpg.core.SkillTreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TravelingSkillTree {

    private TravelingSkillTree() {
    }

    public static Map<Integer, SkillTreeNode> buildTree() {
        Map<Integer, SkillTreeNode> nodes = new HashMap<>();

        nodes.put(1, node(1, "skill_tree.mythicrpg.traveling.1.name",
                "skill_tree.mythicrpg.traveling.1.description", 112, 10, List.of(), -1, -1,
                BonusType.TRAVEL_DOUBLE_JUMP, 1.0));

        nodes.put(2, node(2, "skill_tree.mythicrpg.traveling.2.name",
                "skill_tree.mythicrpg.traveling.2.description",
                22, 45, List.of(1), 1, 0, BonusType.TRAVEL_SOUL_WALKER, 1.0));
        nodes.put(3, node(3, "skill_tree.mythicrpg.traveling.3.name",
                "skill_tree.mythicrpg.traveling.3.description", 22, 75, List.of(2), 1, 0,
                BonusType.TRAVEL_DOLPHINS_GRACE, 1.0));
        nodes.put(4, node(4, "skill_tree.mythicrpg.traveling.4.name",
                "skill_tree.mythicrpg.traveling.4.description", 22, 105, List.of(3), 1, 0,
                BonusType.TRAVEL_POWDER_WALKER, 1.0));

        nodes.put(5, node(5, "skill_tree.mythicrpg.traveling.5.name",
                "skill_tree.mythicrpg.traveling.5.description", 202, 45, List.of(1), 1, 1,
                BonusType.TRAVEL_XP_MULTIPLIER, 0.10));
        nodes.put(6, node(6, "skill_tree.mythicrpg.traveling.6.name",
                "skill_tree.mythicrpg.traveling.6.description", 202, 75, List.of(5), 1, 1,
                BonusType.TRAVEL_XP_MULTIPLIER, 0.15));
        nodes.put(7, node(7, "skill_tree.mythicrpg.traveling.7.name",
                "skill_tree.mythicrpg.traveling.7.description",
                202, 105, List.of(6), 1, 1, BonusType.TRAVEL_DISCOVERY_XP_MULTIPLIER, 0.25));

        nodes.put(8, node(8, "skill_tree.mythicrpg.traveling.8.name",
                "skill_tree.mythicrpg.traveling.8.description",
                112, 140, List.of(4, 7), -1, -1, BonusType.TRAVEL_MINIATURIZATION, 1.0));

        nodes.put(9, node(9, "skill_tree.mythicrpg.traveling.9.name",
                "skill_tree.mythicrpg.traveling.9.description",
                2, 175, List.of(8), 2, 0, BonusType.MONUMENTAL_COMPASS_CRAFT, 1.0));
        nodes.put(10, node(10, "skill_tree.mythicrpg.traveling.10.name",
                "skill_tree.mythicrpg.traveling.10.description",
                2, 205, List.of(9), 2, 0, BonusType.STRUCTURE_MODULES_OVERWORLD, 1.0));
        nodes.put(11, node(11, "skill_tree.mythicrpg.traveling.11.name",
                "skill_tree.mythicrpg.traveling.11.description",
                2, 235, List.of(10), 2, 0, BonusType.STRUCTURE_MODULES_NETHER_END, 1.0));

        nodes.put(12, node(12, "skill_tree.mythicrpg.traveling.12.name",
                "skill_tree.mythicrpg.traveling.12.description",
                112, 175, List.of(8), 2, 1, BonusType.TRAVEL_DEATH_RECALL, 1.0));
        nodes.put(13, node(13, "skill_tree.mythicrpg.traveling.13.name",
                "skill_tree.mythicrpg.traveling.13.description", 112, 205, List.of(12), 2, 1,
                BonusType.TRAVEL_BOOTS_NO_DURABILITY, 1.0));
        nodes.put(14, node(14, "skill_tree.mythicrpg.traveling.14.name",
                "skill_tree.mythicrpg.traveling.14.description",
                112, 235, List.of(13), 2, 1, BonusType.TRAVEL_BIOME_SPEED, 1.0));

        nodes.put(15, node(15, "skill_tree.mythicrpg.traveling.15.name",
                "skill_tree.mythicrpg.traveling.15.description",
                222, 175, List.of(8), 2, 2, BonusType.FAST_MINECART_CRAFT, 1.0));
        nodes.put(16, node(16, "skill_tree.mythicrpg.traveling.16.name",
                "skill_tree.mythicrpg.traveling.16.description",
                222, 205, List.of(15), 2, 2, BonusType.FAST_BOAT_CRAFT, 1.0));
        nodes.put(17, node(17, "skill_tree.mythicrpg.traveling.17.name",
                "skill_tree.mythicrpg.traveling.17.description", 222, 235, List.of(16), 2, 2,
                BonusType.LAND_MOUNTS, 1.0));

        nodes.put(18, node(18, "skill_tree.mythicrpg.traveling.18.name",
                "skill_tree.mythicrpg.traveling.18.description",
                112, 265, List.of(11, 14, 17), -1, -1, BonusType.TREASURE_VANILLA_XP, 1.0));

        nodes.put(19, node(19, "skill_tree.mythicrpg.traveling.19.name",
                "skill_tree.mythicrpg.traveling.19.description",
                62, 295, List.of(18), 3, 0, BonusType.FLYING_MOUNTS, 1.0));
        nodes.put(20, node(20, "skill_tree.mythicrpg.traveling.20.name",
                "skill_tree.mythicrpg.traveling.20.description",
                162, 295, List.of(18), 3, 1, BonusType.GRAPPLING_HOOK_CRAFT, 1.0));

        return nodes;
    }

    private static SkillTreeNode node(
            int id,
            String name,
            String description,
            int x,
            int y,
            List<Integer> parents,
            int forkId,
            int branchId,
            BonusType bonusType,
            double value
    ) {
        return new SkillTreeNode(
                id, name, description, x, y, parents, forkId, branchId,
                stub(), Map.of(bonusType, value)
        );
    }

    private static Perk stub() {
        return player -> {
            // Runtime effects are handled by Traveling managers.
        };
    }
}
