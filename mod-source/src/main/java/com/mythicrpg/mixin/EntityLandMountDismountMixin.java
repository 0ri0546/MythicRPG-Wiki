package com.mythicrpg.mixin;

import com.mythicrpg.traveling.LandMountManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityLandMountDismountMixin {
    @Inject(method = "updatePassengerForDismount", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$findSafeMountDismount(
            LivingEntity passenger,
            CallbackInfoReturnable<Vec3d> cir
    ) {
        Entity self = (Entity) (Object) this;

        if (!(self instanceof MobEntity mount) || !LandMountManager.isAdoptedMount(mount)) {
            return;
        }

        Vec3d safePosition = findNearestSafePosition(mount, passenger);
        if (safePosition != null) {
            cir.setReturnValue(safePosition);
        }
    }

    private static Vec3d findNearestSafePosition(MobEntity mount, LivingEntity passenger) {
        World world = mount.getWorld();
        int radius = Math.max(1, (int) Math.ceil(Math.max(mount.getWidth(), mount.getHeight())) + 1);
        BlockPos origin = mount.getBlockPos();
        BlockPos best = null;
        double bestDistanceSquared = Double.MAX_VALUE;

        for (int yOffset = -radius; yOffset <= radius; yOffset++) {
            for (int xOffset = -radius; xOffset <= radius; xOffset++) {
                for (int zOffset = -radius; zOffset <= radius; zOffset++) {
                    BlockPos feet = origin.add(xOffset, yOffset, zOffset);
                    double distanceSquared = xOffset * xOffset + yOffset * yOffset + zOffset * zOffset;

                    if (distanceSquared >= bestDistanceSquared || !isViable(world, feet)) {
                        continue;
                    }

                    best = feet.toImmutable();
                    bestDistanceSquared = distanceSquared;
                }
            }
        }

        if (best == null) {
            return null;
        }

        return new Vec3d(best.getX() + 0.5D, best.getY(), best.getZ() + 0.5D);
    }

    private static boolean isViable(World world, BlockPos feet) {
        BlockPos floor = feet.down();
        BlockPos head = feet.up();

        if (!world.getBlockState(floor).isSideSolidFullSquare(world, floor, Direction.UP)) {
            return false;
        }

        if (!world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                || !world.getBlockState(head).getCollisionShape(world, head).isEmpty()) {
            return false;
        }

        return !world.getFluidState(floor).isIn(FluidTags.LAVA)
                && !world.getFluidState(feet).isIn(FluidTags.LAVA)
                && !world.getFluidState(head).isIn(FluidTags.LAVA);
    }
}
