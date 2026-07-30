package com.mythicrpg.traveling;

/** Centralized balancing and presentation values for Traveling node 20. */
public final class GrapplingHookConfig {
    /** Maximum straight-line raycast distance. */
    public static final double MAX_RANGE_BLOCKS = 32.0D;

    /** Both the outgoing rope and the player travel at the same configured speed. */
    public static final double TRAVEL_SPEED_BLOCKS_PER_SECOND = 10.0D;
    public static final double TRAVEL_SPEED_BLOCKS_PER_TICK =
            TRAVEL_SPEED_BLOCKS_PER_SECOND / 20.0D;
    public static final double TICKS_PER_BLOCK =
            20.0D / TRAVEL_SPEED_BLOCKS_PER_SECOND;

    /** Stop slightly before numerical contact with the destination. */
    public static final double ARRIVAL_DISTANCE = 0.30D;

    /** Number of almost-motionless ticks before considering the path blocked. */
    public static final int MAX_STALLED_TICKS = 8;

    /** Extra safety ticks beyond the theoretical rope + player travel duration. */
    public static final int MAX_DURATION_GRACE_TICKS = 40;

    /** Briefly clears fall distance after a successful pull. */
    public static final int POST_ARRIVAL_FALL_PROTECTION_TICKS = 20;

    /** How far the safe endpoint may be backed away from the struck surface. */
    public static final double SAFE_POSITION_SEARCH_DISTANCE = 2.0D;
    public static final double SAFE_POSITION_SEARCH_STEP = 0.20D;

    private GrapplingHookConfig() {
    }
}
