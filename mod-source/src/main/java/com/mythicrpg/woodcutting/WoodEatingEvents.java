package com.mythicrpg.woodcutting;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class WoodEatingEvents {

    private WoodEatingEvents() {
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);

            if (!stack.isIn(ItemTags.LOGS)) {
                return TypedActionResult.pass(stack);
            }

            if (!(player instanceof ServerPlayerEntity serverPlayer)) {
                return TypedActionResult.pass(stack);
            }

            if (!SkillTreeManager.hasBonus(serverPlayer, SkillType.WOODCUTTING, BonusType.WOOD_EATER)) {
                return TypedActionResult.pass(stack);
            }

            if (!player.canConsume(false)) {
                return TypedActionResult.pass(stack);
            }

            eatWood(serverPlayer, world, stack);

            return TypedActionResult.success(stack);
        });
    }

    private static void eatWood(ServerPlayerEntity player, World world, ItemStack stack) {
        stack.decrement(1);

        // Golden carrot-like values:
        // 6 hunger, 1.2 saturation modifier.
        player.getHungerManager().add(6, 1.2f);

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_GENERIC_EAT,
                SoundCategory.PLAYERS,
                0.8f,
                0.9f
        );

        world.playSound(
                null,
                player.getBlockPos(),
                SoundEvents.BLOCK_WOOD_BREAK,
                SoundCategory.PLAYERS,
                0.35f,
                1.3f
        );
    }
}