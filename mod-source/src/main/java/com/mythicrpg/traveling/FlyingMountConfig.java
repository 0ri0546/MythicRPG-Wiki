package com.mythicrpg.traveling;

/**
 * Centralized tuning values for all adopted flying mounts.
 *
 * <p>The species' own movement/flying attribute supplies its cruise speed
 * whenever available. Only mobs without a usable speed attribute need a
 * fallback value here.</p>
 */
public final class FlyingMountConfig {
    public static final double PHANTOM_CRUISE_SPEED = 0.38D;
    public static final double PHANTOM_GROUND_SPEED = 0.18D;
    public static final double PHANTOM_ASCEND_SPEED = 0.24D;
    public static final double PHANTOM_DESCEND_SPEED = 0.20D;
    public static final double GHAST_FALLBACK_CRUISE_SPEED = 0.35D;

    public static final double MIN_CRUISE_SPEED = 0.23D;
    public static final double MAX_CRUISE_SPEED = 0.60D;
    public static final double GROUND_SPEED_MULTIPLIER = 0.55D;
    public static final double ASCEND_SPEED_MULTIPLIER = 0.65D;
    public static final double DESCEND_SPEED_MULTIPLIER = 0.55D;
    public static final double MIN_VERTICAL_SPEED = 0.18D;
    public static final double MAX_VERTICAL_SPEED = 0.32D;

    public static final float TURN_DEGREES_PER_TICK = 3.5F;
    public static final float ASCENDING_PITCH = -18.0F;
    public static final float DESCENDING_PITCH = 15.0F;

    public static final double ANCHOR_RETURN_SPEED = 0.24D;

    private FlyingMountConfig() {
    }
}
