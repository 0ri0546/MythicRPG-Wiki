package com.mythicrpg.core;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public final class WorldScanUtils {
    private WorldScanUtils() {
    }

    @FunctionalInterface
    public interface BlockAction {
        boolean apply(BlockPos pos, BlockState state);
    }

    public static boolean hasBlockInBox(
            ServerWorld world,
            BlockPos center,
            int radiusXz,
            int minYOffset,
            int maxYOffset,
            Predicate<BlockState> predicate
    ) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -radiusXz; x <= radiusXz; x++) {
            for (int y = minYOffset; y <= maxYOffset; y++) {
                for (int z = -radiusXz; z <= radiusXz; z++) {
                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);

                    if (predicate.test(world.getBlockState(mutable))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static int forEachBlockInBox(
            ServerWorld world,
            BlockPos center,
            int radiusXz,
            int minYOffset,
            int maxYOffset,
            int maxMatches,
            BlockAction action
    ) {
        int matches = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -radiusXz; x <= radiusXz; x++) {
            for (int y = minYOffset; y <= maxYOffset; y++) {
                for (int z = -radiusXz; z <= radiusXz; z++) {
                    if (matches >= maxMatches) {
                        return matches;
                    }

                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockPos targetPos = mutable.toImmutable();
                    BlockState targetState = world.getBlockState(targetPos);

                    if (action.apply(targetPos, targetState)) {
                        matches++;
                    }
                }
            }
        }

        return matches;
    }

    public static int forEachBlockInCylinder(
            ServerWorld world,
            BlockPos center,
            int radius,
            int minYOffset,
            int maxYOffset,
            boolean skipCenter,
            int maxMatches,
            BlockAction action
    ) {
        int matches = 0;
        int radiusSquared = radius * radius;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radiusSquared) {
                    continue;
                }

                for (int y = minYOffset; y <= maxYOffset; y++) {
                    if (matches >= maxMatches) {
                        return matches;
                    }

                    if (skipCenter && x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    mutable.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockPos targetPos = mutable.toImmutable();
                    BlockState targetState = world.getBlockState(targetPos);

                    if (action.apply(targetPos, targetState)) {
                        matches++;
                    }
                }
            }
        }

        return matches;
    }
}
