package com.mythicrpg.woodcutting;

import com.mythicrpg.core.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public final class WoodcuttingEvents {

    private static final int LOG_XP = 2;
    private static final int TIMBER_MAX_BLOCKS = 32;

    private WoodcuttingEvents() {
    }

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register(WoodcuttingEvents::onBlockBreak);
    }

    private static void onBlockBreak(
            World world,
            net.minecraft.entity.player.PlayerEntity player,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        boolean isLog = state.isIn(BlockTags.LOGS);
        boolean isLeaves = state.isIn(BlockTags.LEAVES);

        if (!isLog && !isLeaves) {
            return;
        }

        ItemStack tool = serverPlayer.getMainHandStack();

        if (isLog) {
            SkillXpManager.addXp(serverPlayer, SkillType.WOODCUTTING, LOG_XP);

            applyWoodDropBonus(serverPlayer, serverWorld, pos, state, blockEntity);
            applyEnchantedWoodDrop(serverPlayer, serverWorld, pos);
            applyWoodVanillaXp(serverPlayer, serverWorld, pos);
            undoAxeDurabilityLossIfBonus(serverPlayer, tool);

            tryTimber(serverPlayer, serverWorld, pos, state);
            return;
        }

        if (isLeaves) {
            if (!hasSilkTouch(serverWorld, tool)) {
                applyLeafDrops(serverPlayer, serverWorld, pos);
            }

            undoAxeDurabilityLossIfBonus(serverPlayer, tool);
        }
    }

    private static void applyWoodDropBonus(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos,
            BlockState state,
            BlockEntity blockEntity
    ) {
        double chance = SkillTreeManager.getBonusTotal(
                player,
                SkillType.WOODCUTTING,
                BonusType.WOOD_DOUBLE_DROP_CHANCE
        );

        if (chance <= 0) {
            return;
        }

        if (world.random.nextDouble() >= chance) {
            return;
        }

        List<ItemStack> drops = Block.getDroppedStacks(
                state,
                world,
                pos,
                blockEntity,
                player,
                player.getMainHandStack()
        );

        for (ItemStack drop : drops) {
            Block.dropStack(world, pos, drop.copy());
        }

        PassiveProcSoundManager.playForPlayer(
                player,
                "woodcutting_double_drop",
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.35f,
                1.4f,
                5
        );
    }

    private static void applyWoodVanillaXp(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        if (!SkillTreeManager.hasBonus(player, SkillType.WOODCUTTING, BonusType.WOOD_VANILLA_XP)) {
            return;
        }

        ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), 1);
    }

    private static void applyLeafDrops(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        applyRandomSaplingDrop(player, world, pos);
        applyAppleDrop(player, world, pos);
        applyGoldenAppleDrop(player, world, pos);
    }

    private static void applyRandomSaplingDrop(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        double chance = SkillTreeManager.getBonusTotal(
                player,
                SkillType.WOODCUTTING,
                BonusType.RANDOM_SAPLING_DROP_CHANCE
        );

        if (chance <= 0) {
            return;
        }

        if (world.random.nextDouble() >= chance) {
            return;
        }

        Block.dropStack(world, pos, getRandomSapling(world));
    }

    private static void applyAppleDrop(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        if (!SkillTreeManager.hasBonus(player, SkillType.WOODCUTTING, BonusType.LEAF_APPLE_DROP)) {
            return;
        }

        Block.dropStack(world, pos, new ItemStack(Items.APPLE));
    }

    private static void applyGoldenAppleDrop(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        double chance = SkillTreeManager.getBonusTotal(
                player,
                SkillType.WOODCUTTING,
                BonusType.LEAF_GOLDEN_APPLE_CHANCE
        );

        if (chance <= 0) {
            return;
        }

        if (world.random.nextDouble() >= chance) {
            return;
        }

        Block.dropStack(world, pos, new ItemStack(Items.GOLDEN_APPLE));

        PassiveProcSoundManager.playForPlayer(
                player,
                "woodcutting_golden_apple",
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.6f,
                0.8f,
                10
        );
    }

    private static void tryTimber(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos origin,
            BlockState originState
    ) {
        if (!SkillTreeManager.hasBonus(player, SkillType.WOODCUTTING, BonusType.TIMBER)) {
            return;
        }

        if (!isHoldingAxe(player)) {
            return;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> toBreak = new ArrayList<>();

        queue.add(origin);

        while (!queue.isEmpty() && toBreak.size() < TIMBER_MAX_BLOCKS) {
            BlockPos current = queue.poll();

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.offset(direction);

                if (toBreak.contains(neighbor)) {
                    continue;
                }

                BlockState neighborState = world.getBlockState(neighbor);

                if (neighborState.getBlock() != originState.getBlock()) {
                    continue;
                }

                toBreak.add(neighbor);
                queue.add(neighbor);
            }
        }

        if (toBreak.isEmpty()) {
            return;
        }

        int xpGained = 0;

        for (BlockPos blockPos : toBreak) {
            BlockState blockState = world.getBlockState(blockPos);

            if (!blockState.isIn(BlockTags.LOGS)) {
                continue;
            }

            Block.dropStacks(
                    blockState,
                    world,
                    blockPos,
                    world.getBlockEntity(blockPos),
                    player,
                    player.getMainHandStack()
            );

            world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

            applyWoodDropBonus(player, world, blockPos, blockState, null);
            applyEnchantedWoodDrop(player, world, blockPos);
            applyWoodVanillaXp(player, world, blockPos);

            xpGained += LOG_XP;
        }

        if (xpGained > 0) {
            SkillXpManager.addXp(player, SkillType.WOODCUTTING, xpGained);

            PassiveProcSoundManager.playForPlayer(
                    player,
                    "woodcutting_timber",
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    0.55f,
                    0.9f,
                    10
            );
        }
    }

    private static boolean isHoldingAxe(ServerPlayerEntity player) {
        ItemStack stack = player.getMainHandStack();
        return isAxe(stack);
    }

    private static boolean isAxe(ItemStack stack) {
        return stack.isIn(ItemTags.AXES) || stack.isOf(ModItems.ENCHANTED_AXE);
    }

    private static ItemStack getRandomSapling(ServerWorld world) {
        List<ItemStack> saplings = List.of(
                new ItemStack(Items.OAK_SAPLING),
                new ItemStack(Items.SPRUCE_SAPLING),
                new ItemStack(Items.BIRCH_SAPLING),
                new ItemStack(Items.JUNGLE_SAPLING),
                new ItemStack(Items.ACACIA_SAPLING),
                new ItemStack(Items.DARK_OAK_SAPLING),
                new ItemStack(Items.CHERRY_SAPLING),
                new ItemStack(Items.MANGROVE_PROPAGULE)
        );

        return saplings.get(world.random.nextInt(saplings.size())).copy();
    }

    private static void undoAxeDurabilityLossIfBonus(ServerPlayerEntity player, ItemStack tool) {
        if (!SkillTreeManager.hasBonus(player, SkillType.WOODCUTTING, BonusType.AXE_NO_DURABILITY)) {
            return;
        }

        if (!isAxe(tool)) {
            return;
        }

        if (tool.isDamageable() && tool.getDamage() > 0) {
            tool.setDamage(tool.getDamage() - 1);
        }
    }

    private static boolean hasSilkTouch(ServerWorld world, ItemStack stack) {
        return world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.SILK_TOUCH)
                .map(entry -> EnchantmentHelper.getLevel(entry, stack) > 0)
                .orElse(false);
    }

    private static void applyEnchantedWoodDrop(
            ServerPlayerEntity player,
            ServerWorld world,
            BlockPos pos
    ) {
        double chance = SkillTreeManager.getBonusTotal(
                player,
                SkillType.WOODCUTTING,
                BonusType.ENCHANTED_WOOD_CHANCE
        );

        if (chance <= 0) {
            return;
        }

        if (world.random.nextDouble() >= chance) {
            return;
        }

        Block.dropStack(
                world,
                pos,
                new ItemStack(ModBlocks.ENCHANTED_WOOD)
        );

        PassiveProcSoundManager.playForPlayer(
                player,
                "woodcutting_enchanted_wood",
                SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                0.7f,
                1.2f,
                10
        );
    }
}