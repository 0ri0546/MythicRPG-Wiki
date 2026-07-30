package com.mythicrpg.mining;

import com.mythicrpg.core.ModAttachments;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class MiningToggleState {
    private MiningToggleState() {}

    public static void setAreaMiningEnabled(ServerPlayerEntity player, boolean enabled) {
        player.setAttached(ModAttachments.MINING_AREA_3X3_ENABLED, enabled);
        sync(player);
    }

    public static boolean isAreaMiningEnabled(ServerPlayerEntity player) {
        return player.getAttachedOrCreate(ModAttachments.MINING_AREA_3X3_ENABLED);
    }

    public static void sync(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new MiningToggleStatePayload(isAreaMiningEnabled(player)));
    }
}
