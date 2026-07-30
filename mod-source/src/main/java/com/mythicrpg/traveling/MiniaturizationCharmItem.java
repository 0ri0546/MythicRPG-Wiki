package com.mythicrpg.traveling;

import com.mythicrpg.core.MythicTooltipItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public final class MiniaturizationCharmItem extends MythicTooltipItem {

    public MiniaturizationCharmItem(Settings settings) {
        super(settings, List.of(
                MythicTooltipItem.line("tooltip.mythicrpg.miniaturization_charm.description", Formatting.GRAY),
                MythicTooltipItem.line("tooltip.mythicrpg.miniaturization_charm.use", Formatting.GREEN)
        ));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!user.isSneaking()) {
            return TypedActionResult.pass(stack);
        }

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        return TravelingMiniaturizationManager.toggle(user)
                ? TypedActionResult.success(stack)
                : TypedActionResult.fail(stack);
    }
}
