package com.mythicrpg.farming;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FarmingDeathManager {
    private static final Map<UUID, PreservedFarmerData> PRESERVED_DATA = new HashMap<>();

    private FarmingDeathManager() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayerEntity player)) {
                return true;
            }

            preserveBeforeDeath(player);
            return true;
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            restoreAfterRespawn(newPlayer);
        });
    }

    private static void preserveBeforeDeath(ServerPlayerEntity player) {
        FoodBackpackDeathData.recordDeath(player);

        if (!SkillTreeManager.hasBonus(player, SkillType.FARMING, BonusType.PRESERVED_FARMER)) {
            return;
        }

        List<ItemStack> preservedBackpacks = extractFoodBackpacks(player);

        PreservedFarmerData data = new PreservedFarmerData(
                player.experienceLevel,
                player.totalExperience,
                player.experienceProgress,
                preservedBackpacks
        );

        PRESERVED_DATA.put(player.getUuid(), data);
    }

    private static List<ItemStack> extractFoodBackpacks(ServerPlayerEntity player) {
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);

            if (!stack.isOf(ModItems.FOOD_BACKPACK)) {
                continue;
            }

            result.add(stack.copy());
            player.getInventory().setStack(i, ItemStack.EMPTY);
        }

        return result;
    }

    private static void restoreAfterRespawn(ServerPlayerEntity newPlayer) {
        PreservedFarmerData data = PRESERVED_DATA.remove(newPlayer.getUuid());

        if (data == null) {
            return;
        }

        newPlayer.experienceLevel = data.experienceLevel();
        newPlayer.totalExperience = data.totalExperience();
        newPlayer.experienceProgress = data.experienceProgress();

        for (ItemStack backpack : data.foodBackpacks()) {
            ItemStack toInsert = backpack.copy();

            if (!newPlayer.getInventory().insertStack(toInsert)) {
                newPlayer.dropItem(toInsert, false);
            }
        }

        newPlayer.currentScreenHandler.sendContentUpdates();
    }

    private record PreservedFarmerData(
            int experienceLevel,
            int totalExperience,
            float experienceProgress,
            List<ItemStack> foodBackpacks
    ) {
    }
}