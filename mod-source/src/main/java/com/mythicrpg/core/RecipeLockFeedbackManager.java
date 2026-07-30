package com.mythicrpg.core;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;


public final class RecipeLockFeedbackManager {
    private static final int COOLDOWN_TICKS = 20;

    private RecipeLockFeedbackManager() {
    }

    public static void sendLockedCraftFeedback(ServerPlayerEntity player, ItemStack result) {
        if (!PlayerCooldownManager.tryUse(
                player,
                "locked_craft_feedback",
                COOLDOWN_TICKS
        )) {
            return;
        }

        Text message = LockedRecipeRegistry.getLockedMessage(player, result);

        player.sendMessage(message, true);

        player.getWorld().playSound(
                null,
                player.getBlockPos(),
                SoundEvents.ENTITY_VILLAGER_NO,
                SoundCategory.PLAYERS,
                0.45f,
                1.2f
        );
    }
}