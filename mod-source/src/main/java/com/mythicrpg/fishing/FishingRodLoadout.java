
package com.mythicrpg.fishing;

import com.mythicrpg.core.ItemContainerUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

/**
 * Immutable view of the three upgrade slots stored directly on a Mythic fishing rod.
 *
 * <p>Slot 0 is the consumable bait. Slots 1 and 2 are unique runes. All gameplay
 * systems read the same representation so the screen, reel logic and tooltips cannot
 * disagree about the installed loadout.</p>
 */
public record FishingRodLoadout(
        FishingUpgradeItem.Kind bait,
        boolean rarityRune,
        boolean speedRune,
        boolean masteryRune
) {
    public static final int SLOT_COUNT = 3;
    public static final int BAIT_SLOT = 0;
    public static final int FIRST_RUNE_SLOT = 1;
    public static final int SECOND_RUNE_SLOT = 2;

    public static FishingRodLoadout read(ItemStack rod) {
        DefaultedList<ItemStack> stacks = ItemContainerUtils.read(rod, SLOT_COUNT);
        FishingUpgradeItem.Kind bait = kind(stacks.get(BAIT_SLOT));
        if (bait != null && !bait.name().startsWith("BAIT_")) {
            bait = null;
        }

        boolean rarity = false;
        boolean speed = false;
        boolean mastery = false;
        for (int slot = FIRST_RUNE_SLOT; slot <= SECOND_RUNE_SLOT; slot++) {
            FishingUpgradeItem.Kind kind = kind(stacks.get(slot));
            if (kind == FishingUpgradeItem.Kind.RUNE_RARITY) rarity = true;
            if (kind == FishingUpgradeItem.Kind.RUNE_SPEED) speed = true;
            if (kind == FishingUpgradeItem.Kind.RUNE_MASTERY) mastery = true;
        }
        return new FishingRodLoadout(bait, rarity, speed, mastery);
    }

    public static boolean consumeOneBait(ItemStack rod) {
        DefaultedList<ItemStack> stacks = ItemContainerUtils.read(rod, SLOT_COUNT);
        ItemStack baitStack = stacks.get(BAIT_SLOT);
        if (!(baitStack.getItem() instanceof FishingUpgradeItem upgrade) || !upgrade.isBait()) {
            return false;
        }
        baitStack.decrement(1);
        if (baitStack.isEmpty()) {
            stacks.set(BAIT_SLOT, ItemStack.EMPTY);
        }
        ItemContainerUtils.write(rod, stacks);
        return true;
    }

    public static boolean containsDuplicateRunes(DefaultedList<ItemStack> stacks) {
        ItemStack first = stacks.get(FIRST_RUNE_SLOT);
        ItemStack second = stacks.get(SECOND_RUNE_SLOT);
        return !first.isEmpty() && !second.isEmpty() && first.getItem() == second.getItem();
    }

    private static FishingUpgradeItem.Kind kind(ItemStack stack) {
        return stack.getItem() instanceof FishingUpgradeItem upgrade ? upgrade.kind() : null;
    }
}
