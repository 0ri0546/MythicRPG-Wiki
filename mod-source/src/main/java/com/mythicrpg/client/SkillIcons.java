package com.mythicrpg.client;

import com.mythicrpg.core.SkillType;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.EnumMap;
import java.util.Map;

public class SkillIcons {
    private static final Map<SkillType, Item> ICONS = new EnumMap<>(SkillType.class);

    static {
        ICONS.put(SkillType.MINING, Items.NETHERITE_PICKAXE);
        ICONS.put(SkillType.FIGHTING, Items.NETHERITE_SWORD);
        ICONS.put(SkillType.WOODCUTTING, Items.NETHERITE_AXE);
        ICONS.put(SkillType.FARMING, Items.WHEAT);
        ICONS.put(SkillType.CRAFTING, Items.CRAFTING_TABLE);
        ICONS.put(SkillType.FISHING, Items.FISHING_ROD);
        ICONS.put(SkillType.BUILDING, Items.BRICKS);
        ICONS.put(SkillType.TRAVELING, Items.GOLDEN_BOOTS);
        ICONS.put(SkillType.EATING, Items.COOKED_BEEF);
    }

    public static Item get(SkillType type) {
        return ICONS.getOrDefault(type, Items.BARRIER);
    }
}