package com.mythicrpg.fishing;

import net.minecraft.entity.LivingEntity;
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

/** Durable charms crafted from the guaranteed material of each sea monster. */
public final class FishingCharmItem extends Item {
    public enum Kind {
        MEGALODON,
        NESSIE,
        WHALE
    }

    private static final int WHALE_COOLDOWN_TICKS = 20 * 5;
    private final Kind kind;

    public FishingCharmItem(Kind kind, Settings settings) {
        super(settings);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (kind != Kind.WHALE) return TypedActionResult.pass(stack);
        if (!player.isTouchingWater()) {
            if (!world.isClient) {
                player.sendMessage(Text.translatable("message.mythicrpg.whale_charm.water_required").formatted(Formatting.RED), true);
            }
            return TypedActionResult.fail(stack);
        }
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(stack);
        }
        if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
            double horizontal = 0.35D;
            serverPlayer.addVelocity(
                    serverPlayer.getRotationVector().x * horizontal,
                    1.15D,
                    serverPlayer.getRotationVector().z * horizontal
            );
            serverPlayer.velocityModified = true;
            SeaMonsterManager.protectWhaleLaunch(serverPlayer);
            stack.damage(1, serverPlayer, LivingEntity.getSlotForHand(hand));
            serverPlayer.getItemCooldownManager().set(this, WHALE_COOLDOWN_TICKS);
        }
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("tooltip.mythicrpg.fishing_charm." + kind.name().toLowerCase(java.util.Locale.ROOT))
                .formatted(Formatting.AQUA));
        if (kind != Kind.WHALE) {
            tooltip.add(Text.translatable("tooltip.mythicrpg.fishing_charm.offhand").formatted(Formatting.DARK_GRAY));
        }
    }
}
