package com.mythicrpg.crafting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

public final class ResourceSaverManager {

    private ResourceSaverManager() {
    }

    public static boolean hasResourceSaver(ServerPlayerEntity player) {
        return getSaveChance(player) > 0.0;
    }

    public static boolean isEligible(
            ServerPlayerEntity player,
            RecipeInputInventory input,
            ItemStack resultPerCraft
    ) {
        if (resultPerCraft.isEmpty()) {
            return false;
        }

        double chance = getSaveChance(player);

        if (chance <= 0.0) {
            return false;
        }

        if (!isInputSafeForResourceSaver(input)) {
            return false;
        }

        if (!isResultSafeForResourceSaver(resultPerCraft)) {
            return false;
        }

        if (RecycleCraftManager.isRecycleRecipe(input, resultPerCraft)) {
            return false;
        }

        if (LuckyInfusionManager.isLuckyInfusionRecipe(input, resultPerCraft)) {
            return false;
        }

        return CraftScoreManager.getCraftScore(input, resultPerCraft) > 0;
    }

    public static void tryRestoreSavedResources(
            ServerPlayerEntity player,
            DefaultedList<ItemStack> beforeInput,
            RecipeInputInventory afterInput,
            ItemStack resultPerCraft,
            int craftedTimes,
            boolean eligible
    ) {
        if (!eligible || resultPerCraft.isEmpty() || craftedTimes <= 0) {
            return;
        }

        double chance = getSaveChance(player);

        if (chance <= 0.0) {
            return;
        }

        if (beforeInput == null || beforeInput.size() != afterInput.size()) {
            return;
        }

        if (!isSnapshotSafeForResourceSaver(beforeInput)) {
            return;
        }

        if (!isResultSafeForResourceSaver(resultPerCraft)) {
            return;
        }

        if (!isSafeToRestore(beforeInput, afterInput)) {
            return;
        }

        int savedCrafts = rollSavedCrafts(player, craftedTimes, chance);

        if (savedCrafts <= 0) {
            return;
        }

        int restoredItems = 0;

        for (int slot = 0; slot < beforeInput.size(); slot++) {
            ItemStack beforeStack = beforeInput.get(slot);

            if (beforeStack.isEmpty()) {
                continue;
            }

            ItemStack afterStack = afterInput.getStack(slot);
            int consumed = getConsumedCount(beforeStack, afterStack);

            if (consumed <= 0) {
                continue;
            }

            int amountToRestore = Math.min(consumed, savedCrafts);
            ItemStack restoreStack = beforeStack.copyWithCount(amountToRestore);

            restoredItems += restoreToCraftingGridOrInventory(
                    player,
                    afterInput,
                    slot,
                    restoreStack
            );
        }

        if (restoredItems > 0) {
            afterInput.markDirty();
        }
    }

    private static double getSaveChance(ServerPlayerEntity player) {
        return SkillTreeManager.getBonusTotal(
                player,
                SkillType.CRAFTING,
                BonusType.CRAFT_RESOURCE_SAVE_CHANCE
        );
    }

    private static int rollSavedCrafts(
            ServerPlayerEntity player,
            int craftedTimes,
            double chance
    ) {
        int savedCrafts = 0;

        for (int i = 0; i < craftedTimes; i++) {
            if (player.getRandom().nextDouble() < chance) {
                savedCrafts++;
            }
        }

        return savedCrafts;
    }

    private static boolean isInputSafeForResourceSaver(RecipeInputInventory input) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getStack(slot);

            if (stack.isEmpty()) {
                continue;
            }

