package com.mythicrpg.fighting.barons;

import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class DarknightBaronBehavior {

    private static final double SEARCH_RADIUS = 32.0;

    private DarknightBaronBehavior() {
    }

    public static void tick(ServerWorld world, SpiderEntity spider) {
        if (spider.getTarget() != null && spider.getTarget().isAlive()) {
            return;
        }

        ServerPlayerEntity nearestPlayer = BaronEntityQuery.findNearestValidPlayerTarget(
                world,
                spider,
                SEARCH_RADIUS
        );

        if (nearestPlayer == null) {
            return;
        }

        spider.setTarget(nearestPlayer);

        world.spawnParticles(
                ParticleTypes.SMOKE,
                spider.getX(),
                spider.getBodyY(0.8),
                spider.getZ(),
                8,
                0.25,
                0.2,
                0.25,
                0.02
        );
    }
}
