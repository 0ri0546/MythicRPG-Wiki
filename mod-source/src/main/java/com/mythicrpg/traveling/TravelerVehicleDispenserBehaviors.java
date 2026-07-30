package com.mythicrpg.traveling;

import com.mythicrpg.core.ModEntities;
import com.mythicrpg.core.ModItems;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public final class TravelerVehicleDispenserBehaviors {

    private TravelerVehicleDispenserBehaviors() {
    }

    public static void register() {
        DispenserBlock.registerBehavior(
                ModItems.TRAVELER_MINECART,
                new TravelerMinecartDispenserBehavior()
        );
        DispenserBlock.registerBehavior(
                ModItems.TRAVELER_BOAT,
                new TravelerBoatDispenserBehavior()
        );
    }

    private static final class TravelerMinecartDispenserBehavior
            extends ItemDispenserBehavior {

        private final ItemDispenserBehavior fallback = new ItemDispenserBehavior();

        @Override
        protected ItemStack dispenseSilently(
                BlockPointer pointer,
                ItemStack stack
        ) {
            Direction direction = pointer.state().get(DispenserBlock.FACING);
            ServerWorld world = pointer.world();
            Vec3d center = pointer.centerPos();

            double x = center.getX() + direction.getOffsetX() * 1.125D;
            double y = Math.floor(center.getY()) + direction.getOffsetY();
            double z = center.getZ() + direction.getOffsetZ() * 1.125D;

            BlockPos frontPos = pointer.pos().offset(direction);
            BlockState frontState = world.getBlockState(frontPos);
            double verticalOffset;

            if (frontState.isIn(BlockTags.RAILS)) {
                verticalOffset = getRailShape(frontState).isAscending()
                        ? 0.6D
                        : 0.1D;
            } else if (frontState.isAir()
                    && world.getBlockState(frontPos.down()).isIn(BlockTags.RAILS)) {
                RailShape shapeBelow = getRailShape(
                        world.getBlockState(frontPos.down())
                );
                verticalOffset = direction != Direction.DOWN && shapeBelow.isAscending()
                        ? -0.4D
                        : -0.9D;
            } else {
                return fallback.dispense(pointer, stack);
            }

            TravelerMinecartEntity minecart = new TravelerMinecartEntity(
                    world,
                    x,
                    y + verticalOffset,
                    z
            );
            EntityType.copier(world, stack, null).accept(minecart);
            world.spawnEntity(minecart);
            stack.decrement(1);
            return stack;
        }

        private static RailShape getRailShape(BlockState state) {
            if (state.getBlock() instanceof AbstractRailBlock railBlock) {
                return state.get(railBlock.getShapeProperty());
            }

            return RailShape.NORTH_SOUTH;
        }
    }

    private static final class TravelerBoatDispenserBehavior
            extends ItemDispenserBehavior {

        private final ItemDispenserBehavior fallback = new ItemDispenserBehavior();

        @Override
        protected ItemStack dispenseSilently(
                BlockPointer pointer,
                ItemStack stack
        ) {
            Direction direction = pointer.state().get(DispenserBlock.FACING);
            ServerWorld world = pointer.world();
            Vec3d center = pointer.centerPos();

            double horizontalOffset = 0.5625D
                    + ModEntities.TRAVELER_BOAT.getWidth() / 2.0D;
            double x = center.getX()
                    + direction.getOffsetX() * horizontalOffset;
            double y = center.getY()
                    + direction.getOffsetY() * 1.125D;
            double z = center.getZ()
                    + direction.getOffsetZ() * horizontalOffset;

            BlockPos frontPos = pointer.pos().offset(direction);
            double verticalOffset;

            if (world.getFluidState(frontPos).isIn(FluidTags.WATER)) {
                verticalOffset = 1.0D;
            } else if (world.getBlockState(frontPos).isAir()
                    && world.getFluidState(frontPos.down()).isIn(FluidTags.WATER)) {
                verticalOffset = 0.0D;
            } else {
                return fallback.dispense(pointer, stack);
            }

            TravelerBoatEntity boat = new TravelerBoatEntity(
                    world,
                    x,
                    y + verticalOffset,
                    z
            );
            EntityType.copier(world, stack, null).accept(boat);
            boat.setYaw(direction.asRotation());
            world.spawnEntity(boat);
            stack.decrement(1);
            return stack;
        }
    }
}