            if (!isStackSafeForResourceSaver(stack)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isSnapshotSafeForResourceSaver(DefaultedList<ItemStack> beforeInput) {
        for (ItemStack stack : beforeInput) {
            if (stack.isEmpty()) {
                continue;
            }

            if (!isStackSafeForResourceSaver(stack)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isStackSafeForResourceSaver(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        if (stack.isDamageable()) {
            return false;
        }

        if (stack.getMaxCount() == 1) {
            return false;
        }

        if (hasSensitiveComponents(stack)) {
            return false;
        }

        return !isKnownContainerOrRemainderItem(stack.getItem())
                && !isKnownSpecialRecipeItem(stack.getItem());
    }

    private static boolean isResultSafeForResourceSaver(ItemStack resultStack) {
        if (resultStack.isEmpty()) {
            return false;
        }

        if (hasSensitiveComponents(resultStack)) {
            return false;
        }

        return !isKnownSpecialRecipeResult(resultStack.getItem());
    }

    private static boolean hasSensitiveComponents(ItemStack stack) {
        return stack.contains(DataComponentTypes.CUSTOM_DATA)
                || stack.contains(DataComponentTypes.CONTAINER)
                || stack.contains(DataComponentTypes.ENCHANTMENTS)
                || stack.contains(DataComponentTypes.ITEM_NAME);
    }

    private static boolean isKnownContainerOrRemainderItem(Item item) {
        return item == Items.BUCKET
                || item == Items.WATER_BUCKET
                || item == Items.LAVA_BUCKET
                || item == Items.MILK_BUCKET
                || item == Items.POWDER_SNOW_BUCKET
                || item == Items.GLASS_BOTTLE
                || item == Items.HONEY_BOTTLE
                || item == Items.POTION
                || item == Items.SPLASH_POTION
                || item == Items.LINGERING_POTION
                || item == Items.DRAGON_BREATH;
    }

    private static boolean isKnownSpecialRecipeItem(Item item) {
        Identifier id = Registries.ITEM.getId(item);

        if (!"minecraft".equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();

        return item == Items.SUSPICIOUS_STEW
                || item == Items.FIREWORK_ROCKET
                || item == Items.FIREWORK_STAR
                || item == Items.MAP
                || item == Items.FILLED_MAP
                || item == Items.WRITABLE_BOOK
                || item == Items.WRITTEN_BOOK
                || item == Items.ENCHANTED_BOOK
                || item == Items.SHIELD
                || item == Items.DECORATED_POT
                || path.endsWith("_banner")
                || path.endsWith("_banner_pattern")
                || path.endsWith("_pottery_sherd");
    }

    private static boolean isKnownSpecialRecipeResult(Item item) {
        Identifier id = Registries.ITEM.getId(item);

        if (!"minecraft".equals(id.getNamespace())) {
            return false;
        }

        String path = id.getPath();

        return item == Items.SUSPICIOUS_STEW
                || item == Items.MUSHROOM_STEW
                || item == Items.RABBIT_STEW
                || item == Items.BEETROOT_SOUP
                || item == Items.FIREWORK_ROCKET
                || item == Items.FIREWORK_STAR
                || item == Items.MAP
                || item == Items.FILLED_MAP
                || item == Items.WRITABLE_BOOK
                || item == Items.WRITTEN_BOOK
                || item == Items.ENCHANTED_BOOK
                || item == Items.SHIELD
                || item == Items.DECORATED_POT
                || path.endsWith("_banner")
                || path.endsWith("_banner_pattern")
                || path.endsWith("_pottery_sherd");
    }

    private static boolean isSafeToRestore(
            DefaultedList<ItemStack> beforeInput,
            RecipeInputInventory afterInput
    ) {
        for (int slot = 0; slot < beforeInput.size(); slot++) {
            ItemStack beforeStack = beforeInput.get(slot);
            ItemStack afterStack = afterInput.getStack(slot);

            if (beforeStack.isEmpty()) {
                continue;
            }

            if (afterStack.isEmpty()) {
                continue;
            }

            if (!ItemStack.areItemsAndComponentsEqual(beforeStack, afterStack)) {
                return false;
            }

            if (afterStack.getCount() > beforeStack.getCount()) {
                return false;
            }
        }

        return true;
    }

    private static int getConsumedCount(ItemStack beforeStack, ItemStack afterStack) {
        if (beforeStack.isEmpty()) {
            return 0;
        }

        if (afterStack.isEmpty()) {
            return beforeStack.getCount();
        }

        if (!ItemStack.areItemsAndComponentsEqual(beforeStack, afterStack)) {
            return 0;
        }

        return Math.max(0, beforeStack.getCount() - afterStack.getCount());
    }

    private static int restoreToCraftingGridOrInventory(
            ServerPlayerEntity player,
            RecipeInputInventory input,
            int slot,
            ItemStack restoreStack
    ) {
        if (restoreStack.isEmpty()) {
            return 0;
        }

        int originalCount = restoreStack.getCount();
        ItemStack currentStack = input.getStack(slot);

        if (currentStack.isEmpty()) {
            input.setStack(slot, restoreStack.copy());
            return originalCount;
        }

        if (ItemStack.areItemsAndComponentsEqual(currentStack, restoreStack)) {
            int space = currentStack.getMaxCount() - currentStack.getCount();
            int inserted = Math.min(space, restoreStack.getCount());

            if (inserted > 0) {
                currentStack.increment(inserted);
                restoreStack.decrement(inserted);
            }
        }

        if (!restoreStack.isEmpty()) {
            if (!player.getInventory().insertStack(restoreStack)) {
                player.dropItem(restoreStack, false);
            }
        }

        return originalCount;
    }
}
