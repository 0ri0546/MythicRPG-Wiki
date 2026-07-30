package com.mythicrpg.building;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable six-face material configuration shared by the block, item and Building plans. */
public record BlankBlockAppearance(
        Identifier down,
        Identifier up,
        Identifier north,
        Identifier south,
        Identifier west,
        Identifier east
) {
    public static final BlankBlockAppearance EMPTY = new BlankBlockAppearance(
            null, null, null, null, null, null
    );

    public Identifier material(Direction face) {
        return switch (face) {
            case DOWN -> down;
            case UP -> up;
            case NORTH -> north;
            case SOUTH -> south;
            case WEST -> west;
            case EAST -> east;
        };
    }

    public BlankBlockAppearance with(Direction face, Identifier material) {
        Objects.requireNonNull(face, "face");
        return switch (face) {
            case DOWN -> new BlankBlockAppearance(material, up, north, south, west, east);
            case UP -> new BlankBlockAppearance(down, material, north, south, west, east);
            case NORTH -> new BlankBlockAppearance(down, up, material, south, west, east);
            case SOUTH -> new BlankBlockAppearance(down, up, north, material, west, east);
            case WEST -> new BlankBlockAppearance(down, up, north, south, material, east);
            case EAST -> new BlankBlockAppearance(down, up, north, south, west, material);
        };
    }

    public boolean isEmpty() {
        return configuredFaceCount() == 0;
    }

    public int configuredFaceCount() {
        int count = 0;
        for (Direction face : Direction.values()) {
            if (material(face) != null) {
                count++;
            }
        }
        return count;
    }

    public List<Identifier> configuredMaterials() {
        List<Identifier> materials = new ArrayList<>(6);
        for (Direction face : Direction.values()) {
            Identifier id = material(face);
            if (id != null) {
                materials.add(id);
            }
        }
        return List.copyOf(materials);
    }
}
