package com.mythicrpg.building;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.Optional;

/** Small synchronized ItemStack state for the Architect's Compass. */
public final class ArchitectCompassData {
    public static final int MIN_RADIUS = 1;
    public static final int MAX_RADIUS = 32;
    public static final int DEFAULT_RADIUS = 5;
    private static final String ROOT_KEY = "mythicrpg_architect_compass";
    private static final int FORMAT_VERSION = 1;

    private ArchitectCompassData() {}

    public static State read(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        if (component.getSize() > 16_384) return State.empty();
        NbtCompound custom = component.copyNbt();
        if (!custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) return State.empty();
        NbtCompound root = custom.getCompound(ROOT_KEY);
        if (root.getInt("version") != FORMAT_VERSION) return State.empty();
        int radius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, root.getInt("radius")));
        Plane plane;
        try {
            plane = Plane.valueOf(root.getString("plane").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            plane = Plane.HORIZONTAL;
        }
        if (!root.contains("dimension") || !root.contains("center")) {
            return new State("", null, radius, plane);
        }
        return new State(root.getString("dimension"), BlockPos.fromLong(root.getLong("center")), radius, plane);
    }

    public static void setCenter(ItemStack stack, String dimensionId, BlockPos center) {
        mutate(stack, root -> {
            root.putString("dimension", dimensionId);
            root.putLong("center", center.asLong());
        });
    }

    public static void setConfiguration(
            ItemStack stack,
            String dimensionId,
            BlockPos center,
            int radius,
            Plane plane
    ) {
        int safeRadius = Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius));
        Plane safePlane = plane == null ? Plane.HORIZONTAL : plane;
        mutate(stack, root -> {
            root.putString("dimension", dimensionId == null ? "" : dimensionId);
            root.putLong("center", (center == null ? BlockPos.ORIGIN : center).asLong());
            root.putInt("radius", safeRadius);
            root.putString("plane", safePlane.name().toLowerCase(Locale.ROOT));
        });
    }

    private static void mutate(ItemStack stack, java.util.function.Consumer<NbtCompound> mutator) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, custom -> {
            NbtCompound root = custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)
                    ? custom.getCompound(ROOT_KEY).copy()
                    : new NbtCompound();
            root.putInt("version", FORMAT_VERSION);
            if (!root.contains("radius")) root.putInt("radius", DEFAULT_RADIUS);
            if (!root.contains("plane")) root.putString("plane", Plane.HORIZONTAL.name().toLowerCase(Locale.ROOT));
            mutator.accept(root);
            custom.put(ROOT_KEY, root);
        });
    }

    public enum Plane {
        HORIZONTAL,
        VERTICAL_X,
        VERTICAL_Z;

        public int axisId() {
            return switch (this) {
                case VERTICAL_X -> 0;
                case HORIZONTAL -> 1;
                case VERTICAL_Z -> 2;
            };
        }

        public static Plane fromAxisId(int axisId) {
            return switch (Math.floorMod(axisId, 3)) {
                case 0 -> VERTICAL_X;
                case 1 -> HORIZONTAL;
                default -> VERTICAL_Z;
            };
        }

        public String translationKey() {
            return "message.mythicrpg.architect_compass.plane." + name().toLowerCase(Locale.ROOT);
        }
    }

    public record State(String dimensionId, BlockPos center, int radius, Plane plane) {
        public static State empty() {
            return new State("", null, DEFAULT_RADIUS, Plane.HORIZONTAL);
        }
        public boolean hasCenter() { return center != null && dimensionId != null && !dimensionId.isBlank(); }
    }
}
