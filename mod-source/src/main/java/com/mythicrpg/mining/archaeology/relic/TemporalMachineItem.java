package com.mythicrpg.mining.archaeology.relic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class TemporalMachineItem extends LeveledRelicItem {
    public TemporalMachineItem(Settings settings) {
        super(settings, "tooltip.mythicrpg.temporal_machine.description");
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) return TypedActionResult.success(stack);
        if (!(user instanceof ServerPlayerEntity player)) return TypedActionResult.pass(stack);
        return TemporalReturnManager.activate(player, stack)
                ? TypedActionResult.success(stack)
                : TypedActionResult.fail(stack);
    }
}
