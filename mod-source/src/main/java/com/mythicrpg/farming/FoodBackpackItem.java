package com.mythicrpg.farming;

import com.mythicrpg.core.ItemContainerUtils;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.PlayerCooldownManager;
import com.mythicrpg.eating.EatingFoodStorage;
import com.mythicrpg.eating.EatingPreservationManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.Formatting;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;

public class FoodBackpackItem extends Item {
    private static final int BACKPACK_SLOTS = 54;
    private static final int STORE_FEEDBACK_COOLDOWN_TICKS = 12;
    private static final int FULL_FEEDBACK_COOLDOWN_TICKS = 40;

    public FoodBackpackItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!world.isClient() && Math.floorMod(entity.age + slot, 20) == 0) {
            EatingPreservationManager.PreservationMode mode = entity instanceof ServerPlayerEntity player
                    ? EatingPreservationManager.modeForPlayer(player)
                    : EatingPreservationManager.PreservationMode.NONE;
            boolean changed;
            if (entity instanceof ServerPlayerEntity player) {
                var openInventory = FoodBackpackSessionManager.inventory(player, stack);
                changed = openInventory.isPresent()
                        ? updateInventoryPreservation(openInventory.get(), world.getTime(), mode)
                        : FoodBackpackPerishableIndex.hasPerishables(stack, BACKPACK_SLOTS)
                                && EatingPreservationManager.updateContainer(stack, BACKPACK_SLOTS, world.getTime(), mode);
            } else {
                changed = FoodBackpackPerishableIndex.hasPerishables(stack, BACKPACK_SLOTS)
                        && EatingPreservationManager.updateContainer(stack, BACKPACK_SLOTS, world.getTime(), mode);
            }
            if (changed) {
                if (entity instanceof ServerPlayerEntity player
                        && FoodBackpackSessionManager.inventory(player, stack).isEmpty()) {
                    FoodBackpackPerishableIndex.refresh(stack, BACKPACK_SLOTS);
                }
                if (entity instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.getInventory().markDirty();
                }
            }
        }
    }

    public static boolean tryStore(ServerPlayerEntity player, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return false;
        }

        if (!isAcceptedItem(incoming)) {
            return false;
        }

        ItemStack backpack = findBackpack(player);

        if (backpack.isEmpty()) {
            return false;
        }

        FoodBackpackDeathData.activate(backpack, player);
        int before = incoming.getCount();

        boolean changed = FoodBackpackSessionManager.inventory(player, backpack)
                .map(inventory -> insertIntoInventory(inventory, incoming))
                .orElseGet(() -> insertIntoBackpack(backpack, incoming));

        int storedAmount = before - incoming.getCount();

        if (storedAmount > 0) {
            sendStoreFeedback(player, storedAmount);
        } else if (!incoming.isEmpty()) {
            sendFullFeedback(player);
        }

        return changed;
    }

    private static void sendStoreFeedback(ServerPlayerEntity player, int storedAmount) {
        if (!PlayerCooldownManager.tryUse(
                player,
                "food_backpack_store_feedback",
                STORE_FEEDBACK_COOLDOWN_TICKS
        )) {
            return;
        }

        player.sendMessage(
                Text.translatable("message.mythicrpg.food_backpack.stored")
                        .formatted(Formatting.LIGHT_PURPLE),
                true
        );

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ITEM_BUNDLE_INSERT,
                SoundCategory.PLAYERS,
                0.35f,
                1.25f
        );
    }

    private static void sendFullFeedback(ServerPlayerEntity player) {
        if (!PlayerCooldownManager.tryUse(
                player,
                "food_backpack_full_feedback",
                FULL_FEEDBACK_COOLDOWN_TICKS
        )) {
            return;
        }

        player.sendMessage(
                Text.translatable("message.mythicrpg.food_backpack.full")
                        .formatted(Formatting.RED),
                true
        );

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_CHEST_LOCKED,
                SoundCategory.PLAYERS,
                0.35f,
                1.0f
        );
    }

    private static ItemStack findBackpack(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);

            if (stack.isOf(ModItems.FOOD_BACKPACK)) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    private static boolean insertIntoBackpack(ItemStack backpack, ItemStack incoming) {
        boolean changed = ItemContainerUtils.insert(backpack, BACKPACK_SLOTS, incoming);
        if (changed) {
            FoodBackpackPerishableIndex.refresh(backpack, BACKPACK_SLOTS);
        }
        return changed;
    }

    private static boolean insertIntoInventory(Inventory inventory, ItemStack incoming) {
        boolean changed = false;
        for (int slot = 0; slot < inventory.size() && !incoming.isEmpty(); slot++) {
            ItemStack existing = inventory.getStack(slot);
            if (existing.isEmpty() || !ItemStack.areItemsAndComponentsEqual(existing, incoming)) {
                continue;
            }
            int moved = Math.min(existing.getMaxCount() - existing.getCount(), incoming.getCount());
            if (moved > 0) {
                existing.increment(moved);
                incoming.decrement(moved);
                changed = true;
            }
        }
        for (int slot = 0; slot < inventory.size() && !incoming.isEmpty(); slot++) {
            if (!inventory.getStack(slot).isEmpty()) {
                continue;
            }
            int moved = Math.min(incoming.getMaxCount(), incoming.getCount());
            ItemStack stored = incoming.copyWithCount(moved);
            inventory.setStack(slot, stored);
            incoming.decrement(moved);
            changed = true;
        }
        if (changed) {
            inventory.markDirty();
        }
        return changed;
    }

    private static boolean updateInventoryPreservation(
            Inventory inventory,
            long gameTime,
            EatingPreservationManager.PreservationMode mode
    ) {
        boolean changed = false;
        for (int slot = 0; slot < inventory.size(); slot++) {
            changed |= EatingPreservationManager.updateStack(inventory.getStack(slot), gameTime, mode);
        }
        if (changed) {
            inventory.markDirty();
        }
        return changed;
    }

    public static boolean isAcceptedItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        // Important : jamais de backpack dans le backpack.
        if (stack.isOf(ModItems.FOOD_BACKPACK)) {
            return false;
        }

        return isFoodItem(stack)
                || isPlantItem(stack)
                || isFarmingResource(stack);
    }

    private static boolean isFoodItem(ItemStack stack) {
        return EatingFoodStorage.isFood(stack);
    }

    private static boolean isFarmingResource(ItemStack stack) {
        return stack.isOf(Items.BONE_MEAL)
                || stack.isOf(Items.BONE_BLOCK)
                || stack.isOf(ModItems.ENCHANTED_SEED);
    }

    private static boolean isPlantItem(ItemStack stack) {
        return stack.isIn(ItemTags.SAPLINGS)
                || stack.isIn(ItemTags.LEAVES)
                || stack.isIn(ItemTags.FLOWERS)
                || stack.isOf(Items.WHEAT_SEEDS)
                || stack.isOf(Items.PUMPKIN_SEEDS)
                || stack.isOf(Items.MELON_SEEDS)
                || stack.isOf(Items.BEETROOT_SEEDS)
                || stack.isOf(Items.WHEAT)
                || stack.isOf(Items.NETHER_WART)
                || stack.isOf(Items.COCOA_BEANS)
                || stack.isOf(Items.SWEET_BERRIES)
                || stack.isOf(Items.GLOW_BERRIES)
                || stack.isOf(Items.BAMBOO)
                || stack.isOf(Items.CACTUS)
                || stack.isOf(Items.SUGAR_CANE)
                || stack.isOf(Items.KELP)
                || stack.isOf(Items.DRIED_KELP)
                || stack.isOf(Items.SEAGRASS)
                || stack.isOf(Items.SEA_PICKLE)
                || stack.isOf(Items.VINE)
                || stack.isOf(Items.TWISTING_VINES)
                || stack.isOf(Items.WEEPING_VINES)
                || stack.isOf(Items.LILY_PAD)
                || stack.isOf(Items.MOSS_BLOCK)
                || stack.isOf(Items.MOSS_CARPET)
                || stack.isOf(Items.BIG_DRIPLEAF)
                || stack.isOf(Items.SMALL_DRIPLEAF)
                || stack.isOf(Items.SPORE_BLOSSOM)
                || stack.isOf(Items.HANGING_ROOTS)
                || stack.isOf(Items.ROOTED_DIRT)
                || stack.isOf(Items.BROWN_MUSHROOM)
                || stack.isOf(Items.RED_MUSHROOM)
                || stack.isOf(Items.BROWN_MUSHROOM_BLOCK)
                || stack.isOf(Items.RED_MUSHROOM_BLOCK)
                || stack.isOf(Items.MUSHROOM_STEM)
                || stack.isOf(Items.CHORUS_FRUIT)
                || stack.isOf(Items.CHORUS_FLOWER)
                || stack.isOf(Items.CARROT)
                || stack.isOf(Items.POTATO)
                || stack.isOf(Items.BEETROOT)
                || stack.isOf(Items.MELON)
                || stack.isOf(Items.MELON_SLICE)
                || stack.isOf(Items.PUMPKIN)
                || stack.isOf(Items.CARVED_PUMPKIN);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack backpack = user.getStackInHand(hand);

        if (world.isClient) {
            return TypedActionResult.success(backpack);
        }

        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.success(backpack);
        }

        FoodBackpackDeathData.activate(backpack, serverPlayer);
        EatingPreservationManager.updateContainer(
                backpack,
                BACKPACK_SLOTS,
                world.getTime(),
                EatingPreservationManager.modeForPlayer(serverPlayer)
        );
        int sourceInventorySlot = getSourceInventorySlot(serverPlayer, hand);

        FoodBackpackInventory liveInventory = new FoodBackpackInventory(backpack);
        FoodBackpackSessionManager.open(serverPlayer, backpack, liveInventory);
        serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) -> new FoodBackpackScreenHandler(
                        syncId,
                        playerInventory,
                        liveInventory,
                        sourceInventorySlot,
                        FoodBackpackDeathData.getBackpackId(backpack)
                ),
                Text.empty()
        ));

        return TypedActionResult.success(backpack);
    }

    private static int getSourceInventorySlot(ServerPlayerEntity player, Hand hand) {
        if (hand == Hand.OFF_HAND) {
            return 40;
        }

        return player.getInventory().selectedSlot;
    }


    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        int totalItems = ItemContainerUtils.countItems(stack, BACKPACK_SLOTS);
        int usedSlots = ItemContainerUtils.countUsedSlots(stack, BACKPACK_SLOTS);

        tooltip.add(Text.translatable("tooltip.mythicrpg.food_backpack.description")
                .formatted(Formatting.GRAY));

        tooltip.add(Text.translatable("tooltip.mythicrpg.food_backpack.stored", totalItems)
                .formatted(Formatting.GOLD));

        tooltip.add(Text.translatable("tooltip.mythicrpg.food_backpack.slots", usedSlots, 54)
                .formatted(Formatting.YELLOW));

        int deathCount = FoodBackpackDeathData.getDeathCount(stack);
        boolean titleThresholdReached = deathCount >= FoodBackpackDeathData.TITLE_DEATH_REQUIREMENT;
        tooltip.add((titleThresholdReached
                        ? Text.translatable("tooltip.mythicrpg.food_backpack.deaths_unlocked", deathCount)
                        : Text.translatable(
                                "tooltip.mythicrpg.food_backpack.deaths",
                                deathCount,
                                FoodBackpackDeathData.TITLE_DEATH_REQUIREMENT
                        ))
                .formatted(titleThresholdReached ? Formatting.GOLD : Formatting.GRAY));

        tooltip.add(Text.translatable("tooltip.mythicrpg.food_backpack.open")
                .formatted(Formatting.GREEN));
    }

    private static final class FoodBackpackInventory extends SimpleInventory implements FoodBackpackSessionManager.FlushableSessionInventory {
        private final ItemStack backpack;
        private boolean loading;
        private boolean dirty;

        private FoodBackpackInventory(ItemStack backpack) {
            super(BACKPACK_SLOTS);
            this.backpack = backpack;
            loadFromBackpack();
        }

        private void loadFromBackpack() {
            loading = true;

            DefaultedList<ItemStack> contents = ItemContainerUtils.read(backpack, BACKPACK_SLOTS);

            for (int i = 0; i < contents.size(); i++) {
                setStack(i, contents.get(i));
            }

            loading = false;
        }

        private void saveToBackpack() {
            DefaultedList<ItemStack> contents = DefaultedList.ofSize(BACKPACK_SLOTS, ItemStack.EMPTY);

            for (int i = 0; i < size(); i++) {
                contents.set(i, getStack(i).copy());
            }

            ItemContainerUtils.write(backpack, contents);
            FoodBackpackPerishableIndex.refresh(backpack, this);
            dirty = false;
        }

        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return stack.isEmpty() || FoodBackpackItem.isAcceptedItem(stack);
        }

        @Override
        public void markDirty() {
            super.markDirty();

            if (!loading) {
                dirty = true;
            }
        }



        @Override
        public void mythicrpg$flushIfDirty() {
            if (dirty && !loading) {
                saveToBackpack();
            }
        }

        @Override
        public void mythicrpg$flushNow() {
            if (!loading) {
                saveToBackpack();
            }
        }

        @Override
        public void onClose(PlayerEntity player) {
            super.onClose(player);
            if (!player.getWorld().isClient() && player instanceof ServerPlayerEntity serverPlayer) {
                EatingPreservationManager.PreservationMode mode =
                        EatingPreservationManager.hasPortableFridge(serverPlayer)
                                ? EatingPreservationManager.PreservationMode.PORTABLE_CONTINUOUS
                                : EatingPreservationManager.PreservationMode.NONE;
                long gameTime = player.getWorld().getTime();
                for (int slot = 0; slot < size(); slot++) {
                    EatingPreservationManager.updateStack(getStack(slot), gameTime, mode);
                }
            }
            saveToBackpack();
            if (player instanceof ServerPlayerEntity serverPlayer) {
                FoodBackpackSessionManager.close(serverPlayer, this);
            }
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable(this.getTranslationKey(stack))
                .formatted(Formatting.LIGHT_PURPLE);
    }
}