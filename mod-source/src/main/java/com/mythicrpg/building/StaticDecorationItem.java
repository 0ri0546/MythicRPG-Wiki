package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Configures one vanilla particle in the UI, then places one non-ticking anchor block. */
public final class StaticDecorationItem extends BlockItem {
    public StaticDecorationItem(net.minecraft.block.Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        if (user instanceof ServerPlayerEntity player) {
            StaticDecorationUiManager.openItem(player, hand);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (!SkillTreeManager.hasBonus(serverPlayer, SkillType.BUILDING, BonusType.BUILD_STATIC_DECORATION)) {
                serverPlayer.sendMessage(Text.translatable("message.mythicrpg.static_decoration.no_perk")
                        .formatted(Formatting.RED), true);
                BuildingSoundFeedback.error(serverPlayer);
                return ActionResult.FAIL;
            }
            net.minecraft.item.ItemPlacementContext placement = new net.minecraft.item.ItemPlacementContext(context);
            if (!placement.getWorld().getFluidState(placement.getBlockPos()).isEmpty()) {
                serverPlayer.sendMessage(Text.translatable("message.mythicrpg.static_decoration.blocked")
                        .formatted(Formatting.RED), true);
                BuildingSoundFeedback.error(serverPlayer);
                return ActionResult.FAIL;
            }
            if (placement.getWorld() instanceof ServerWorld serverWorld
                    && !StaticDecorationState.get(serverWorld.getServer()).canPlace(
                            serverWorld,
                            placement.getBlockPos(),
                            serverPlayer.getUuid()
                    )) {
                serverPlayer.sendMessage(Text.translatable("message.mythicrpg.static_decoration.limit")
                        .formatted(Formatting.RED), true);
                BuildingSoundFeedback.error(serverPlayer);
                return ActionResult.FAIL;
            }
        }
        return super.useOnBlock(context);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.translatable("item.mythicrpg.static_decoration_generator");
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        StaticDecorationEffect effect = StaticDecorationItemData.read(stack);
        tooltip.add(Text.translatable("tooltip.mythicrpg.static_decoration.effect",
                Text.translatable(effect.translationKey())).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.mythicrpg.static_decoration.open_ui").formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.mythicrpg.static_decoration.place").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.static_decoration.limits",
                StaticDecorationState.MAX_PER_PLAYER,
                StaticDecorationState.MAX_PER_CHUNK).formatted(Formatting.DARK_GRAY));
    }
}
