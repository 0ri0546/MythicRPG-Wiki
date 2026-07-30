package com.mythicrpg.client.building;

/** Hard per-frame client budget for static decoration anchors. */
public final class StaticDecorationRenderBudget {
    public static final int MAX_PER_FRAME = 64;

    private static int remaining;

    private StaticDecorationRenderBudget() {
    }

    public static void beginFrame() {
        remaining = MAX_PER_FRAME;
    }

    public static boolean tryAcquire() {
        if (remaining <= 0) return false;
        remaining--;
        return true;
    }
}
