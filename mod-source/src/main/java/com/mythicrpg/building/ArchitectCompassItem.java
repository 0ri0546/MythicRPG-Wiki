package com.mythicrpg.building;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Client-rendered circle guide unlocked by Building perk 14. */
public final class ArchitectCompassItem extends Item {
    public ArchitectCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        return ArchitectCompassUiManager.useOnBlock(context);
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

        ArchitectCompassUiManager.open(player, hand);
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        ArchitectCompassData.State state = ArchitectCompassData.read(stack);
        if (state.hasCenter()) {
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.architect_compass.configured_full",
                    state.center().getX(),
                    state.center().getY(),
                    state.center().getZ(),
                    state.radius(),
                    Text.translatable(state.plane().translationKey())
            ).formatted(Formatting.AQUA));
        } else {
            tooltip.add(Text.translatable("tooltip.mythicrpg.architect_compass.unconfigured")
                    .formatted(Formatting.GRAY));
        }
        tooltip.add(Text.translatable("tooltip.mythicrpg.architect_compass.set_center")
                .formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.mythicrpg.architect_compass.open_interface")
                .formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("tooltip.mythicrpg.architect_compass.client_only")
                .formatted(Formatting.DARK_AQUA));
    }
}
