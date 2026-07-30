package com.mythicrpg.building;

import net.minecraft.block.BlockState;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Safe rigid rotations used by locked Building plans.
 *
 * <p>Coordinates rotate around selection point A. Block-state properties are
 * transformed only when their target value can be represented by the block's
 * existing state manager. Unsupported rotations are rejected before a plan is
 * locked or modified.</p>
 */
public final class BuildingPlanTransforms {
    private static final Map<String, Direction> DIRECTION_PROPERTY_NAMES = Map.of(
            "down", Direction.DOWN,
            "up", Direction.UP,
            "north", Direction.NORTH,
            "south", Direction.SOUTH,
            "west", Direction.WEST,
            "east", Direction.EAST
    );

    private BuildingPlanTransforms() {
    }

    /** Rotates a signed vector around local origin without translating its bounds. */
    public static BlockPos rotateVector(BlockPos source, BuildingStructureRotation rotation) {
        BuildingStructureRotation safe = safe(rotation);
        int x = source.getX();
        int y = source.getY();
        int z = source.getZ();

        for (int turn = 0; turn < safe.xQuarterTurns(); turn++) {
            int nextY = -z;
            int nextZ = y;
            y = nextY;
            z = nextZ;
        }
        for (int turn = 0; turn < safe.yQuarterTurns(); turn++) {
            int nextX = -z;
            int nextZ = x;
            x = nextX;
            z = nextZ;
        }
        for (int turn = 0; turn < safe.zQuarterTurns(); turn++) {
            int nextX = -y;
            int nextY = x;
            x = nextX;
            y = nextY;
        }
        return new BlockPos(x, y, z);
    }

    public static BlankBlockAppearance rotateAppearance(
            BlankBlockAppearance appearance,
            BuildingStructureRotation rotation
    ) {
        BlankBlockAppearance source = appearance == null ? BlankBlockAppearance.EMPTY : appearance;
        BuildingStructureRotation safe = safe(rotation);
        if (source.isEmpty() || safe.equals(BuildingStructureRotation.NONE)) {
            return source;
        }

        BlankBlockAppearance result = BlankBlockAppearance.EMPTY;
        for (Direction sourceFace : Direction.values()) {
            Identifier material = source.material(sourceFace);
            if (material != null) {
                result = result.with(safe.rotateDirection(sourceFace), material);
            }
        }
        return result;
    }

