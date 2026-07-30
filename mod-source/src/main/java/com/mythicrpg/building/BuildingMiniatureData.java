package com.mythicrpg.building;

import com.mythicrpg.core.ModBlocks;
import com.mythicrpg.core.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.UUID;

/** Versioned compact codec for ongoing and finished 5x5x5 miniature projects. */
public final class BuildingMiniatureData {
    private static final String ROOT_KEY = "mythicrpg_building_miniature";
    private static final int VERSION = 1;

    private BuildingMiniatureData() {}

    public static Optional<Selection> readSelection(ItemStack stack) {
        if (!stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT)) return Optional.empty();
        NbtCompound root = readRoot(stack);
        if (root.getInt("version") != VERSION || !"ongoing".equals(root.getString("status"))) {
            return Optional.empty();
        }
        if (!root.contains("first") || !root.contains("dimension")) return Optional.empty();
        String dimension = root.getString("dimension");
        if (!validDimension(dimension)) return Optional.empty();
        BlockPos first = BlockPos.fromLong(root.getLong("first"));
        BlockPos second = root.contains("second") ? BlockPos.fromLong(root.getLong("second")) : null;
        BuildingStructureRotation rotation = new BuildingStructureRotation(
                root.getInt("rotation_x"),
                root.getInt("rotation_y"),
                root.getInt("rotation_z")
        );
        return Optional.of(new Selection(dimension, first, second, rotation));
    }

    public static void writeFirst(ItemStack stack, String dimension, BlockPos first) {
        mutate(stack, root -> {
            root.putString("status", "ongoing");
            root.putString("dimension", dimension);
            root.putLong("first", first.asLong());
            root.remove("second");
            root.putInt("rotation_x", 0);
            root.putInt("rotation_y", 0);
            root.putInt("rotation_z", 0);
            root.remove("project");
        });
    }

    public static void writeSelection(ItemStack stack, Selection selection) {
        mutate(stack, root -> {
            root.putString("status", "ongoing");
            root.putString("dimension", selection.dimensionId());
            root.putLong("first", selection.first().asLong());
            if (selection.second() != null) root.putLong("second", selection.second().asLong());
            else root.remove("second");
            BuildingStructureRotation rotation = selection.rotation() == null
                    ? BuildingStructureRotation.NONE
                    : selection.rotation();
            root.putInt("rotation_x", rotation.xQuarterTurns());
            root.putInt("rotation_y", rotation.yQuarterTurns());
            root.putInt("rotation_z", rotation.zQuarterTurns());
            root.remove("project");
        });
    }

    public static Optional<Project> readProject(ItemStack stack) {
        if (!stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT)) return Optional.empty();
        NbtCompound root = readRoot(stack);
        if (root.getInt("version") != VERSION
                || !"finished".equals(root.getString("status"))
                || !root.contains("project", NbtElement.COMPOUND_TYPE)) return Optional.empty();
        NbtCompound project = root.getCompound("project");
        try {
            UUID id = UUID.fromString(project.getString("id"));
            UUID author = UUID.fromString(project.getString("author"));
            String authorName = project.getString("author_name");
            if (authorName.length() > 64) return Optional.empty();
            int sizeX = project.getInt("size_x");
            int sizeY = project.getInt("size_y");
            int sizeZ = project.getInt("size_z");
            BuildingStructureRotation rotation = new BuildingStructureRotation(
                    project.getInt("rotation_x"),
                    project.getInt("rotation_y"),
                    project.getInt("rotation_z")
            );
            if (sizeX < 1 || sizeY < 1 || sizeZ < 1 || sizeX > 5 || sizeY > 5 || sizeZ > 5) {
                return Optional.empty();
            }
            NbtList list = project.getList("blocks", NbtElement.COMPOUND_TYPE);
            if (list.isEmpty() || list.size() > 125) return Optional.empty();
            List<Entry> entries = new ArrayList<>(list.size());
            Set<Integer> occupied = new HashSet<>();
            for (int i = 0; i < list.size(); i++) {
                NbtCompound tag = list.getCompound(i);
                Optional<BlockState> state = readState(tag.getCompound("state"));
                if (state.isEmpty() || !isSupportedState(state.get())) {
                    return Optional.empty();
                }
                int x = tag.getInt("x");
                int y = tag.getInt("y");
                int z = tag.getInt("z");
                if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
                    return Optional.empty();
                }
                int packed = x + y * 5 + z * 25;
                if (!occupied.add(packed)) return Optional.empty();
                entries.add(new Entry(x, y, z, state.get()));
            }
            return Optional.of(new Project(
                    id,
                    author,
                    authorName,
                    sizeX,
                    sizeY,
                    sizeZ,
                    List.copyOf(entries),
                    rotation
            ));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    /** Rotation around the miniature's local Z axis, persisted inside the project item. */
    public static float readRollZ(ItemStack stack) {
        if (!stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT)) return 0.0F;
        NbtCompound root = readRoot(stack);
        if (root.getInt("version") != VERSION || !root.contains("roll_z", NbtElement.NUMBER_TYPE)) {
            return 0.0F;
        }
        return normalizeRollZ(root.getFloat("roll_z"));
    }

    public static void writeRollZ(ItemStack stack, float degrees) {
        if (!stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT)) return;
        mutate(stack, root -> root.putFloat("roll_z", normalizeRollZ(degrees)));
    }

    public static float normalizeRollZ(float degrees) {
        if (!Float.isFinite(degrees)) return 0.0F;
        float normalized = degrees % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }

    public static void writeProject(ItemStack stack, Project project) {
        mutate(stack, root -> {
            root.putString("status", "finished");
            NbtCompound data = new NbtCompound();
            data.putString("id", project.id().toString());
            data.putString("author", project.author().toString());
            data.putString("author_name", project.authorName());
            data.putInt("size_x", project.sizeX());
            data.putInt("size_y", project.sizeY());
            data.putInt("size_z", project.sizeZ());
            data.putInt("rotation_x", project.rotation().xQuarterTurns());
            data.putInt("rotation_y", project.rotation().yQuarterTurns());
            data.putInt("rotation_z", project.rotation().zQuarterTurns());
            NbtList blocks = new NbtList();
            for (Entry entry : project.entries()) {
                NbtCompound tag = new NbtCompound();
                tag.putInt("x", entry.x());
                tag.putInt("y", entry.y());
                tag.putInt("z", entry.z());
                tag.put("state", writeState(entry.state()));
                blocks.add(tag);
            }
            data.put("blocks", blocks);
            root.put("project", data);
            root.remove("dimension");
            root.remove("first");
            root.remove("second");
            root.remove("rotation_x");
            root.remove("rotation_y");
            root.remove("rotation_z");
        });
    }

    public static void clear(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.remove(ROOT_KEY));
    }

    /** Miniatures store only BlockState data: no inventories, custom face data, or fluids. */
    public static boolean isSupportedState(BlockState state) {
        return state != null
                && !state.isAir()
                && !state.hasBlockEntity()
                && !state.isOf(ModBlocks.BLANK_BLOCK)
                && state.getFluidState().isEmpty();
    }

    private static boolean validDimension(String value) {
        return value != null && !value.isBlank() && value.length() <= 256
                && Identifier.tryParse(value) != null;
    }

    private static NbtCompound readRoot(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        if (component.getSize() > 262144) return new NbtCompound();
        NbtCompound custom = component.copyNbt();
        return custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)
                ? custom.getCompound(ROOT_KEY)
                : new NbtCompound();
    }

    private static void mutate(ItemStack stack, java.util.function.Consumer<NbtCompound> consumer) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, custom -> {
            NbtCompound root = custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)
                    ? custom.getCompound(ROOT_KEY).copy()
                    : new NbtCompound();
            root.putInt("version", VERSION);
            consumer.accept(root);
            custom.put(ROOT_KEY, root);
        });
    }

    private static NbtCompound writeState(BlockState state) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("block", Registries.BLOCK.getId(state.getBlock()).toString());
        if (!state.getEntries().isEmpty()) {
            NbtCompound properties = new NbtCompound();
            state.getEntries().forEach((property, value) -> writePropertyUnchecked(properties, property, value));
            nbt.put("properties", properties);
        }
        return nbt;
    }

    private static Optional<BlockState> readState(NbtCompound nbt) {
        Identifier id = Identifier.tryParse(nbt.getString("block"));
        if (id == null) return Optional.empty();
        Block block = Registries.BLOCK.get(id);
        if (block == Blocks.AIR && !id.equals(Registries.BLOCK.getId(Blocks.AIR))) return Optional.empty();
        BlockState state = block.getDefaultState();
        if (!nbt.contains("properties", NbtElement.COMPOUND_TYPE)) return Optional.of(state);
        NbtCompound properties = nbt.getCompound("properties");
        for (String name : properties.getKeys()) {
            Property<?> property = block.getStateManager().getProperty(name);
            if (property == null) return Optional.empty();
            Optional<BlockState> parsed = applyProperty(state, property, properties.getString(name));
            if (parsed.isEmpty()) return Optional.empty();
            state = parsed.get();
        }
        return Optional.of(state);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void writePropertyUnchecked(NbtCompound nbt, Property<?> property, Comparable<?> value) {
        Property raw = property;
        nbt.putString(property.getName(), raw.name(value));
    }

    private static <T extends Comparable<T>> Optional<BlockState> applyProperty(
            BlockState state, Property<T> property, String serializedValue) {
        return property.parse(serializedValue).map(value -> state.with(property, value));
    }

    public record Selection(
            String dimensionId,
            BlockPos first,
            BlockPos second,
            BuildingStructureRotation rotation
    ) {
        public Selection {
            rotation = rotation == null ? BuildingStructureRotation.NONE : rotation;
        }

        public Selection(String dimensionId, BlockPos first, BlockPos second) {
            this(dimensionId, first, second, BuildingStructureRotation.NONE);
        }

        public boolean complete() { return second != null; }
        public BlockPos min() { return second == null ? first : new BlockPos(
                Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()), Math.min(first.getZ(), second.getZ())); }
        public BlockPos max() { return second == null ? first : new BlockPos(
                Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()), Math.max(first.getZ(), second.getZ())); }
    }

    public record Project(
            UUID id,
            UUID author,
            String authorName,
            int sizeX,
            int sizeY,
            int sizeZ,
            List<Entry> entries,
            BuildingStructureRotation rotation
    ) {
        public Project {
            rotation = rotation == null ? BuildingStructureRotation.NONE : rotation;
        }

        public Project(
                UUID id,
                UUID author,
                String authorName,
                int sizeX,
                int sizeY,
                int sizeZ,
                List<Entry> entries
        ) {
            this(id, author, authorName, sizeX, sizeY, sizeZ, entries, BuildingStructureRotation.NONE);
        }

        public int blockCount() { return entries.size(); }

        public BuildingStructureRotation.Size rotatedSize() {
            return rotation.rotatedSize(sizeX, sizeY, sizeZ);
        }
    }

    public record Entry(int x, int y, int z, BlockState state)
            implements BuildingPlanTransforms.RotatableEntry {}
}
