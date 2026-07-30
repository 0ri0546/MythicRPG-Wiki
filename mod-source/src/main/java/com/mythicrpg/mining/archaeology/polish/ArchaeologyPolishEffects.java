package com.mythicrpg.mining.archaeology.polish;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/** Shared lightweight visual helpers used by archaeology polish effects. */
public final class ArchaeologyPolishEffects {

    private ArchaeologyPolishEffects() {
    }

    public static void spawnHorizontalRing(
            ServerWorld world,
            ParticleEffect particle,
            Vec3d center,
            double radius,
            int pointCount,
            double yOffset
    ) {
        int points = Math.max(8, pointCount);
        for (int index = 0; index < points; index++) {
            double angle = Math.PI * 2.0 * index / points;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            world.spawnParticles(
                    particle,
                    x,
                    center.y + yOffset,
                    z,
                    1,
                    0.0,
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    public static String formatTicks(long ticks) {
        long totalSeconds = Math.max(0L, (ticks + 19L) / 20L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes > 0L
                ? minutes + ":" + (seconds < 10L ? "0" : "") + seconds
                : totalSeconds + "s";
    }
}
