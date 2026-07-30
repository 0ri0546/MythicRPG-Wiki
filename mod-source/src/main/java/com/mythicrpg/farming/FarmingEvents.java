package com.mythicrpg.farming;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.core.SkillXpManager;
import com.mythicrpg.core.WorldScanUtils;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.Fertilizable;
import net.minecraft.util.ActionResult;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.block.ComposterBlock;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;

public final class FarmingEvents {
    private static final Map<ServerWorld, Map<BlockPos, Long>> RECENT_REPLANTS = new HashMap<>();
    private static boolean harvestingArea = false;
    private FarmingEvents() {
    }

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return true;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return true;
            }

            if (isRecentlyReplanted(serverWorld, pos)) {
                return false;
            }

            if (tryHandleFarmingBreak(serverPlayer, serverWorld, pos, state)) {
                return false;
            }

            return true;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);

            if (!stack.isOf(Items.BONE_MEAL)) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (!canUseBoneMealOn(serverWorld, pos, state)) {
                return ActionResult.PASS;
            }

            tryApplyBoneMealRegen(serverPlayer);

            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);

            if (!stack.isIn(ItemTags.HOES)) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (!canHoeCreateFarmland(serverWorld, pos, state)) {
                return ActionResult.PASS;
            }

            tryApplyIrrigatedStep(serverPlayer, serverWorld, pos);

            return ActionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return ActionResult.PASS;
            }

            if (!(world instanceof ServerWorld serverWorld)) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);

            if (!state.isOf(Blocks.COMPOSTER)) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);

            if (!canTriggerCompostTreasure(state, stack)) {
                return ActionResult.PASS;
            }

            tryDropCompostTreasure(serverPlayer, serverWorld, pos);

            return ActionResult.PASS;
        });
    }

    private static boolean isFarmingBlock(BlockState state) {
        Block block = state.getBlock();

        return block instanceof CropBlock
                || block instanceof NetherWartBlock
                || block instanceof CocoaBlock
                || block instanceof SweetBerryBushBlock
                || state.isOf(Blocks.MELON)
                || block instanceof PumpkinBlock
                || block instanceof MushroomBlock
                || state.isOf(Blocks.PUMPKIN);
    }

    private static boolean isMatureFarmingBlock(BlockState state) {
        Block block = state.getBlock();

        if (block instanceof CropBlock cropBlock) {
            return cropBlock.isMature(state);
        }

        if (block instanceof NetherWartBlock) {
            return state.get(NetherWartBlock.AGE) >= 3;
        }

        if (block instanceof CocoaBlock) {
            return state.get(CocoaBlock.AGE) >= 2;
        }

        if (block instanceof SweetBerryBushBlock) {
            return state.get(SweetBerryBushBlock.AGE) >= 3;
        }

        // Melon, pumpkin, mushrooms are considered mature farming blocks.
        return state.isOf(Blocks.MELON)
                || state.isOf(Blocks.PUMPKIN)
                || block instanceof MushroomBlock
                || state.isOf(Blocks.MUSHROOM_STEM);
    }

    private static int getFarmingXp(BlockState state) {
        Block block = state.getBlock();

        if (state.isOf(Blocks.MELON) || state.isOf(Blocks.PUMPKIN)) {
            return 4;
        }

        if (isMushroomBlock(state)) {
            return 3;
        }

        if (block instanceof NetherWartBlock || block instanceof CocoaBlock) {
            return 3;
        }

        return 2;
    }

    private static void tryDropEnchantedSeed(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        double chance = SkillTreeManager.getBonusTotal(
                player,
                SkillType.FARMING,
                BonusType.ENCHANTED_SEED_CHANCE
        );

        if (chance <= 0.0) {
            return;
        }

        if (world.random.nextDouble() >= chance) {
            return;
        }

        dropOrStoreFarmingStack(
                player,
                world,
                pos,
                new ItemStack(ModItems.ENCHANTED_SEED)
        );

        playEnchantedSeedFeedback(player, world, pos);
    }

    private static void playEnchantedSeedFeedback(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        world.spawnParticles(
                ParticleTypes.ENCHANT,
                pos.getX() + 0.5,
                pos.getY() + 0.8,
                pos.getZ() + 0.5,
                14,
                0.25,
                0.35,
                0.25,
                0.05
        );

        world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5,
                pos.getY() + 0.9,
                pos.getZ() + 0.5,
                6,
                0.18,
                0.20,
                0.18,
                0.02
        );

        world.playSound(
                null,
                pos,
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.55f,
                1.7f
        );

        player.sendMessage(
                Text.translatable("message.mythicrpg.enchanted_seed.found")
                        .formatted(Formatting.LIGHT_PURPLE),
                true
        );
    }

    private static void tryDoubleDrops(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state
    ) {
        double chance = SkillTreeManager.getBonusTotal(
                player,
                SkillType.FARMING,
                BonusType.FARMING_DOUBLE_DROP_CHANCE
        );

        if (chance <= 0.0) {
            return;
        }

        if (world.random.nextDouble() >= chance) {
            return;
        }

        for (ItemStack drop : Block.getDroppedStacks(state, world, pos, null, player, player.getMainHandStack())) {
            if (drop.isEmpty()) {
                continue;
            }

            dropOrStoreFarmingStack(player, world, pos, drop);
        }

        world.playSound(
                null,
                pos,
                SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS,
                0.35f,
                1.4f
        );
    }

    private static void tryVanillaXp(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        if (!SkillTreeManager.hasBonus(player, SkillType.FARMING, BonusType.FARMING_VANILLA_XP)) {
            return;
        }

        ExperienceOrbEntity.spawn(world, pos.toCenterPos(), 1);
    }

    private static BlockState getReplantedState(BlockState originalState) {
        Block block = originalState.getBlock();

        if (block instanceof CropBlock cropBlock) {
            return cropBlock.getDefaultState();
        }

        if (block instanceof NetherWartBlock) {
            return Blocks.NETHER_WART.getDefaultState();
        }

        if (block instanceof CocoaBlock) {
            // Cocoa needs a facing/support block, so skip for now.
            return null;
        }

        if (block instanceof SweetBerryBushBlock) {
            return Blocks.SWEET_BERRY_BUSH.getDefaultState();
        }

        return null;
    }

    private static boolean isMushroomBlock(BlockState state) {
        return state.isOf(Blocks.BROWN_MUSHROOM)
                || state.isOf(Blocks.RED_MUSHROOM)
                || state.isOf(Blocks.BROWN_MUSHROOM_BLOCK)
                || state.isOf(Blocks.RED_MUSHROOM_BLOCK)
                || state.isOf(Blocks.MUSHROOM_STEM);
    }

    private static boolean tryHandleFarmingBreak(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state
    ) {
        if (!isFarmingBlock(state)) {
            return false;
        }

        if (!isMatureFarmingBlock(state)) {
            return false;
        }

        BlockState replantedState = null;

        if (SkillTreeManager.hasBonus(player, SkillType.FARMING, BonusType.AUTO_REPLANT)) {
            replantedState = getReplantedState(state);
        }

        for (ItemStack drop : Block.getDroppedStacks(state, world, pos, null, player, player.getMainHandStack())) {
            if (!drop.isEmpty()) {
                dropOrStoreFarmingStack(player, world, pos, drop);
            }
        }

        int totalXp = getFarmingXp(state);
        tryDropEnchantedSeed(player, world, pos);
        tryDoubleDrops(player, world, pos, state);
        tryVanillaXp(player, world, pos);
        tryApplyCultivatedShield(player, state);
        totalXp += tryHarvestAroundWithHoe(player, world, pos, state);
        SkillXpManager.addXp(player, SkillType.FARMING, totalXp, false);

        if (replantedState != null) {
            world.setBlockState(pos, replantedState, Block.NOTIFY_ALL);
            protectReplantedCrop(world, pos);

            world.playSound(
                    null,
                    pos,
                    SoundEvents.ITEM_CROP_PLANT,
                    SoundCategory.BLOCKS,
                    0.6f,
                    1.1f
            );
        } else {
            world.breakBlock(pos, false, player);
        }

        return true;
    }

    private static void protectReplantedCrop(ServerWorld world, BlockPos pos) {
        RECENT_REPLANTS
                .computeIfAbsent(world, ignored -> new HashMap<>())
                .put(pos.toImmutable(), world.getTime() + 6);
    }

    private static boolean isRecentlyReplanted(ServerWorld world, BlockPos pos) {
        Map<BlockPos, Long> protectedPositions = RECENT_REPLANTS.get(world);

        if (protectedPositions == null) {
            return false;
        }

        Long protectedUntil = protectedPositions.get(pos);

        if (protectedUntil == null) {
            return false;
        }

        if (world.getTime() <= protectedUntil) {
            return true;
        }

        protectedPositions.remove(pos);

        if (protectedPositions.isEmpty()) {
            RECENT_REPLANTS.remove(world);
        }

        return false;
    }

    public static void cleanupRecentReplants(long currentTick) {
        RECENT_REPLANTS.entrySet().removeIf(worldEntry -> {
            Map<BlockPos, Long> protectedPositions = worldEntry.getValue();

            protectedPositions.entrySet().removeIf(entry -> currentTick > entry.getValue());

            return protectedPositions.isEmpty();
        });
    }

    private static boolean canUseBoneMealOn(ServerWorld world, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof Fertilizable fertilizable)) {
            return false;
        }

        return fertilizable.isFertilizable(world, pos, state)
                && fertilizable.canGrow(world, world.random, pos, state);
    }

    private static void tryApplyBoneMealRegen(ServerPlayerEntity player) {
        if (!SkillTreeManager.hasBonus(player, SkillType.FARMING, BonusType.BONE_MEAL_REGEN)) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.REGENERATION,
                60,
                0,
                false,
                true,
                true
        ));

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                SoundCategory.PLAYERS,
                0.35f,
                1.6f
        );
    }

    private static boolean canHoeCreateFarmland(ServerWorld world, BlockPos pos, BlockState state) {
        if (!world.getBlockState(pos.up()).isAir()) {
            return false;
        }

        return state.isOf(Blocks.GRASS_BLOCK)
                || state.isOf(Blocks.DIRT)
                || state.isOf(Blocks.COARSE_DIRT)
                || state.isOf(Blocks.DIRT_PATH)
                || state.isOf(Blocks.ROOTED_DIRT);
    }

    private static void tryApplyIrrigatedStep(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        if (!SkillTreeManager.hasBonus(player, SkillType.FARMING, BonusType.IRRIGATED_STEP)) {
            return;
        }

        if (!hasWaterNearby(world, pos, 4)) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.SPEED,
                60,
                0,
                false,
                true,
                true
        ));

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_GRASS_PLACE,
                SoundCategory.PLAYERS,
                0.45f,
                1.4f
        );
    }

    private static boolean hasWaterNearby(ServerWorld world, BlockPos center, int radius) {
        return WorldScanUtils.hasBlockInBox(
                world,
                center,
                radius,
                -1,
                1,
                state -> state.isOf(Blocks.WATER)
        );
    }

    private static void tryApplyCultivatedShield(ServerPlayerEntity player, BlockState state) {
        if (!SkillTreeManager.hasBonus(player, SkillType.FARMING, BonusType.CULTIVATED_SHIELD)) {
            return;
        }

        if (!isCultivatedShieldBlock(state)) {
            return;
        }

        StatusEffectInstance current = player.getStatusEffect(StatusEffects.ABSORPTION);

        int amplifier = 0;
        int duration = 200;

        if (current != null) {
            amplifier = Math.min(current.getAmplifier() + 1, 4);
            duration = Math.max(current.getDuration(), 200);
        }

        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.ABSORPTION,
                duration,
                amplifier,
                false,
                true,
                true
        ));

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ITEM_HONEY_BOTTLE_DRINK,
                SoundCategory.PLAYERS,
                0.45f,
                1.3f
        );
    }

    private static boolean isCultivatedShieldBlock(BlockState state) {
        return state.isOf(Blocks.MELON)
                || state.isOf(Blocks.PUMPKIN)
                || isMushroomBlock(state);
    }

    private static int tryHarvestAroundWithHoe(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos center,
            BlockState brokenState
    ) {
        if (harvestingArea) {
            return 0;
        }

        double radiusValue = SkillTreeManager.getBonusTotal(
                player,
                SkillType.FARMING,
                BonusType.FARMER_REACH_RADIUS
        );

        int radius = (int) radiusValue;

        if (radius <= 0) {
            return 0;
        }

        ItemStack tool = player.getMainHandStack();

        if (!tool.isIn(net.minecraft.registry.tag.ItemTags.HOES)) {
            return 0;
        }

        if (!isFarmingBlock(brokenState) || !isMatureFarmingBlock(brokenState)) {
            return 0;
        }

        harvestingArea = true;

        try {
            return harvestArea(player, world, center, radius);
        } finally {
            harvestingArea = false;
        }
    }

    private static int harvestArea(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos center,
            int radius
    ) {
        int maxHarvest = 96;
        int[] totalXp = {0};

        WorldScanUtils.forEachBlockInCylinder(
                world,
                center,
                radius,
                -1,
                1,
                true,
                maxHarvest,
                (targetPos, targetState) -> {
                    if (!isFarmingBlock(targetState)) {
                        return false;
                    }

                    if (!isMatureFarmingBlock(targetState)) {
                        return false;
                    }

                    totalXp[0] += harvestSingleFarmingBlock(player, world, targetPos, targetState);
                    return true;
                }
        );

        return totalXp[0];
    }

    private static int harvestSingleFarmingBlock(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state
    ) {
        BlockState replantedState = null;

        if (SkillTreeManager.hasBonus(player, SkillType.FARMING, BonusType.AUTO_REPLANT)) {
            replantedState = getReplantedState(state);
        }

        for (ItemStack drop : Block.getDroppedStacks(state, world, pos, null, player, player.getMainHandStack())) {
            if (!drop.isEmpty()) {
                dropOrStoreFarmingStack(player, world, pos, drop);
            }
        }

        int xp = getFarmingXp(state);
        tryDropEnchantedSeed(player, world, pos);
        tryDoubleDrops(player, world, pos, state);
        tryVanillaXp(player, world, pos);
        tryApplyCultivatedShield(player, state);

        if (replantedState != null) {
            world.setBlockState(pos, replantedState, Block.NOTIFY_ALL);
            protectReplantedCrop(world, pos);
        } else {
            world.breakBlock(pos, false, player);
        }
        return xp;
    }

    private static boolean canTriggerCompostTreasure(BlockState state, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        int level = state.get(ComposterBlock.LEVEL);

        if (level >= 7) {
            return false;
        }

        return ComposterBlock.ITEM_TO_LEVEL_INCREASE_CHANCE.containsKey(stack.getItem());
    }

    private static void tryDropCompostTreasure(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        double chance = SkillTreeManager.getBonusTotal(
                player,
                SkillType.FARMING,
                BonusType.COMPOST_RARE_DROP_CHANCE
        );

        if (chance <= 0.0) {
            return;
        }

        if (world.random.nextDouble() >= chance) {
            return;
        }

        giveCompostRareReward(player, world, pos);
    }

    private static void giveCompostRareReward(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        ItemStack reward = ItemStack.EMPTY;
        int xpReward = 0;

        int roll = world.random.nextInt(5);

        switch (roll) {
            case 0 -> reward = new ItemStack(Items.GOLDEN_CARROT);
            case 1 -> reward = new ItemStack(Items.BONE_BLOCK);
            case 2 -> reward = getRandomPlantableReward(world);
            case 3 -> reward = new ItemStack(Items.VINE);
            case 4 -> xpReward = 3 + world.random.nextInt(5);
            default -> {
            }
        }

        if (!reward.isEmpty()) {
            Block.dropStack(world, pos.up(), reward.copy());
            playCompostTreasureFeedback(player, world, pos, reward.getName());
            return;
        }

        if (xpReward > 0) {
            ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos.up()), xpReward);
            playCompostTreasureFeedback(player, world, pos, Text.translatable("message.mythicrpg.compost_treasure.xp", xpReward));
        }
    }

    private static void playCompostTreasureFeedback(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            Text rewardName
    ) {
        world.spawnParticles(
                ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5,
                pos.getY() + 1.15,
                pos.getZ() + 0.5,
                10,
                0.35,
                0.25,
                0.35,
                0.02
        );

        world.spawnParticles(
                ParticleTypes.COMPOSTER,
                pos.getX() + 0.5,
                pos.getY() + 1.05,
                pos.getZ() + 0.5,
                12,
                0.30,
                0.20,
                0.30,
                0.03
        );

        world.playSound(
                null,
                pos,
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.BLOCKS,
                0.65f,
                1.45f
        );

        player.sendMessage(
                Text.translatable("message.mythicrpg.compost_treasure", rewardName.copy().formatted(Formatting.GOLD))
                        .formatted(Formatting.GREEN),
                true
        );
    }

    private static ItemStack getRandomPlantableReward(ServerWorld world) {
        Item[] rewards = {
                Items.WHEAT_SEEDS,
                Items.PUMPKIN_SEEDS,
                Items.MELON_SEEDS,
                Items.BEETROOT_SEEDS,
                Items.CARROT,
                Items.POTATO,
                Items.NETHER_WART,
                Items.COCOA_BEANS,
                Items.SWEET_BERRIES,
                Items.GLOW_BERRIES,
                Items.BAMBOO,
                Items.CACTUS,
                Items.SUGAR_CANE,
                Items.BROWN_MUSHROOM,
                Items.RED_MUSHROOM
        };

        Item item = rewards[world.random.nextInt(rewards.length)];
        return new ItemStack(item);
    }

    private static void dropOrStoreFarmingStack(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = stack.copy();

        FoodBackpackItem.tryStore(player, remaining);

        if (!remaining.isEmpty()) {
            Block.dropStack(world, pos, remaining);
        }
    }
}