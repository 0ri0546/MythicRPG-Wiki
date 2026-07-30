package com.mythicrpg.mining.archaeology;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Single source of truth for fossil-incubator input validation and output resolution.
 * Both the server block entity and the client screen handler use this class.
 */
public final class FossilIncubationRecipe {

    public static final int REQUIRED_FOSSILS = 9;
    public static final int REQUIRED_KELP = 9;

    private FossilIncubationRecipe() {
    }

    /** Resolves the family and median rarity from the nine fossil slots. */
    public static Optional<Output> resolveFossils(Inventory inventory) {
        FossilFamily family = null;
        List<FossilRarity> rarities = new ArrayList<>(REQUIRED_FOSSILS);

        for (int slot = FossilIncubatorBlockEntity.FOSSIL_SLOT_START;
             slot < FossilIncubatorBlockEntity.FOSSIL_SLOT_END;
             slot++) {
            ItemStack stack = inventory.getStack(slot);
            if (!(stack.getItem() instanceof FossilItem fossilItem) || stack.getCount() < 1) {
                return Optional.empty();
            }

            if (family == null) {
                family = fossilItem.family();
            } else if (family != fossilItem.family()) {
                return Optional.empty();
            }
            rarities.add(fossilItem.rarity());
        }

        if (family == null) {
            return Optional.empty();
        }

        FossilFamily resolvedFamily = family;
        FossilRarity rarity = FossilRarity.median(rarities);
        Optional<Item> skeletonItem = FossilContentRegistry.skeletonItem(resolvedFamily, rarity);
        return skeletonItem.map(item -> new Output(resolvedFamily, rarity, item));
    }

    /** Resolves a complete startable recipe, including water and kelp requirements. */
    public static Optional<Output> resolveReadyRecipe(Inventory inventory) {
        if (!hasRequiredCatalysts(inventory)) {
            return Optional.empty();
        }
        return resolveFossils(inventory);
    }

    public static boolean hasRequiredCatalysts(Inventory inventory) {
        ItemStack water = inventory.getStack(FossilIncubatorBlockEntity.WATER_SLOT);
        ItemStack kelp = inventory.getStack(FossilIncubatorBlockEntity.KELP_SLOT);
        return water.isOf(Items.WATER_BUCKET)
                && kelp.isOf(Items.KELP)
                && kelp.getCount() >= REQUIRED_KELP;
    }

    /** Prevents mixing fossil families while still allowing any rarity combination. */
    public static boolean canInsertFossil(Inventory inventory, int targetSlot, ItemStack stack) {
        if (!(stack.getItem() instanceof FossilItem candidate)) {
            return false;
        }

        for (int slot = FossilIncubatorBlockEntity.FOSSIL_SLOT_START;
             slot < FossilIncubatorBlockEntity.FOSSIL_SLOT_END;
             slot++) {
            if (slot == targetSlot) {
                continue;
            }

            ItemStack existing = inventory.getStack(slot);
            if (existing.isEmpty()) {
                continue;
            }
            if (!(existing.getItem() instanceof FossilItem fossilItem)
                    || fossilItem.family() != candidate.family()) {
                return false;
            }
        }
        return true;
    }

    public record Output(FossilFamily family, FossilRarity rarity, Item item) {
    }
}
