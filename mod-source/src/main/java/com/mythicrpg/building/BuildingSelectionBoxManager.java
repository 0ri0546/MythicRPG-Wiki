package com.mythicrpg.building;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/** Server-side send helpers for the shared Building selection box. */
public final class BuildingSelectionBoxManager {
    private BuildingSelectionBoxManager() {
    }

    public static void show(
            ServerPlayerEntity player,
            BuildingUiTool tool,
            String dimensionId,
            BlockPos first,
            BlockPos second,
            boolean valid
    ) {
        if (player == null || tool == null || first == null) {
            return;
        }
        ServerPlayNetworking.send(
                player,
                BuildingSelectionBoxPayload.show(tool, dimensionId, first, second, valid)
        );
    }

    public static void clear(ServerPlayerEntity player) {
        if (player != null) {
            ServerPlayNetworking.send(player, BuildingSelectionBoxPayload.clear());
        }
    }
}
