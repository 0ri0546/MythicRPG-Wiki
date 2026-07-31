package com.mythicrpg.crafting;

import com.mythicrpg.fighting.BaronMobManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class LuckyBlockBaronBridge {

    private LuckyBlockBaronBridge() {
    }

    public static boolean spawnRandomBaron(
            ServerWorld world,
            BlockPos ritualPos,
            ServerPlayerEntity player
    ) {
        return BaronMobManager.spawnLuckyBlockBaron(world, ritualPos, player);
    }
}