    public static Optional<BlockState> rotateState(
            BlockState source,
            BuildingStructureRotation rotation
    ) {
        if (source == null) {
            return Optional.empty();
        }
        BuildingStructureRotation safe = safe(rotation);
        if (safe.equals(BuildingStructureRotation.NONE)) {
            return Optional.of(source);
        }

        BlockState result = source;

        // Direction-valued properties, including horizontal-only facings.
        for (Property<?> property : source.getProperties()) {
            if (!(property instanceof DirectionProperty directionProperty)) {
                continue;
            }
            Direction current = source.get(directionProperty);
            Direction target = safe.rotateDirection(current);
            if (!directionProperty.getValues().contains(target)) {
                return Optional.empty();
            }
            result = result.with(directionProperty, target);
        }

        // Axis-valued properties such as logs and pillars.
        for (Property<?> property : source.getProperties()) {
            Comparable<?> current = source.get(property);
            if (!(current instanceof Direction.Axis axis)) {
                continue;
            }
            Direction.Axis target = safe.rotateAxis(axis);
            Optional<BlockState> changed = setRaw(result, property, target);
            if (changed.isEmpty()) {
                return Optional.empty();
            }
            result = changed.get();
        }

        // Direction-named connection values used by fences, panes and walls.
        // Values are copied generically so enum wall shapes rotate as well as booleans.
        Map<Direction, DirectionalValue> connections = new HashMap<>();
        for (Map.Entry<String, Direction> entry : DIRECTION_PROPERTY_NAMES.entrySet()) {
            Property<?> property = source.getBlock().getStateManager().getProperty(entry.getKey());
            if (property != null) {
                connections.put(
                        entry.getValue(),
                        new DirectionalValue(property, source.get(property))
                );
            }
        }
        if (!connections.isEmpty()) {
            for (Map.Entry<Direction, DirectionalValue> entry : connections.entrySet()) {
                Direction targetDirection = safe.rotateDirection(entry.getKey());
                Property<?> targetProperty = source.getBlock().getStateManager().getProperty(
                        targetDirection.asString()
                );
                if (targetProperty == null) {
                    return Optional.empty();
                }
                Optional<BlockState> changed = setSerialized(
                        result,
                        targetProperty,
                        propertyName(entry.getValue().property(), entry.getValue().value())
                );
                if (changed.isEmpty()) {
                    return Optional.empty();
                }
                result = changed.get();
            }
        }

        // Sign/standing rotation (0..15) is horizontal and only supports Y turns.
        Property<?> rotationProperty = source.getBlock().getStateManager().getProperty("rotation");
        if (rotationProperty != null) {
            Comparable<?> raw = source.get(rotationProperty);
            if (raw instanceof Integer value) {
                if (safe.xQuarterTurns() != 0 || safe.zQuarterTurns() != 0) {
                    return Optional.empty();
                }
                int target = Math.floorMod(value + safe.yQuarterTurns() * 4, 16);
                Optional<BlockState> changed = setRaw(result, rotationProperty, target);
                if (changed.isEmpty()) {
                    return Optional.empty();
                }
                result = changed.get();
            }
        }

        // TOP/BOTTOM states can survive a rotation only while world-up remains vertical.
        Direction rotatedUp = safe.rotateDirection(Direction.UP);
        for (String propertyName : new String[]{"half", "type"}) {
            Property<?> property = source.getBlock().getStateManager().getProperty(propertyName);
            if (property == null) {
                continue;
            }
            Comparable<?> value = source.get(property);
            String serialized = propertyName(property, value);
            if (!serialized.equals("top") && !serialized.equals("bottom")) {
                continue;
            }
            if (rotatedUp.getAxis() != Direction.Axis.Y) {
                return Optional.empty();
            }
            String targetName = rotatedUp == Direction.DOWN
                    ? (serialized.equals("top") ? "bottom" : "top")
                    : serialized;
            Optional<BlockState> changed = setSerialized(result, property, targetName);
            if (changed.isEmpty()) {
                return Optional.empty();
            }
            result = changed.get();
        }

        // Floor/wall/ceiling mounting cannot be represented safely after a tilt.
        if (source.getBlock().getStateManager().getProperty("face") != null
                && (safe.xQuarterTurns() != 0 || safe.zQuarterTurns() != 0)) {
            return Optional.empty();
        }

        return Optional.of(result);
    }

    public static boolean canRotate(
            Iterable<? extends RotatableEntry> entries,
            BuildingStructureRotation rotation
    ) {
        for (RotatableEntry entry : entries) {
            if (entry == null || rotateState(entry.state(), rotation).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static BuildingStructureRotation safe(BuildingStructureRotation rotation) {
        return rotation == null ? BuildingStructureRotation.NONE : rotation;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> Optional<BlockState> setRaw(
            BlockState state,
            Property<?> property,
            Comparable<?> value
    ) {
        Property<T> typedProperty;
        T typedValue;
        try {
            typedProperty = (Property<T>) property;
            typedValue = (T) value;
        } catch (ClassCastException ignored) {
            return Optional.empty();
        }
        if (!typedProperty.getValues().contains(typedValue)) {
            return Optional.empty();
        }
        try {
            return Optional.of(state.with(typedProperty, typedValue));
        } catch (IllegalArgumentException | ClassCastException ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Optional<BlockState> setSerialized(
            BlockState state,
            Property<?> property,
            String serialized
    ) {
        Property rawProperty = property;
        Optional<? extends Comparable> parsed = rawProperty.parse(serialized);
        return parsed.flatMap(value -> setRaw(state, property, value));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyName(Property<?> property, Comparable<?> value) {
        Property rawProperty = property;
        return rawProperty.name(value);
    }


    private record DirectionalValue(Property<?> property, Comparable<?> value) {
    }

    /** Small common view implemented by plan entries without coupling their records. */
    public interface RotatableEntry {
        BlockState state();
    }
}
