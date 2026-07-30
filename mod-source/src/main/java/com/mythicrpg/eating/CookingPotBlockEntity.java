package com.mythicrpg.eating;

import com.mythicrpg.fishing.FishingCatchData;
import com.mythicrpg.fishing.FishingDishEffectData;
import com.mythicrpg.fishing.FishingFamily;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.core.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Optional;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class CookingPotBlockEntity extends LockableContainerBlockEntity implements SidedInventory {
    public static final int INPUT_SLOT_START = 0;
    public static final int INPUT_SLOT_END = 5;
    public static final int RESULT_SLOT = 5;
    public static final int INVENTORY_SIZE = 6;

    public static final int PROPERTY_REMAINING_TICKS = 0;
    public static final int PROPERTY_TOTAL_TICKS = 1;
    public static final int PROPERTY_STATUS = 2;
    public static final int PROPERTY_RARITY = 3;
    public static final int PROPERTY_CATEGORY = 4;
    public static final int PROPERTY_PORTIONS = 5;
    public static final int PROPERTY_ALLOWED_SLOTS = 6;
    public static final int PROPERTY_HEAT = 7;
    public static final int PROPERTY_SIGNATURE_UNLOCKED = 8;
    public static final int PROPERTY_SIGNATURE_PREPARED = 9;
    public static final int PROPERTY_COUNT = 10;

    /** Phase 1 deliberately keeps the pot manual: hoppers cannot bypass bowl or perk rules. */
    private static final int[] NO_AUTOMATION_SLOTS = new int[0];

    private DefaultedList<ItemStack> items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
    private boolean processing;
    private int remainingTicks;
    private int totalDurationTicks;
    private String outputRecipeId = "dubious_dish";
    private DishRarity outputRarity = DishRarity.COMMON;
    private DishCategory outputCategory = DishCategory.MAIN;
    private int outputScore;
    private int outputShelfLifeDays = 2;
    private int readyPortions;
    private long outputCreatedAt;
    private boolean outputDubious;
    private FishingFamily outputFishingEffect;
    private UUID chefUuid;
    private boolean codexRecorded;
    private String signatureName = "";
    private String signatureIcon = "minecraft:bowl";
    private SignatureBonus signatureBonus = SignatureBonus.DAMAGE;
    private int signatureDurationTicks;
    private boolean signaturePrepared;
    private UUID signaturePreparedByUuid;
    private boolean signatureMutationGuard;
    private List<String> signatureIngredientIds = List.of();

    private final PropertyDelegate machineProperties = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case PROPERTY_REMAINING_TICKS -> getRemainingTicksForSync();
                case PROPERTY_TOTAL_TICKS -> totalDurationTicks;
                case PROPERTY_STATUS -> processing ? 1 : (readyPortions > 0 ? 2 : 0);
                case PROPERTY_RARITY -> outputRarity.rank();
                case PROPERTY_CATEGORY -> outputCategory.ordinal();
                case PROPERTY_PORTIONS -> readyPortions;
                case PROPERTY_HEAT -> world != null && CookingHeatRegistry.isHeatSource(world.getBlockState(pos.down())) ? 1 : 0;
                case PROPERTY_SIGNATURE_PREPARED -> signaturePrepared ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Server-authoritative.
        }

        @Override
        public int size() {
            return PROPERTY_COUNT;
        }
    };

    public CookingPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOKING_POT, pos, state);
    }

    public PropertyDelegate screenProperties(int allowedSlots, boolean signatureUnlocked) {
        int safeSlots = Math.max(0, Math.min(5, allowedSlots));
        return new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (index == PROPERTY_ALLOWED_SLOTS) {
                    return safeSlots;
                }
                if (index == PROPERTY_SIGNATURE_UNLOCKED) {
                    return signatureUnlocked ? 1 : 0;
                }
                return machineProperties.get(index);
            }

            @Override
            public void set(int index, int value) {
                // Server-authoritative.
            }

            @Override
            public int size() {
                return PROPERTY_COUNT;
            }
        };
    }

    public boolean isProcessing() {
        return processing;
    }

    public boolean hasReadyPortions() {
        return readyPortions > 0 && !items.get(RESULT_SLOT).isEmpty();
    }

    public boolean isLockedForBreaking() {
        return processing || readyPortions > 0;
    }

    public boolean startCooking(ServerPlayerEntity player, int allowedSlots) {
        if (world == null
                || world.isClient
                || processing
                || readyPortions > 0
                || !items.get(RESULT_SLOT).isEmpty()
                || signaturePrepared
                || !CookingHeatRegistry.isHeatSource(world.getBlockState(pos.down()))) {
            return false;
        }

        int maxSlots = Math.max(2, Math.min(5, allowedSlots));
        ArrayList<ItemStack> inputs = new ArrayList<>();
        for (int slot = INPUT_SLOT_START; slot < INPUT_SLOT_END; slot++) {
            ItemStack stack = items.get(slot);
            if (slot >= maxSlots && !stack.isEmpty()) {
                return false;
            }
            if (slot < maxSlots && !stack.isEmpty()) {
                inputs.add(stack.copyWithCount(1));
            }
        }

        Optional<CookingResult> resolved = CookingRecipeRegistry.resolve(inputs);
        if (resolved.isEmpty()) {
            return false;
        }
        CookingResult result = resolved.get();
        outputFishingEffect = fishingEffect(inputs);
        outputRecipeId = result.recipe().id();
        outputRarity = result.rarity();
        if (!result.dubious()
                && EatingPerks.hasRarityUpgrade(player)
                && outputRarity != DishRarity.MYTHIC
                && world.random.nextDouble() < 0.15) {
            outputRarity = DishRarity.byRank(outputRarity.rank() + 1);
            player.sendMessage(
                    Text.translatable("message.mythicrpg.eating.rarity_upgrade", outputRarity.displayName()),
                    true
            );
        }
        outputCategory = result.recipe().category();
        outputScore = result.score();
        outputShelfLifeDays = result.recipe().shelfLifeDays();
        outputDubious = result.dubious();
        readyPortions = result.portions();
        chefUuid = player.getUuid();
        codexRecorded = false;
        clearSignatureData();

        for (int slot = INPUT_SLOT_START; slot < maxSlots; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Item remainder = stack.getItem().getRecipeRemainder();
            stack.decrement(1);
            if (stack.isEmpty() && remainder != null) {
                items.set(slot, new ItemStack(remainder));
            }
        }

        totalDurationTicks = EatingBalance.COOKING_BASE_TICKS
                + readyPortions * EatingBalance.COOKING_TICKS_PER_PORTION;
        remainingTicks = totalDurationTicks;
        processing = true;

        // The ingredients are committed and the pot cannot be broken while processing.
        // Recording here guarantees that the chef keeps the discovery even if they log out
        // while another player keeps the chunk loaded until the cooking completes.
        EatingCodexManager.recordPreparation(
                player,
                outputRecipeId,
                outputRarity,
                readyPortions,
                outputShelfLifeDays
        );
        codexRecorded = true;
        markDirtyAndSync();

        if (world instanceof ServerWorld serverWorld) {
            serverWorld.playSound(null, pos, SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE,
                    SoundCategory.BLOCKS, 0.65F, 0.9F);
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    6, 0.2, 0.12, 0.2, 0.01);
        }
        return true;
    }

    public SignaturePrepareResult prepareSignatureIngredients(
            ServerPlayerEntity player,
            ChefNotebookData.Configuration configuration,
            int allowedSlots
    ) {
        if (world == null
                || world.isClient
                || processing
                || readyPortions > 0
                || !items.get(RESULT_SLOT).isEmpty()
                || !EatingPerks.hasSignatureDish(player)
                || configuration == null
                || !configuration.isValid()) {
            return SignaturePrepareResult.INVALID;
        }
        for (int slot = INPUT_SLOT_START; slot < INPUT_SLOT_END; slot++) {
            if (!items.get(slot).isEmpty()) {
                return SignaturePrepareResult.POT_NOT_EMPTY;
            }
        }

        int maxSlots = Math.max(2, Math.min(5, allowedSlots));
        if (configuration.ingredients().size() > maxSlots) {
            return SignaturePrepareResult.TOO_MANY_INGREDIENTS;
        }

        ArrayList<Integer> inventorySlots = new ArrayList<>();
        Set<Integer> reservedSlots = new HashSet<>();
        for (SignatureIngredient ingredientTemplate : configuration.ingredients()) {
            Identifier ingredientId = ingredientTemplate.itemId();
            int foundSlot = -1;
            for (int slot = 0; slot < player.getInventory().size(); slot++) {
                if (reservedSlots.contains(slot)) {
                    continue;
                }
                ItemStack candidate = player.getInventory().getStack(slot);
                if (!candidate.isEmpty()
                        && Registries.ITEM.getId(candidate.getItem()).equals(ingredientId)
                        && ingredientTemplate.matches(candidate)) {
                    foundSlot = slot;
                    break;
                }
            }
            if (foundSlot < 0) {
                return SignaturePrepareResult.MISSING_INGREDIENT;
            }
            reservedSlots.add(foundSlot);
            inventorySlots.add(foundSlot);
        }

        signatureMutationGuard = true;
        try {
            for (int index = 0; index < inventorySlots.size(); index++) {
                ItemStack source = player.getInventory().getStack(inventorySlots.get(index));
                ItemStack transferred = source.copyWithCount(1);
                source.decrement(1);
                items.set(index, transferred);
            }
        } finally {
            signatureMutationGuard = false;
        }

        signaturePrepared = true;
        signaturePreparedByUuid = player.getUuid();
        signatureName = configuration.name();
        signatureIcon = configuration.icon().toString();
        signatureBonus = configuration.bonus();
        signatureDurationTicks = 0;
        signatureIngredientIds = configuration.ingredientIds().stream().map(Identifier::toString).toList();
        player.getInventory().markDirty();
        markDirtyAndSync();
        EatingDeliveryManager.refreshPreparedSignatureIndex(this);
        return SignaturePrepareResult.SUCCESS;
    }

    public boolean startPreparedSignatureCooking(
            ServerPlayerEntity player,
            ChefNotebookData.Configuration configuration,
            int allowedSlots
    ) {
        if (signaturePrepared && signaturePreparedByUuid == null) {
            clearPreparedSignature();
            return false;
        }
        if (signaturePrepared
                && signaturePreparedByUuid != null
                && signaturePreparedByUuid.equals(player.getUuid())
                && !EatingPerks.hasSignatureDish(player)) {
            clearPreparedSignature();
            return false;
        }
        if (world == null
                || world.isClient
                || !signaturePrepared
                || processing
                || readyPortions > 0
                || !items.get(RESULT_SLOT).isEmpty()
                || !CookingHeatRegistry.isHeatSource(world.getBlockState(pos.down()))
                || !EatingPerks.hasSignatureDish(player)
                || signaturePreparedByUuid == null
                || !signaturePreparedByUuid.equals(player.getUuid())) {
            return false;
        }
        if (configuration == null
                || !configuration.isValid()
                || !matchesPreparedConfiguration(configuration)) {
            clearPreparedSignature();
            return false;
        }

        int maxSlots = Math.max(2, Math.min(5, allowedSlots));
        if (signatureIngredientIds.size() < 2 || signatureIngredientIds.size() > maxSlots) {
            clearPreparedSignature();
            return false;
        }

        ArrayList<ItemStack> inputs = new ArrayList<>();
        for (int slot = INPUT_SLOT_START; slot < INPUT_SLOT_END; slot++) {
            ItemStack stack = items.get(slot);
            if (slot >= signatureIngredientIds.size()) {
                if (!stack.isEmpty()) {
                    clearPreparedSignature();
                    return false;
                }
                continue;
            }
            Identifier expected = Identifier.tryParse(signatureIngredientIds.get(slot));
            if (expected == null
                    || stack.isEmpty()
                    || !Registries.ITEM.getId(stack.getItem()).equals(expected)
                    || !CulinaryIngredientRegistry.isCulinaryIngredient(stack)) {
                clearPreparedSignature();
                return false;
            }
            inputs.add(stack.copyWithCount(1));
        }

        Optional<CookingResult> resolved = CookingRecipeRegistry.resolveSignature(inputs);
        if (resolved.isEmpty()) {
            clearPreparedSignature();
            return false;
        }
        CookingResult base = resolved.get();
        outputFishingEffect = fishingEffect(inputs);
        outputRecipeId = "signature_dish";
        outputRarity = base.rarity();
        if (EatingPerks.hasRarityUpgrade(player)
                && outputRarity != DishRarity.MYTHIC
                && world.random.nextDouble() < 0.15) {
            outputRarity = DishRarity.byRank(outputRarity.rank() + 1);
        }
        outputCategory = base.recipe().category();
        outputScore = base.score();
        outputShelfLifeDays = base.recipe().shelfLifeDays();
        outputDubious = false;
        readyPortions = inputs.size();
        chefUuid = player.getUuid();
        codexRecorded = false;
        signatureDurationTicks = Math.max(200, Math.min(600, 100 + outputScore * 20));

        for (int slot = 0; slot < signatureIngredientIds.size(); slot++) {
            ItemStack stack = items.get(slot);
            Item remainder = stack.getItem().getRecipeRemainder();
            stack.decrement(1);
            if (stack.isEmpty() && remainder != null) {
                items.set(slot, new ItemStack(remainder));
            }
        }

        signaturePrepared = false;
        signaturePreparedByUuid = null;
        signatureIngredientIds = List.of();
        EatingDeliveryManager.untrackPreparedSignature(this);
        totalDurationTicks = EatingBalance.COOKING_BASE_TICKS
                + readyPortions * EatingBalance.COOKING_TICKS_PER_PORTION;
        remainingTicks = totalDurationTicks;
        processing = true;
        EatingCodexManager.recordPreparation(
                player,
                outputRecipeId,
                outputRarity,
                readyPortions,
                outputShelfLifeDays
        );
        codexRecorded = true;
        markDirtyAndSync();
        return true;
    }

    private boolean matchesPreparedConfiguration(ChefNotebookData.Configuration configuration) {
        if (!signatureName.equals(configuration.name())
                || !signatureIcon.equals(configuration.icon().toString())
                || signatureBonus != configuration.bonus()
                || !signatureIngredientIds.equals(
                        configuration.ingredientIds().stream().map(Identifier::toString).toList()
                )) {
            return false;
        }
        for (int slot = 0; slot < configuration.ingredients().size(); slot++) {
            if (!configuration.ingredients().get(slot).matches(items.get(slot))) {
                return false;
            }
        }
        return true;
    }

    private void clearPreparedSignature() {
        clearSignatureData();
        markDirtyAndSync();
    }

    public boolean invalidatePreparedSignatureOwnedBy(UUID playerUuid) {
        if (!signaturePrepared
                || signaturePreparedByUuid == null
                || !signaturePreparedByUuid.equals(playerUuid)) {
            return false;
        }
        clearPreparedSignature();
        return true;
    }

    public boolean isSignaturePrepared() {
        return signaturePrepared;
    }

    public UUID getPreparedSignatureOwner() {
        return signaturePrepared ? signaturePreparedByUuid : null;
    }

    public boolean hasHeatSource() {
        return world != null && CookingHeatRegistry.isHeatSource(world.getBlockState(pos.down()));
    }

    public enum SignaturePrepareResult {
        SUCCESS,
        INVALID,
        POT_NOT_EMPTY,
        TOO_MANY_INGREDIENTS,
        MISSING_INGREDIENT
    }

    public int deliverTo(ServerPlayerEntity player, int maximum) {
        if (maximum <= 0
                || chefUuid == null
                || !chefUuid.equals(player.getUuid())
                || !hasReadyPortions()
                || world == null) {
            return 0;
        }
        PreparedDishData.Dish currentDish = PreparedDishData.refreshExpiration(
                items.get(RESULT_SLOT),
                world.getTime()
        );
        if (currentDish.dubious()) {
            markOutputDubious();
            items.set(RESULT_SLOT, createOutputStack());
            markDirtyAndSync();
            return 0;
        }
        int delivered = 0;
        while (delivered < maximum && hasReadyPortions()) {
            int bowlSlot = player.isCreative() ? -1 : findDeliveryBowlSlot(player);
            if (!player.isCreative() && bowlSlot < 0) {
                break;
            }

            if (bowlSlot >= 0) {
                player.getInventory().getStack(bowlSlot).decrement(1);
            }

            ItemStack portion = items.get(RESULT_SLOT).copyWithCount(1);
            if (!player.getInventory().insertStack(portion) || !portion.isEmpty()) {
                if (bowlSlot >= 0) {
                    restoreDeliveryBowl(player, bowlSlot);
                }
                break;
            }

            claimPortion(player);
            delivered++;
        }
        if (delivered > 0) {
            player.getInventory().markDirty();
        }
        return delivered;
    }

    private static int findDeliveryBowlSlot(ServerPlayerEntity player) {
        for (int slot = 0; slot < player.getInventory().size(); slot++) {
            if (player.getInventory().getStack(slot).isOf(net.minecraft.item.Items.BOWL)) {
                return slot;
            }
        }
        return -1;
    }

    private static void restoreDeliveryBowl(ServerPlayerEntity player, int preferredSlot) {
        ItemStack preferred = player.getInventory().getStack(preferredSlot);
        if (preferred.isEmpty()) {
            player.getInventory().setStack(preferredSlot, new ItemStack(net.minecraft.item.Items.BOWL));
            return;
        }
        if (preferred.isOf(net.minecraft.item.Items.BOWL) && preferred.getCount() < preferred.getMaxCount()) {
            preferred.increment(1);
            return;
        }
        ItemStack bowl = new ItemStack(net.minecraft.item.Items.BOWL);
        if (!player.getInventory().insertStack(bowl) && !bowl.isEmpty()) {
            player.dropItem(bowl, false);
        }
    }

    public boolean claimPortion(PlayerEntity player) {
        if (readyPortions <= 0) {
            return false;
        }
        readyPortions--;
        if (readyPortions <= 0) {
            items.set(RESULT_SLOT, ItemStack.EMPTY);
            readyPortions = 0;
            totalDurationTicks = 0;
            outputCreatedAt = 0L;
            clearSignatureData();
        } else {
            items.set(RESULT_SLOT, createOutputStack());
        }
        recordCodexIfPossible(player);
        markDirtyAndSync();
        return true;
    }

    private void recordCodexIfPossible(PlayerEntity claimant) {
        if (codexRecorded || chefUuid == null || world == null || world.isClient) {
            return;
        }
        ServerPlayerEntity chef = null;
        if (world.getServer() != null) {
            chef = world.getServer().getPlayerManager().getPlayer(chefUuid);
        }
        if (chef == null && claimant instanceof ServerPlayerEntity serverClaimant
                && serverClaimant.getUuid().equals(chefUuid)) {
            chef = serverClaimant;
        }
        if (chef != null) {
            EatingCodexManager.recordPreparation(
                    chef,
                    outputRecipeId,
                    outputRarity,
                    Math.max(1, readyPortions),
                    outputShelfLifeDays
            );
            codexRecorded = true;
        }
    }

    private int getRemainingTicks() {
        return processing ? Math.max(0, remainingTicks) : 0;
    }

    private int getRemainingTicksForSync() {
        int remaining = getRemainingTicks();
        return remaining <= 0 ? 0 : ((remaining + 4) / 5) * 5;
    }

    public static void serverTick(
            World world,
            BlockPos pos,
            BlockState state,
            CookingPotBlockEntity blockEntity
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        long time = serverWorld.getTime();
        if (blockEntity.signaturePrepared
                && blockEntity.signaturePreparedByUuid != null
                && Math.floorMod(time, 20L) == Math.floorMod(pos.asLong(), 20L)) {
            ServerPlayerEntity owner = serverWorld.getServer().getPlayerManager()
                    .getPlayer(blockEntity.signaturePreparedByUuid);
            if (owner != null && !EatingPerks.hasSignatureDish(owner)) {
                blockEntity.clearPreparedSignature();
            }
        }
        if (!blockEntity.processing) {
            blockEntity.refreshReadyDishExpiration(serverWorld, time);
            return;
        }

        if (!CookingHeatRegistry.isHeatSource(serverWorld.getBlockState(pos.down()))) {
            return;
        }

        if (Math.floorMod(time, 10L) == Math.floorMod(pos.asLong(), 10L)) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                    1, 0.16, 0.08, 0.16, 0.0);
        }
        if (blockEntity.remainingTicks > 0) {
            blockEntity.remainingTicks--;
            if (blockEntity.remainingTicks > 0 && blockEntity.remainingTicks % 20 == 0) {
                // Persist the countdown once per second without dirtying the chunk every tick.
                blockEntity.markDirty();
            }
        }
        if (blockEntity.remainingTicks > 0) {
            return;
        }

        blockEntity.outputCreatedAt = time;
        blockEntity.items.set(RESULT_SLOT, blockEntity.createOutputStack());
        blockEntity.processing = false;
        blockEntity.remainingTicks = 0;
        blockEntity.recordCodexIfPossible(null);
        blockEntity.markDirtyAndSync();
        EatingAdvancedManager.onCookingCompleted(serverWorld, pos, blockEntity.chefUuid);

        serverWorld.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.25, 0.2, 0.25, 0.01);
        serverWorld.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN,
                SoundCategory.BLOCKS, 0.8F, 1.15F);
    }

    private void refreshReadyDishExpiration(ServerWorld serverWorld, long time) {
        if (readyPortions <= 0
                || items.get(RESULT_SLOT).isEmpty()
                || Math.floorMod(time, 20L) != Math.floorMod(pos.asLong(), 20L)) {
            return;
        }
        PreparedDishData.Dish dish = PreparedDishData.refreshExpiration(
                items.get(RESULT_SLOT),
                time
        );
        if (!dish.dubious() || outputDubious) {
            return;
        }
        markOutputDubious();
        items.set(RESULT_SLOT, createOutputStack());
        markDirtyAndSync();
        serverWorld.playSound(
                null,
                pos,
                SoundEvents.BLOCK_BREWING_STAND_BREW,
                SoundCategory.BLOCKS,
                0.35F,
                0.55F
        );
    }


    private static FishingFamily fishingEffect(List<ItemStack> inputs) {
        boolean hasVoid = false;
        for (ItemStack stack : inputs) {
            var caught = FishingCatchData.read(stack);
            if (caught.isEmpty()) continue;
            if (caught.get().family() == FishingFamily.INFERNAL) return FishingFamily.INFERNAL;
            if (caught.get().family() == FishingFamily.VOID) hasVoid = true;
        }
        return hasVoid ? FishingFamily.VOID : null;
    }

    private void markOutputDubious() {
        outputRecipeId = "dubious_dish";
        outputRarity = DishRarity.COMMON;
        outputCategory = DishCategory.MAIN;
        outputScore = 0;
        outputDubious = true;
        outputFishingEffect = null;
        clearSignatureData();
    }

    private ItemStack createOutputStack() {
        CookingRecipe recipe = CookingRecipeRegistry.byId(outputRecipeId)
                .orElseGet(() -> CookingRecipeRegistry.byId("dubious_dish").orElseThrow());
        CookingResult result = new CookingResult(
                new CookingRecipe(
                        recipe.id(),
                        outputCategory,
                        outputRarity,
                        outputShelfLifeDays,
                        recipe.ingredients(),
                        recipe.improvised()
                ),
                outputRarity,
                Math.max(1, readyPortions),
                outputScore,
                outputDubious
        );
        long createdAt = outputCreatedAt > 0L
                ? outputCreatedAt
                : (world == null ? 0L : world.getTime());
        boolean signature = !outputDubious && !signatureName.isBlank() && signatureDurationTicks > 0;
        ItemStack output = signature
                ? PreparedDishData.createSignature(result, chefUuid, createdAt)
                : PreparedDishData.create(result, chefUuid, createdAt);
        if (outputFishingEffect != null) {
            FishingDishEffectData.write(output, outputFishingEffect);
        }
        if (signature) {
            Identifier icon = Identifier.tryParse(signatureIcon);
            SignatureDishData.write(
                    output,
                    signatureName,
                    icon == null ? Identifier.ofVanilla("bowl") : icon,
                    signatureBonus,
                    signatureDurationTicks
            );
        }
        return output;
    }

    private void clearSignatureData() {
        signatureName = "";
        signatureIcon = "minecraft:bowl";
        signatureBonus = SignatureBonus.DAMAGE;
        signatureDurationTicks = 0;
        signaturePrepared = false;
        signaturePreparedByUuid = null;
        signatureIngredientIds = List.of();
        EatingDeliveryManager.untrackPreparedSignature(this);
    }

    private void markDirtyAndSync() {
        markDirty();
        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    @Override
    protected Text getContainerName() {
        return Text.translatable("screen.mythicrpg.cooking_pot");
    }

    @Override
    protected DefaultedList<ItemStack> getHeldStacks() {
        return items;
    }

    @Override
    protected void setHeldStacks(DefaultedList<ItemStack> stacks) {
        items = stacks;
    }

    @Override
    protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
        int allowedSlots = 2;
        boolean signatureUnlocked = false;
        if (playerInventory.player instanceof ServerPlayerEntity serverPlayer) {
            allowedSlots = EatingPerks.maxIngredients(serverPlayer);
            signatureUnlocked = EatingPerks.hasSignatureDish(serverPlayer);
        }
        return new CookingPotScreenHandler(
                syncId,
                playerInventory,
                this,
                screenProperties(allowedSlots, signatureUnlocked)
        );
    }

    @Override
    public int size() {
        return INVENTORY_SIZE;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) {
            return false;
        }
        return player.squaredDistanceTo(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        ) <= 64.0;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack removed = Inventories.splitStack(items, slot, amount);
        if (!removed.isEmpty()) {
            invalidatePreparedSignatureOnMutation(slot);
            markDirtyAndSync();
        }
        return removed;
    }

    @Override
    public ItemStack removeStack(int slot) {
        ItemStack removed = Inventories.removeStack(items, slot);
        if (!removed.isEmpty()) {
            invalidatePreparedSignatureOnMutation(slot);
            markDirtyAndSync();
        }
        return removed;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        int maximum = Math.min(stack.getMaxCount(), getMaxCountPerStack());
        if (!stack.isEmpty() && stack.getCount() > maximum) {
            stack.setCount(maximum);
        }
        invalidatePreparedSignatureOnMutation(slot);
        markDirtyAndSync();
    }

    @Override
    public void clear() {
        items.clear();
        if (!signatureMutationGuard) {
            clearSignatureData();
        }
        markDirtyAndSync();
    }

    private void invalidatePreparedSignatureOnMutation(int slot) {
        if (!signatureMutationGuard
                && signaturePrepared
                && slot >= INPUT_SLOT_START
                && slot < INPUT_SLOT_END) {
            clearSignatureData();
        }
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return slot >= INPUT_SLOT_START
                && slot < INPUT_SLOT_END
                && !processing
                && readyPortions == 0
                && CulinaryIngredientRegistry.isCulinaryIngredient(stack);
    }


    @Override
    public int[] getAvailableSlots(Direction side) {
        return NO_AUTOMATION_SLOTS.clone();
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        items = DefaultedList.ofSize(INVENTORY_SIZE, ItemStack.EMPTY);
        Inventories.readNbt(nbt, items, registryLookup);
        processing = nbt.getBoolean("processing");
        remainingTicks = Math.max(0, Math.min(EatingBalance.COOKING_BASE_TICKS + 5 * EatingBalance.COOKING_TICKS_PER_PORTION, nbt.getInt("remaining_ticks")));
        totalDurationTicks = Math.max(0, Math.min(EatingBalance.COOKING_BASE_TICKS + 5 * EatingBalance.COOKING_TICKS_PER_PORTION, nbt.getInt("total_duration_ticks")));
        outputRecipeId = nbt.getString("output_recipe");
        outputRarity = DishRarity.byId(nbt.getString("output_rarity")).orElse(DishRarity.COMMON);
        outputCategory = DishCategory.byId(nbt.getString("output_category")).orElse(DishCategory.MAIN);
        outputScore = nbt.getInt("output_score");
        outputShelfLifeDays = Math.max(1, nbt.getInt("output_shelf_life_days"));
        readyPortions = Math.max(0, Math.min(5, nbt.getInt("ready_portions")));
        outputCreatedAt = nbt.getLong("output_created_at");
        outputDubious = nbt.getBoolean("output_dubious");
        outputFishingEffect = FishingFamily.byId(nbt.getString("output_fishing_effect")).orElse(null);
        codexRecorded = nbt.getBoolean("codex_recorded");
        signatureName = nbt.getString("signature_name");
        signatureIcon = nbt.getString("signature_icon");
        if (signatureIcon.isBlank()) {
            signatureIcon = "minecraft:bowl";
        }
        signatureBonus = SignatureBonus.byId(nbt.getString("signature_bonus")).orElse(SignatureBonus.DAMAGE);
        signatureDurationTicks = Math.max(0, Math.min(600, nbt.getInt("signature_duration")));
        signaturePrepared = nbt.getBoolean("signature_prepared");
        signaturePreparedByUuid = null;
        if (!nbt.getString("signature_prepared_by").isBlank()) {
            try {
                signaturePreparedByUuid = UUID.fromString(nbt.getString("signature_prepared_by"));
            } catch (IllegalArgumentException ignored) {
                signaturePrepared = false;
            }
        }
        String rawSignatureIngredients = nbt.getString("signature_ingredients");
        signatureIngredientIds = rawSignatureIngredients.isBlank()
                ? List.of()
                : java.util.Arrays.stream(rawSignatureIngredients.split(";"))
                        .limit(5)
                        .filter(id -> Identifier.tryParse(id) != null)
                        .toList();
        if (signaturePrepared
                && (signaturePreparedByUuid == null
                || signatureIngredientIds.size() < 2
                || signatureIngredientIds.size() > 5)) {
            clearSignatureData();
        }
        if (processing && (totalDurationTicks <= 0 || remainingTicks > totalDurationTicks)) {
            processing = false;
            remainingTicks = 0;
        }
        if (readyPortions > 0 && items.get(RESULT_SLOT).isEmpty() && !processing) {
            readyPortions = 0;
        }
        chefUuid = null;
        if (!nbt.getString("chef_uuid").isBlank()) {
            try {
                chefUuid = UUID.fromString(nbt.getString("chef_uuid"));
            } catch (IllegalArgumentException ignored) {
                MythicRPG.LOGGER.warn("Invalid cooking pot chef UUID at {}", pos);
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        Inventories.writeNbt(nbt, items, registryLookup);
        nbt.putBoolean("processing", processing);
        nbt.putInt("remaining_ticks", remainingTicks);
        nbt.putInt("total_duration_ticks", totalDurationTicks);
        nbt.putString("output_recipe", outputRecipeId);
        nbt.putString("output_rarity", outputRarity.id());
        nbt.putString("output_category", outputCategory.id());
        nbt.putInt("output_score", outputScore);
        nbt.putInt("output_shelf_life_days", outputShelfLifeDays);
        nbt.putInt("ready_portions", readyPortions);
        nbt.putLong("output_created_at", outputCreatedAt);
        nbt.putBoolean("output_dubious", outputDubious);
        nbt.putString("output_fishing_effect", outputFishingEffect == null ? "" : outputFishingEffect.id());
        nbt.putBoolean("codex_recorded", codexRecorded);
        nbt.putString("signature_name", signatureName);
        nbt.putString("signature_icon", signatureIcon);
        nbt.putString("signature_bonus", signatureBonus.id());
        nbt.putInt("signature_duration", signatureDurationTicks);
        nbt.putBoolean("signature_prepared", signaturePrepared);
        nbt.putString("signature_prepared_by", signaturePreparedByUuid == null ? "" : signaturePreparedByUuid.toString());
        nbt.putString("signature_ingredients", String.join(";", signatureIngredientIds));
        nbt.putString("chef_uuid", chefUuid == null ? "" : chefUuid.toString());
    }
}
