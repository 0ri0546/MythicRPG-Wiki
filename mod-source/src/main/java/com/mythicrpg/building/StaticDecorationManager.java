package com.mythicrpg.building;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Owner protection for static decoration anchors. */
public final class StaticDecorationManager {
    private StaticDecorationManager() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                StaticDecorationState.get(server).pruneMissingDimensions(server));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!(blockEntity instanceof StaticDecorationBlockEntity decoration)
                    || decoration.owner() == null
                    || decoration.isOwner(player.getUuid())
                    || player.isCreative()) return true;
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.sendMessage(Text.translatable("message.mythicrpg.static_decoration.not_owner")
                        .formatted(Formatting.RED), true);
            }
            return false;
        });
    }
}
