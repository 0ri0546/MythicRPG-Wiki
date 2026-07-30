package com.mythicrpg.client.eating;

import com.mythicrpg.core.ModItems;
import com.mythicrpg.eating.EatingBalance;
import com.mythicrpg.eating.PreparedDishData;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.block.CakeBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

/** Adds the effective saturation gain to every regular edible item tooltip. */
public final class FoodSaturationTooltip {
    private FoodSaturationTooltip() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (stack.isEmpty()) {
                return;
            }
            if (stack.isOf(ModItems.PREPARED_DISH) || stack.isOf(ModItems.SIGNATURE_DISH)) {
                PreparedDishData.read(stack).filter(dish -> !dish.dubious()).ifPresent(dish -> {
                    if (!PreparedDishData.isPreserved(dish) && MinecraftClient.getInstance().world != null) {
                        long remaining = PreparedDishData.remainingTicks(
                                dish,
                                MinecraftClient.getInstance().world.getTime()
                        );
                        lines.add(Text.translatable(
                                "tooltip.mythicrpg.prepared_dish.shelf_life_days",
                                Math.max(0L, (remaining + 23_999L) / 24_000L)
                        ).formatted(Formatting.DARK_AQUA));
                    }
                });
                return;
            }

            Float saturation = saturationPoints(stack);
            if (saturation == null) {
                return;
            }

            boolean vanillaFood = "minecraft".equals(
                    Registries.ITEM.getId(stack.getItem()).getNamespace()
            );
            boolean cakeSlice = stack.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof CakeBlock;
            if (vanillaFood || cakeSlice) {
                saturation *= EatingBalance.VANILLA_SATURATION_MULTIPLIER;
            }

            lines.add(Text.translatable(
                    "tooltip.mythicrpg.food.saturation",
                    String.format(Locale.ROOT, "%.1f", saturation)
            ).formatted(Formatting.GOLD));
        });
    }

    private static Float saturationPoints(ItemStack stack) {
        FoodComponent food = stack.get(DataComponentTypes.FOOD);
        if (food != null) {
            return Math.max(0.0F, food.saturation());
        }

        // Vanilla-style cakes are consumed as placed blocks and therefore have no FOOD component.
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof CakeBlock) {
            return 0.4F;
        }
        return null;
    }
}
