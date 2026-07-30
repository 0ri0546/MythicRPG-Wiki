package com.mythicrpg.fighting.items;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.Optional;

public final class BaronItemTargeting {

    private BaronItemTargeting() {
    }

    public static Optional<EntityTarget> findLivingTarget(PlayerEntity player, double range) {
        World world = player.getWorld();
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d look = player.getRotationVec(1.0f);
        Vec3d end = start.add(look.multiply(range));

        double blockedDistanceSq = getBlockHitDistanceSq(world, player, start, end);
        Box searchBox = player.getBoundingBox().stretch(look.multiply(range)).expand(1.0);

        LivingEntity bestTarget = null;
        Vec3d bestHit = null;
        double bestDistanceSq = Math.min(range * range, blockedDistanceSq);

        for (Entity entity : world.getOtherEntities(player, searchBox, candidate ->
                candidate instanceof LivingEntity living
                        && living.isAlive()
                        && !candidate.isSpectator()
                        && candidate.isAttackable()
        )) {
            Box targetBox = entity.getBoundingBox().expand(0.35);
            Optional<Vec3d> hit = targetBox.raycast(start, end);

            if (hit.isEmpty()) {
                continue;
            }

            double distanceSq = start.squaredDistanceTo(hit.get());

            if (distanceSq < bestDistanceSq) {
                bestTarget = (LivingEntity) entity;
                bestHit = hit.get();
                bestDistanceSq = distanceSq;
            }
        }

        if (bestTarget == null || bestHit == null) {
            return Optional.empty();
        }

        return Optional.of(new EntityTarget(bestTarget, bestHit, bestDistanceSq));
    }

    public static BlockHitResult raycastBlock(PlayerEntity player, double range) {
        Vec3d start = player.getCameraPosVec(1.0f);
        Vec3d end = start.add(player.getRotationVec(1.0f).multiply(range));

        return player.getWorld().raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));
    }

    private static double getBlockHitDistanceSq(World world, PlayerEntity player, Vec3d start, Vec3d end) {
        BlockHitResult blockHit = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (blockHit.getType() == HitResult.Type.MISS) {
            return start.squaredDistanceTo(end);
        }

        return start.squaredDistanceTo(blockHit.getPos());
    }

    public record EntityTarget(LivingEntity entity, Vec3d hitPos, double distanceSq) {
    }
}
