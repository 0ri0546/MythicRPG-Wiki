package com.mythicrpg.building;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/** Compact ItemStack/NBT codec for Blank Block six-face configurations. */
public final class BlankBlockItemData {
    private static final String ROOT_KEY = "mythicrpg_blank_block";
    private static final String VERSION_KEY = "version";
    private static final String FACES_KEY = "faces";
    private static final int FORMAT_VERSION = 1;

    private BlankBlockItemData() {
    }

    public static BlankBlockAppearance read(ItemStack stack) {
        return readStrict(stack).orElse(BlankBlockAppearance.EMPTY);
    }

    public static Optional<BlankBlockAppearance> readStrict(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        );
        if (component.getSize() > 32_768) return Optional.empty();
        NbtCompound custom = component.copyNbt();
        if (!custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) {
            return Optional.of(BlankBlockAppearance.EMPTY);
        }
        NbtCompound root = custom.getCompound(ROOT_KEY);
        if (root.getInt(VERSION_KEY) != FORMAT_VERSION
                || !root.contains(FACES_KEY, NbtElement.COMPOUND_TYPE)) {
            return Optional.empty();
        }
        return readAppearance(root.getCompound(FACES_KEY));
    }

    public static void write(ItemStack stack, BlankBlockAppearance appearance) {
        if (appearance == null || !BlankBlockMaterialRegistry.isValid(appearance)) {
            throw new IllegalArgumentException("Invalid Blank Block appearance");
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, custom -> {
            if (appearance.isEmpty()) {
                custom.remove(ROOT_KEY);
                return;
            }
            NbtCompound root = new NbtCompound();
            root.putInt(VERSION_KEY, FORMAT_VERSION);
            root.put(FACES_KEY, writeAppearance(appearance));
            custom.put(ROOT_KEY, root);
        });
    }

    public static NbtCompound writeAppearance(BlankBlockAppearance appearance) {
        NbtCompound nbt = new NbtCompound();
        if (appearance == null) {
            return nbt;
        }
        for (Direction face : Direction.values()) {
            Identifier id = appearance.material(face);
            if (id != null) {
                nbt.putString(key(face), id.toString());
            }
        }
        return nbt;
    }

    public static Optional<BlankBlockAppearance> readAppearance(NbtCompound nbt) {
        if (nbt == null) {
            return Optional.empty();
        }
        BlankBlockAppearance appearance = BlankBlockAppearance.EMPTY;
        for (Direction face : Direction.values()) {
            String key = key(face);
            if (!nbt.contains(key)) {
                continue;
            }
            Identifier id = Identifier.tryParse(nbt.getString(key));
            if (id == null || BlankBlockMaterialRegistry.resolve(id).isEmpty()) {
                return Optional.empty();
            }
            appearance = appearance.with(face, id);
        }
        return Optional.of(appearance);
    }

    private static String key(Direction face) {
        return switch (face) {
            case DOWN -> "d";
            case UP -> "u";
            case NORTH -> "n";
            case SOUTH -> "s";
            case WEST -> "w";
            case EAST -> "e";
        };
    }
}
