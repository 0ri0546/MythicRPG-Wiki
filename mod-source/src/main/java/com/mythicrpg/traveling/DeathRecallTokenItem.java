package com.mythicrpg.traveling;

import net.minecraft.entity.Entity;
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
import java.util.Optional;

public final class DeathRecallTokenItem extends Item {

    public DeathRecallTokenItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }

        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        return TravelingDeathRecallManager.tryUse(player, stack)
                ? TypedActionResult.success(stack)
                : TypedActionResult.fail(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient() || !(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Full token validation parses custom data and checks persistent state.
        // Once per second is sufficient because use() always validates immediately.
        if (Math.floorMod(player.age + slot, 20) == 0) {
            TravelingDeathRecallManager.discardInvalidInventoryToken(player, stack);
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.death_recall_token.description")
                .formatted(Formatting.GRAY));

        Optional<DeathRecallTokenData.Data> data = DeathRecallTokenData.read(stack);
        if (data.isEmpty()) {
            tooltip.add(Text.translatable("tooltip.mythicrpg.death_recall_token.invalid")
                    .formatted(Formatting.RED));
            return;
        }

        DeathRecallTokenData.Data recall = data.get();
        tooltip.add(Text.translatable(
                "tooltip.mythicrpg.death_recall_token.destination",
                recall.deathPos().getX(),
                recall.deathPos().getY(),
                recall.deathPos().getZ()
        ).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable(
                "tooltip.mythicrpg.death_recall_token.remaining",
                recall.remainingSeconds()
        ).formatted(recall.remainingSeconds() > 0 ? Formatting.YELLOW : Formatting.RED));
    }
}
