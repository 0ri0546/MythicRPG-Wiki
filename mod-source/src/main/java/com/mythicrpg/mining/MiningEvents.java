package com.mythicrpg.mining;

import com.mythicrpg.core.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class MiningEvents {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer) || !(world instanceof ServerWorld serverWorld)) {
                return;
            }

            ItemStack tool = serverPlayer.getMainHandStack();
            if (hasSilkTouch(serverWorld, tool)) {
                return;
            }

            if (!isMiningBlock(state)) {
                return;
            }

            applyDropBonus(serverPlayer, serverWorld, pos, state, blockEntity);
            applyVanillaXpBonus(serverPlayer, serverWorld, pos, state);
            undoDurabilityLossIfBonus(serverPlayer, tool);

            int totalSkillXp = skillXpForBlock(state)
                    + MiningAreaEffects.tryVeinMine(serverPlayer, serverWorld, pos, state)
                    + MiningAreaEffects.tryAreaMine3x3(serverPlayer, serverWorld, pos, state);
            SkillXpManager.addXp(serverPlayer, SkillType.MINING, totalSkillXp, true);
        });
    }

    private static boolean hasSilkTouch(ServerWorld world, ItemStack stack) {
        return world.getRegistryManager()
                .get(RegistryKeys.ENCHANTMENT)
                .getEntry(Enchantments.SILK_TOUCH)
                .map(entry -> EnchantmentHelper.getLevel(entry, stack) > 0)
                .orElse(false);
    }

    // XP du skill MythicRPG (progression interne, sert a debloquer les nodes de l'arbre)
    static int skillXpForBlock(BlockState state) {
        if (state.isIn(BlockTags.DIAMOND_ORES)) return 10;
        if (state.isIn(BlockTags.EMERALD_ORES)) return 15;
        if (state.getBlock() == Blocks.ANCIENT_DEBRIS) return 20;
        if (state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.LAPIS_ORES)) return 7;
        if (state.isIn(BlockTags.IRON_ORES)) return 5;
        if (state.isIn(BlockTags.COAL_ORES)) return 3;
        return 1;
    }

    static void applyDropBonus(ServerPlayerEntity player, ServerWorld world, BlockPos pos,
                               BlockState state, BlockEntity blockEntity) {
        if (!state.isIn(ModBlockTags.ORES)) {
            return;
        }

        double dropBonus = SkillTreeManager.getBonusTotal(player, SkillType.MINING, BonusType.DROP_MULTIPLIER);
        if (dropBonus <= 0) {
            return;
        }

        List<ItemStack> drops = Block.getDroppedStacks(state, world, pos, blockEntity, player, player.getMainHandStack());
        int procCount = 0;

        for (ItemStack stack : drops) {
            if (world.random.nextDouble() < dropBonus) {
                Block.dropStack(world, pos, stack.copy());
                procCount++;
            }
        }

        if (procCount > 0) {
            player.sendMessage(
                    net.minecraft.text.Text.translatable(
                            procCount == 1
                                    ? "message.mythicrpg.mining_bonus.proc"
                                    : "message.mythicrpg.mining_bonus.proc_multiple",
                            procCount
                    ).formatted(net.minecraft.util.Formatting.GOLD),
                    true
            );
        }
    }

    // Moyenne approximative de l'XP vanilla donnee par le bloc au minage (sans silk touch).
    // Sert de base pour calculer le bonus, pas une reproduction exacte du roll vanilla.
    private static int averageVanillaXp(BlockState state) {
        if (state.isIn(BlockTags.DIAMOND_ORES)) return 5;
        if (state.isIn(BlockTags.EMERALD_ORES)) return 5;
        if (state.isIn(BlockTags.REDSTONE_ORES)) return 3;
        if (state.isIn(BlockTags.LAPIS_ORES)) return 4;
        if (state.getBlock() == Blocks.NETHER_QUARTZ_ORE) return 3;
        if (state.isIn(BlockTags.COAL_ORES)) return 1;
        return 0;
    }

    static void applyVanillaXpBonus(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state) {
        int baseXp = averageVanillaXp(state);
        if (baseXp <= 0) {
            return;
        }

        double xpMultiplier = SkillTreeManager.getBonusTotal(player, SkillType.MINING, BonusType.XP_MULTIPLIER);
        if (xpMultiplier <= 0) {
            return;
        }

        int bonusXp = (int) Math.round(baseXp * xpMultiplier);
        if (bonusXp > 0) {
            ExperienceOrbEntity.spawn(world, Vec3d.ofCenter(pos), bonusXp);
        }
    }

    private static void undoDurabilityLossIfBonus(ServerPlayerEntity player, ItemStack tool) {
        if (!SkillTreeManager.hasBonus(player, SkillType.MINING, BonusType.NO_DURABILITY_LOSS)) {
            return;
        }
        if (tool.isDamageable() && tool.getDamage() > 0) {
            tool.setDamage(tool.getDamage() - 1);
        }
    }

    private static boolean isMiningBlock(BlockState state) {
        return state.isIn(BlockTags.PICKAXE_MINEABLE);
    }
}