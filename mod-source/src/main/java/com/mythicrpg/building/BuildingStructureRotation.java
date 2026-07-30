package com.mythicrpg.building;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Immutable quarter-turn rotation shared by the 2D plan, 3D plan and miniature UIs.
 *
 * <p>Rotations are applied in X, then Y, then Z order. Coordinates are translated
 * after every turn so the resulting structure always starts at local coordinate 0.</p>
 */
public record BuildingStructureRotation(int xQuarterTurns, int yQuarterTurns, int zQuarterTurns) {
    public static final BuildingStructureRotation NONE = new BuildingStructureRotation(0, 0, 0);

    public BuildingStructureRotation {
        xQuarterTurns = normalize(xQuarterTurns);
        yQuarterTurns = normalize(yQuarterTurns);
        zQuarterTurns = normalize(zQuarterTurns);
    }

    /**
     * Applies one additional world-axis quarter turn to the current orientation.
     *
     * <p>The result is canonicalized back to an X/Y/Z triple, so button order is
     * preserved: rotating X then Y is intentionally different from rotating Y
     * then X.</p>
     */
    public BuildingStructureRotation rotate(BuildingRotationAxis axis) {
        if (axis == null) {
            return this;
        }
        Direction east = rotateQuarterTurn(rotateDirection(Direction.EAST), axis);
        Direction up = rotateQuarterTurn(rotateDirection(Direction.UP), axis);
        Direction south = rotateQuarterTurn(rotateDirection(Direction.SOUTH), axis);

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    BuildingStructureRotation candidate = new BuildingStructureRotation(x, y, z);
                    if (candidate.rotateDirection(Direction.EAST) == east
                            && candidate.rotateDirection(Direction.UP) == up
                            && candidate.rotateDirection(Direction.SOUTH) == south) {
                        return candidate;
                    }
                }
            }
        }
        throw new IllegalStateException("Unable to canonicalize Building structure rotation");
    }

    private static Direction rotateQuarterTurn(Direction direction, BuildingRotationAxis axis) {
        int x = direction.getOffsetX();
        int y = direction.getOffsetY();
        int z = direction.getOffsetZ();
        Direction rotated = switch (axis) {
            case X -> Direction.fromVector(x, -z, y);
            case Y -> Direction.fromVector(-z, y, x);
            case Z -> Direction.fromVector(-y, x, z);
        };
        if (rotated == null) {
            throw new IllegalStateException("Quarter turn produced a non-cardinal direction");
        }
        return rotated;
    }

    public int degrees(BuildingRotationAxis axis) {
        return switch (axis) {
            case X -> xQuarterTurns * 90;
            case Y -> yQuarterTurns * 90;
            case Z -> zQuarterTurns * 90;
        };
    }

    public Size rotatedSize(int sizeX, int sizeY, int sizeZ) {
        MutablePoint point = new MutablePoint(0, 0, 0, sizeX, sizeY, sizeZ);
        applyRotations(point, false);
        return new Size(point.sizeX, point.sizeY, point.sizeZ);
    }

    public BlockPos rotateOffset(BlockPos offset, int sizeX, int sizeY, int sizeZ) {
        MutablePoint point = new MutablePoint(
                offset.getX(),
                offset.getY(),
                offset.getZ(),
                sizeX,
                sizeY,
                sizeZ
        );
        applyRotations(point, true);
        return new BlockPos(point.x, point.y, point.z);
    }

    public Direction rotateDirection(Direction direction) {
        int x = direction.getOffsetX();
        int y = direction.getOffsetY();
        int z = direction.getOffsetZ();

        for (int turn = 0; turn < xQuarterTurns; turn++) {
            int nextY = -z;
            int nextZ = y;
            y = nextY;
            z = nextZ;
        }
        for (int turn = 0; turn < yQuarterTurns; turn++) {
            int nextX = -z;
            int nextZ = x;
            x = nextX;
            z = nextZ;
        }
        for (int turn = 0; turn < zQuarterTurns; turn++) {
            int nextX = -y;
            int nextY = x;
            x = nextX;
            y = nextY;
        }

        Direction rotated = Direction.fromVector(x, y, z);
        return rotated == null ? direction : rotated;
    }

    public Direction.Axis rotateAxis(Direction.Axis axis) {
        Direction representative = switch (axis) {
            case X -> Direction.EAST;
            case Y -> Direction.UP;
            case Z -> Direction.SOUTH;
        };
        return rotateDirection(representative).getAxis();
    }

    private void applyRotations(MutablePoint point, boolean rotateCoordinates) {
        for (int turn = 0; turn < xQuarterTurns; turn++) {
            if (rotateCoordinates) {
                int nextY = point.sizeZ - 1 - point.z;
                int nextZ = point.y;
                point.y = nextY;
                point.z = nextZ;
            }
            int previousY = point.sizeY;
            point.sizeY = point.sizeZ;
            point.sizeZ = previousY;
        }

        for (int turn = 0; turn < yQuarterTurns; turn++) {
            if (rotateCoordinates) {
                int nextX = point.sizeZ - 1 - point.z;
                int nextZ = point.x;
                point.x = nextX;
                point.z = nextZ;
            }
            int previousX = point.sizeX;
            point.sizeX = point.sizeZ;
            point.sizeZ = previousX;
        }

        for (int turn = 0; turn < zQuarterTurns; turn++) {
            if (rotateCoordinates) {
                int nextX = point.sizeY - 1 - point.y;
                int nextY = point.x;
                point.x = nextX;
                point.y = nextY;
            }
            int previousX = point.sizeX;
            point.sizeX = point.sizeY;
            point.sizeY = previousX;
        }
    }

    private static int normalize(int turns) {
        return Math.floorMod(turns, 4);
    }

    public record Size(int x, int y, int z) {
        public Size {
            if (x < 1 || y < 1 || z < 1) {
                throw new IllegalArgumentException("Structure dimensions must be positive");
            }
        }
    }

    private static final class MutablePoint {
        private int x;
        private int y;
        private int z;
        private int sizeX;
        private int sizeY;
        private int sizeZ;

        private MutablePoint(int x, int y, int z, int sizeX, int sizeY, int sizeZ) {
            if (sizeX < 1 || sizeY < 1 || sizeZ < 1) {
                throw new IllegalArgumentException("Structure dimensions must be positive");
            }
            this.x = x;
            this.y = y;
            this.z = z;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
        }
    }
}
