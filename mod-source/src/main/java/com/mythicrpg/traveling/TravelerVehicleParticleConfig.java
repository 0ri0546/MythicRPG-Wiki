package com.mythicrpg.traveling;

/**
 * Centralized visual-polish values for the custom Traveling vehicles.
 *
 * <p>Speeds are expressed in blocks per tick. Particle intervals are expressed
 * in game ticks (20 ticks = 1 second).</p>
 */
public final class TravelerVehicleParticleConfig {

    private TravelerVehicleParticleConfig() {
    }

    // Traveler's Minecart
    public static final double MINECART_MIN_HORIZONTAL_SPEED = 0.08D;
    public static final int MINECART_PARTICLE_INTERVAL_TICKS = 4;
    public static final double MINECART_REAR_OFFSET = 0.34D;
    public static final double MINECART_SIDE_OFFSET = 0.38D;
    public static final double MINECART_PARTICLE_HEIGHT = 0.12D;

    // Traveler's Boat
    public static final double BOAT_MIN_HORIZONTAL_SPEED = 0.06D;
    public static final int BOAT_PARTICLE_INTERVAL_TICKS = 3;
    public static final double BOAT_REAR_OFFSET = 0.90D;
    public static final double BOAT_SIDE_OFFSET = 0.46D;
    public static final double BOAT_PARTICLE_HEIGHT = 0.12D;
}
