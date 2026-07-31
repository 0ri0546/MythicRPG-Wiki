package com.mythicrpg.mining;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModBlockTags;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import com.mythicrpg.core.PassiveProcSoundManager;
import net.minecraft.sound.SoundEvents;

public class MiningAreaEffects {
    private static final int VEIN_MINING_MAX_BLOCKS = 10;

    public static int tryVeinMine(ServerPlayerEntity player, ServerWorld world, BlockPos origin, BlockState originState) {
        if (!SkillTreeManager.hasBonus(player, SkillType.MINING, BonusType.VEIN_MINING)) {
            return 0;
        }
        if (!VeinMiningToggleState.isEnabled(player.getUuid())) {
            return 0;
        }
        if (!originState.isIn(ModBlockTags.ORES)) {
            return 0;
        }

        Block targetBlock = originState.getBlock();
        Set<BlockPos> visited = new HashSet<>();
        visited.add(origin);
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);

        int brokenCount = 0;
        int totalXp = 0;

        while (!queue.isEmpty() && brokenCount < VEIN_MINING_MAX_BLOCKS) {
            BlockPos current = queue.poll();

            for (Direction direction : Direction.values()) {
                BlockPos neighborPos = current.offset(direction);
                if (visited.contains(neighborPos)) {
                    continue;
                }
                visited.add(neighborPos);

                BlockState neighborState = world.getBlockState(neighborPos);
                if (neighborState.getBlock() == targetBlock) {
                    queue.add(neighborPos);
                    totalXp += breakBonusBlock(player, world, neighborPos, neighborState);
                    brokenCount++;
                    if (brokenCount >= VEIN_MINING_MAX_BLOCKS) {
                        break;
                    }
                }
            }
        }
        if (brokenCount > 0) {
            PassiveProcSoundManager.playForPlayer(
                    player,
                    "mining_vein_mining",
                    SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                    0.35f,
                    1.4f,
                    10
            );
        }
        return totalXp;
    }

    public static int tryAreaMine3x3(ServerPlayerEntity player, ServerWorld world, BlockPos origin, BlockState originState) {
        if (!SkillTreeManager.hasBonus(player, SkillType.MINING, BonusType.MINING_3X3)) {
            return 0;
        }
        if (!MiningToggleState.isAreaMiningEnabled(player)) {
            return 0;
        }

        Direction.Axis axis = player.getHorizontalFacing().getAxis();
        int totalXp = 0;

        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }

                BlockPos targetPos = (axis == Direction.Axis.Z)
                        ? origin.add(a, b, 0)
                        : origin.add(0, b, a);

                BlockState targetState = world.getBlockState(targetPos);
                if (targetState.getBlock() == originState.getBlock()) {
                    totalXp += breakBonusBlock(player, world, targetPos, targetState);
                }
            }
        }
        return totalXp;
    }

    private static int breakBonusBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        world.breakBlock(pos, true, player);

        MiningEvents.applyDropBonus(player, world, pos, state, blockEntity);
        MiningEvents.applyVanillaXpBonus(player, world, pos, state);
        consumeDurability(player, player.getMainHandStack());
        return MiningEvents.skillXpForBlock(state);
    }

    private static void consumeDurability(ServerPlayerEntity player, ItemStack tool) {
        if (SkillTreeManager.hasBonus(player, SkillType.MINING, BonusType.NO_DURABILITY_LOSS)) {
            return;
        }
        if (tool.isDamageable()) {
            tool.damage(1, (ServerWorld) player.getWorld(), player, item -> {});
        }
    }
}