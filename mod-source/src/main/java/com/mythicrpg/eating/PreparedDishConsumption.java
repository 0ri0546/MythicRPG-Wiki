package com.mythicrpg.eating;

import com.mythicrpg.fishing.FishingDishEffectData;
import com.mythicrpg.fishing.FishingFamily;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class PreparedDishConsumption {
    private PreparedDishConsumption() {
    }

    public static boolean canConsume(ServerPlayerEntity player) {
        return player.getHungerManager().isNotFull() || EatingPerks.canEatWhenFull(player);
    }

    public static void consumeFromPlate(
            ServerPlayerEntity player,
            PreparedDishData.Dish dish,
            ItemStack consumedStack
    ) {
        if (dish.dubious()) {
            player.getHungerManager().add(3, 0.2F);
        }
        consume(player, dish, consumedStack);
    }

    public static void consumeFromPlate(ServerPlayerEntity player, PreparedDishData.Dish dish) {
        consumeFromPlate(player, dish, ItemStack.EMPTY);
    }

    public static void consume(
            ServerPlayerEntity player,
            PreparedDishData.Dish dish,
            ItemStack consumedStack
    ) {
        if (dish.dubious()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.eating.dubious_eaten")
                            .formatted(Formatting.DARK_GREEN),
                    true
            );
            return;
        }

        player.getHungerManager().setFoodLevel(20);
        player.getHungerManager().setSaturationLevel(dish.rarity().saturation());
        EatingXpManager.awardDish(player, dish);
        FishingFamily fishingEffect = FishingDishEffectData.read(consumedStack);
        if (fishingEffect == FishingFamily.INFERNAL) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 60 * (dish.rarity().rank() + 1), 0, false, true, true));
        } else if (fishingEffect == FishingFamily.VOID) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 20 * 60 * (dish.rarity().rank() + 1), 0, false, true, true));
        }
        EatingAdvancedManager.onDishConsumed(player, dish, consumedStack);
    }

    public static void consume(ServerPlayerEntity player, PreparedDishData.Dish dish) {
        consume(player, dish, ItemStack.EMPTY);
    }
}
