package com.mythicrpg.farming;

import com.mythicrpg.core.ModAttachments;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.titles.TitleManager;
import com.mythicrpg.titles.TitleRegistry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * Owns the identity and death counter stored directly on each Food Backpack.
 *
 * <p>A player also keeps the id of one active linked backpack. Opening a bag or
 * letting it auto-store farming loot makes it active. Deaths always target that
 * exact bag when it is still carried; deterministic fallbacks are used only for
 * old bags created before the link existed.</p>
 */
public final class FoodBackpackDeathData {
    public static final int TITLE_DEATH_REQUIREMENT = 10;

    private static final String BACKPACK_ID_KEY = "MythicFoodBackpackId";
    private static final String OWNER_KEY = "MythicFoodBackpackOwner";
    private static final String DEATHS_KEY = "MythicFoodBackpackDeaths";

    private FoodBackpackDeathData() {
    }

    /**
     * Ensures an unowned bag has a stable identity, binds it to this player and
     * marks it as the player's active linked Food Backpack.
     *
     * @return true when the bag belongs to this player and could be activated
     */
    public static boolean activate(ItemStack backpack, ServerPlayerEntity player) {
        if (!backpack.isOf(ModItems.FOOD_BACKPACK)) {
            return false;
        }

        String playerId = player.getUuidAsString();
        NbtCompound before = customData(backpack);
        String existingOwner = before.getString(OWNER_KEY);
        if (!existingOwner.isBlank() && !existingOwner.equals(playerId)) {
            return false;
        }

        final String[] backpackId = {before.getString(BACKPACK_ID_KEY)};
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, backpack, nbt -> {
            if (backpackId[0].isBlank()) {
                backpackId[0] = UUID.randomUUID().toString();
                nbt.putString(BACKPACK_ID_KEY, backpackId[0]);
            }
            if (nbt.getString(OWNER_KEY).isBlank()) {
                nbt.putString(OWNER_KEY, playerId);
            }
        });

        ModAttachments.setActiveFoodBackpackId(player, backpackId[0]);
        return true;
    }

    public static void recordDeath(ServerPlayerEntity player) {
        Optional<ItemStack> target = findActiveBackpack(player)
                .or(() -> findFirstOwnedBackpack(player))
                .or(() -> findAndActivateFirstUnownedBackpack(player));
        if (target.isEmpty()) {
            return;
        }

        ItemStack backpack = target.get();
        // Migrated/owned bags become the explicit active target from this point.
        activate(backpack, player);

        final int[] newCount = {0};
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, backpack, nbt -> {
            int current = Math.max(0, nbt.getInt(DEATHS_KEY));
            newCount[0] = current == Integer.MAX_VALUE ? Integer.MAX_VALUE : current + 1;
            nbt.putInt(DEATHS_KEY, newCount[0]);
        });

        if (newCount[0] >= TITLE_DEATH_REQUIREMENT) {
            TitleManager.grantSpecialTitle(
                    player,
                    TitleRegistry.FOOD_BACKPACK_TEN_DEATHS_ID,
                    true
            );
        }
    }

    public static String getBackpackId(ItemStack backpack) {
        if (!backpack.isOf(ModItems.FOOD_BACKPACK)) {
            return "";
        }
        return customData(backpack).getString(BACKPACK_ID_KEY);
    }

    public static int getDeathCount(ItemStack backpack) {
        if (!backpack.isOf(ModItems.FOOD_BACKPACK)) {
            return 0;
        }
        return Math.max(0, customData(backpack).getInt(DEATHS_KEY));
    }

    public static boolean isLinkedTo(ItemStack backpack, ServerPlayerEntity player) {
        if (!backpack.isOf(ModItems.FOOD_BACKPACK)) {
            return false;
        }
        return player.getUuidAsString().equals(customData(backpack).getString(OWNER_KEY));
    }

    private static Optional<ItemStack> findActiveBackpack(ServerPlayerEntity player) {
        String activeId = ModAttachments.getActiveFoodBackpackId(player);
        if (activeId.isBlank()) {
            return Optional.empty();
        }

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!isLinkedTo(stack, player)) {
                continue;
            }
            if (activeId.equals(customData(stack).getString(BACKPACK_ID_KEY))) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    private static Optional<ItemStack> findFirstOwnedBackpack(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isLinkedTo(stack, player)) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    private static Optional<ItemStack> findAndActivateFirstUnownedBackpack(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isOf(ModItems.FOOD_BACKPACK)) {
                continue;
            }
            if (customData(stack).getString(OWNER_KEY).isBlank() && activate(stack, player)) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    private static NbtCompound customData(ItemStack backpack) {
        return backpack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }
}
