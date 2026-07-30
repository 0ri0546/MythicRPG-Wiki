package com.mythicrpg.eating;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public final class ChefNotebookItem extends Item {
    public ChefNotebookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity player) || !EatingPerks.hasSignatureDish(player)) {
            if (user instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable("message.mythicrpg.eating.signature_locked")
                        .formatted(Formatting.RED), true);
            }
            return TypedActionResult.fail(stack);
        }
        int handId = hand == Hand.MAIN_HAND ? 0 : 1;
        SignatureDishOpenPayload payload = SignatureDishManager.getConfiguration(player)
                .map(configuration -> new SignatureDishOpenPayload(
                        handId,
                        configuration.name(),
                        configuration.bonus().ordinal(),
                        configuration.icon().toString(),
                        configuration.ingredientIds().stream().map(Object::toString).toList()
                ))
                .orElseGet(() -> SignatureDishOpenPayload.empty(handId));
        ServerPlayNetworking.send(player, payload);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.chef_notebook.description")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.chef_notebook.configure")
                .formatted(Formatting.GOLD));
    }
}
