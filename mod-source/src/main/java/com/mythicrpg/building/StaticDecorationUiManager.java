package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

/** Server authority for configuring generator items and owner decorations. */
public final class StaticDecorationUiManager {
    private StaticDecorationUiManager() {
    }

    public static void openItem(ServerPlayerEntity player, Hand hand) {
        if (!canUse(player)) {
            deny(player, "message.mythicrpg.static_decoration.no_perk");
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModBlocks.STATIC_DECORATION.asItem())) {
            return;
        }
        StaticDecorationEffect effect = StaticDecorationItemData.read(stack);
        BuildingUiSessionManager.open(
                player,
                BuildingUiSessionManager.Tool.STATIC_DECORATION_ITEM,
                hand,
                stack,
                Long.MIN_VALUE
        );
        send(player, hand, true, false, BlockPos.ORIGIN, effect, "", false);
    }

    public static void openBlock(
            ServerPlayerEntity player,
            Hand hand,
            BlockPos pos,
            StaticDecorationBlockEntity decoration
    ) {
        if (!canUse(player)) {
            deny(player, "message.mythicrpg.static_decoration.no_perk");
            return;
        }
        if (decoration == null || (!decoration.isOwner(player.getUuid()) && !player.isCreative())) {
            deny(player, "message.mythicrpg.static_decoration.not_owner");
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        BuildingUiSessionManager.open(
                player,
                BuildingUiSessionManager.Tool.STATIC_DECORATION_BLOCK,
                hand,
                stack,
                pos.asLong()
        );
        send(player, hand, true, true, pos, decoration.effect(), "", false);
    }

    public static void handle(ServerPlayerEntity player, StaticDecorationUiActionPayload payload) {
        Hand hand = payload.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        if (!canUse(player)) {
            deny(player, "message.mythicrpg.static_decoration.no_perk");
            send(player, hand, false, payload.editingBlock(), payload.targetPos(),
                    StaticDecorationEffect.byIndex(payload.effectIndex()),
                    "message.mythicrpg.static_decoration.no_perk", true);
            return;
        }

        ItemStack stack = player.getStackInHand(hand);
        BuildingUiSessionManager.Tool sessionTool = payload.editingBlock()
                ? BuildingUiSessionManager.Tool.STATIC_DECORATION_BLOCK
                : BuildingUiSessionManager.Tool.STATIC_DECORATION_ITEM;
        long target = payload.editingBlock() ? payload.targetPosPacked() : Long.MIN_VALUE;
        if (!BuildingUiSessionManager.allow(
                player,
                sessionTool,
                hand,
                stack,
                BuildingUiSessionManager.ActionCost.MUTATION,
                payload.hashCode(),
                target
        )) {
            return;
        }

        StaticDecorationEffect effect = StaticDecorationEffect.byIndex(payload.effectIndex());
        if (payload.editingBlock()) {
            applyBlock(player, hand, payload.targetPos(), effect);
        } else {
            applyItem(player, hand, effect);
        }
    }

    private static void applyItem(
            ServerPlayerEntity player,
            Hand hand,
            StaticDecorationEffect effect
    ) {
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModBlocks.STATIC_DECORATION.asItem())) {
            send(player, hand, false, false, BlockPos.ORIGIN, effect,
                    "screen.mythicrpg.static_decoration_ui.item_missing", true);
            BuildingSoundFeedback.error(player);
            return;
        }
        StaticDecorationItemData.write(stack, effect);
        player.getInventory().markDirty();
        BuildingSoundFeedback.decorationApplied(player, player.getBlockPos());
        player.sendMessage(Text.translatable(
                "message.mythicrpg.static_decoration.selected",
                Text.translatable(effect.translationKey())
        ).formatted(Formatting.AQUA), true);
        send(player, hand, false, false, BlockPos.ORIGIN, effect,
                "screen.mythicrpg.static_decoration_ui.saved", false);
        BuildingUiSessionManager.close(player);
    }

    private static void applyBlock(
            ServerPlayerEntity player,
            Hand hand,
            BlockPos pos,
            StaticDecorationEffect effect
    ) {
        ServerWorld world = player.getServerWorld();
        if (!player.getStackInHand(hand).isOf(ModBlocks.STATIC_DECORATION.asItem())) {
            send(player, hand, false, true, pos, effect,
                    "screen.mythicrpg.static_decoration_ui.item_missing", true);
            BuildingSoundFeedback.error(player);
            return;
        }
        if (player.squaredDistanceTo(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D
        ) > 64.0D) {
            send(player, hand, true, true, pos, effect,
                    "screen.mythicrpg.static_decoration_ui.target_missing", true);
            BuildingSoundFeedback.error(player);
            return;
        }
        if (!world.isInBuildLimit(pos)
                || !world.getWorldBorder().contains(pos)
                || !(world.getBlockEntity(pos) instanceof StaticDecorationBlockEntity decoration)) {
            send(player, hand, true, true, pos, effect,
                    "screen.mythicrpg.static_decoration_ui.target_missing", true);
            BuildingSoundFeedback.error(player);
            return;
        }
        if (!decoration.isOwner(player.getUuid()) && !player.isCreative()) {
            send(player, hand, true, true, pos, decoration.effect(),
                    "message.mythicrpg.static_decoration.not_owner", true);
            BuildingSoundFeedback.error(player);
            return;
        }
        decoration.setEffect(effect);
        BuildingSoundFeedback.decorationApplied(player, pos);
        player.sendMessage(Text.translatable(
                "message.mythicrpg.static_decoration.updated",
                Text.translatable(effect.translationKey())
        ).formatted(Formatting.AQUA), true);
        send(player, hand, false, true, pos, effect,
                "screen.mythicrpg.static_decoration_ui.saved", false);
        BuildingUiSessionManager.close(player);
    }

    private static void send(
            ServerPlayerEntity player,
            Hand hand,
            boolean open,
            boolean editing,
            BlockPos pos,
            StaticDecorationEffect effect,
            String messageKey,
            boolean error
    ) {
        ServerPlayNetworking.send(player, new StaticDecorationUiStatePayload(
                hand == Hand.MAIN_HAND ? 0 : 1,
                open,
                editing,
                player.getServerWorld().getRegistryKey().getValue().toString(),
                pos.asLong(),
                effect.ordinal(),
                messageKey,
                error
        ));
    }

    private static boolean canUse(ServerPlayerEntity player) {
        return SkillTreeManager.hasBonus(
                player,
                SkillType.BUILDING,
                BonusType.BUILD_STATIC_DECORATION
        );
    }

    private static void deny(ServerPlayerEntity player, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true);
        BuildingSoundFeedback.error(player);
    }
}
