package com.mythicrpg.core;

import com.mythicrpg.building.BuildingSkillTree;
import com.mythicrpg.crafting.CraftingSkillTree;
import com.mythicrpg.eating.EatingSkillTree;
import com.mythicrpg.fishing.FishingSkillTree;
import com.mythicrpg.farming.FarmingSkillTree;
import com.mythicrpg.fighting.FightingSkillTree;
import com.mythicrpg.mining.MiningSkillTree;
import com.mythicrpg.traveling.TravelingSkillTree;
import com.mythicrpg.woodcutting.WoodcuttingSkillTree;

import java.util.EnumMap;
import java.util.Map;

public class SkillTreeRegistry {
    private static final Map<SkillType, Map<Integer, SkillTreeNode>> TREES = new EnumMap<>(SkillType.class);

    static {
        TREES.put(SkillType.MINING, MiningSkillTree.buildTree());
        TREES.put(SkillType.FIGHTING, FightingSkillTree.buildTree());
        TREES.put(SkillType.WOODCUTTING, WoodcuttingSkillTree.buildTree());
        TREES.put(SkillType.FARMING, FarmingSkillTree.buildTree());
        TREES.put(SkillType.CRAFTING, CraftingSkillTree.buildTree());
        TREES.put(SkillType.TRAVELING, TravelingSkillTree.buildTree());
        TREES.put(SkillType.BUILDING, BuildingSkillTree.buildTree());
        TREES.put(SkillType.FISHING, FishingSkillTree.buildTree());
        TREES.put(SkillType.EATING, EatingSkillTree.buildTree());
        // ajouter au fur et à mesure :
        // TREES.put(SkillType.WOODCUTTING, WoodcuttingSkillTree.buildTree());
    }

    public static Map<Integer, SkillTreeNode> getTree(SkillType type) {
        return TREES.getOrDefault(type, Map.of());
    }
}