package com.mythicrpg.traveling;

import com.mythicrpg.core.MythicTooltipItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Custom item unlocked by Traveling node 20. */
public final class GrapplingHookItem extends MythicTooltipItem {

    public GrapplingHookItem(Settings settings) {
        super(settings, List.of(
                MythicTooltipItem.line("tooltip.mythicrpg.grappling_hook.description", Formatting.GRAY),
                MythicTooltipItem.line("tooltip.mythicrpg.grappling_hook.range", Formatting.AQUA),
                MythicTooltipItem.line("tooltip.mythicrpg.grappling_hook.speed", Formatting.YELLOW),
                MythicTooltipItem.line("tooltip.mythicrpg.grappling_hook.use", Formatting.GREEN)
        ));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.fail(stack);
        }

        return GrapplingHookManager.tryFire(serverPlayer)
                ? TypedActionResult.success(stack)
                : TypedActionResult.fail(stack);
    }
}
