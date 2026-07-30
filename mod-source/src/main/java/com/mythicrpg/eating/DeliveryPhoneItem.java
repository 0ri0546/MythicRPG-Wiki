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

public final class DeliveryPhoneItem extends Item {
    public DeliveryPhoneItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity player) || !EatingPerks.hasDelivery(player)) {
            if (user instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable("message.mythicrpg.eating.delivery_locked")
                        .formatted(Formatting.RED), true);
            }
            return TypedActionResult.fail(stack);
        }
        DeliveryPhoneData.Settings settings = DeliveryPhoneData.read(stack);
        ServerPlayNetworking.send(player, new DeliveryPhoneOpenPayload(
                hand == Hand.MAIN_HAND ? 0 : 1,
                settings.source().ordinal(),
                settings.count()
        ));
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        DeliveryPhoneData.Settings settings = DeliveryPhoneData.read(stack);
        tooltip.add(Text.translatable("tooltip.mythicrpg.delivery_phone.description")
                .formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(
                "tooltip.mythicrpg.delivery_phone.settings",
                Text.translatable("delivery_source.mythicrpg." + settings.source().id()),
                settings.count()
        ).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.delivery_phone.custom_only")
                .formatted(Formatting.DARK_GREEN));
    }
}
