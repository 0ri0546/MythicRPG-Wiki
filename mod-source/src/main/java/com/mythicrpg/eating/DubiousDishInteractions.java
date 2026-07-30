package com.mythicrpg.eating;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ComposterBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class DubiousDishInteractions {
    private DubiousDishInteractions() {
    }

    public static boolean canHeal(PlayerEntity player, LivingEntity entity) {
        if (entity.getHealth() >= entity.getMaxHealth()) {
            return false;
        }
        if (entity instanceof WolfEntity wolf) {
            return wolf.isTamed() && wolf.isOwner(player);
        }
        return entity instanceof MobEntity mob && LandMountManager.isOwner(mob, player);
    }

    public static void healCompanion(World world, LivingEntity entity) {
        entity.heal(2.0F);
        world.playSound(
                null,
                entity.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_EAT,
                SoundCategory.NEUTRAL,
                0.8F,
                1.15F
        );
    }

    public static boolean compost(
            ServerPlayerEntity player,
            World world,
            BlockPos pos,
            BlockState state
    ) {
        if (!state.isOf(Blocks.COMPOSTER) || !EatingPerks.canCompostDubiousDish(player)) {
            return false;
        }

        int level = state.get(ComposterBlock.LEVEL);
        if (level >= 7) {
            return false;
        }

        int newLevel = level + 1;
        world.setBlockState(pos, state.with(ComposterBlock.LEVEL, newLevel), Block.NOTIFY_ALL);
        world.syncWorldEvent(null, 1500, pos, 1);
        if (newLevel == 7) {
            world.scheduleBlockTick(pos, Blocks.COMPOSTER, 20);
        }
        return true;
    }
}
