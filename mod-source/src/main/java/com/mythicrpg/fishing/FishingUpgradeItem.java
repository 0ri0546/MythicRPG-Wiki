package com.mythicrpg.fishing;

import net.minecraft.item.Item;

public final class FishingUpgradeItem extends Item {
    public enum Kind {
        BAIT_I,
        BAIT_II,
        BAIT_III,
        BAIT_LEGENDARY,
        RUNE_RARITY,
        RUNE_SPEED,
        RUNE_MASTERY
    }

    private final Kind kind;

    public FishingUpgradeItem(Kind kind, Settings settings) {
        super(settings);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public boolean isBait() {
        return kind.name().startsWith("BAIT_");
    }

    public boolean isRune() {
        return kind.name().startsWith("RUNE_");
    }
}
