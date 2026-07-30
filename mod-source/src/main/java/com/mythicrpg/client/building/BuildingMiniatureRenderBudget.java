package com.mythicrpg.client.building;

/** Hard client-side caps for miniature entities and rendered blocks per world frame. */
public final class BuildingMiniatureRenderBudget {
    public static final int MAX_ENTITIES_PER_FRAME = 32;
    public static final int MAX_BLOCKS_PER_FRAME = 3_200;

    private static int remainingEntities;
    private static int remainingBlocks;

    private BuildingMiniatureRenderBudget() {
    }

    public static void beginFrame() {
        remainingEntities = MAX_ENTITIES_PER_FRAME;
        remainingBlocks = MAX_BLOCKS_PER_FRAME;
    }

    public static boolean tryAcquire(int blockCount) {
        int safeCount = Math.max(0, blockCount);
        if (remainingEntities <= 0 || safeCount > remainingBlocks) {
            return false;
        }
        remainingEntities--;
        remainingBlocks -= safeCount;
        return true;
    }
}
