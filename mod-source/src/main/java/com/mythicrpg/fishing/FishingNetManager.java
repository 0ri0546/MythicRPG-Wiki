package com.mythicrpg.fishing;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class FishingNetManager {
    private FishingNetManager() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                FishingNetState.get(server).pruneMissingDimensions(server)
        );
    }

    public static boolean claim(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = player.getServerWorld();
        return FishingNetState.get(world.getServer()).claim(
                world,
                pos,
                player.getUuid()
        );
    }

    public static boolean ensureActive(UUID owner, ServerWorld world, BlockPos pos) {
        return FishingNetState.get(world.getServer()).ensureActive(world, pos, owner);
    }

    public static void release(UUID owner, ServerWorld world, BlockPos pos) {
        FishingNetState.get(world.getServer()).release(world, pos, owner);
    }
}
