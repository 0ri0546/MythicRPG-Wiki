package com.mythicrpg.building;

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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Compact ItemStack data for the reusable 8x8x8 Building Plan. */
public final class BuildingPlan3DData {
    private static final String ROOT_KEY = "mythicrpg_building_plan_3d";
    private static final int FORMAT_VERSION = 2;
    private static final String VERSION_KEY = "version";
    private static final String SELECTION_KEY = "selection";
    private static final String PLAN_KEY = "plan";

    private BuildingPlan3DData() {
    }

    public static Optional<Selection> readSelection(ItemStack stack) {
        NbtCompound root = readRoot(stack);
        int version = root.getInt(VERSION_KEY);
        if ((version != 1 && version != FORMAT_VERSION)
                || !root.contains(SELECTION_KEY, NbtElement.COMPOUND_TYPE)) {
            return Optional.empty();
        }
        NbtCompound data = root.getCompound(SELECTION_KEY);
        if (!data.contains("dimension")) {
            return Optional.empty();
        }
        long firstLong;
        if (data.contains("first")) {
            firstLong = data.getLong("first");
        } else if (data.contains("pos")) {
            firstLong = data.getLong("pos");
        } else {
            return Optional.empty();
        }
        String dimension = data.getString("dimension");
        if (!validDimension(dimension)) return Optional.empty();
        BlockPos second = data.contains("second")
                ? BlockPos.fromLong(data.getLong("second"))
                : null;
        return Optional.of(new Selection(
                dimension,
                BlockPos.fromLong(firstLong),
                second,
                readRotation(data)
        ));
    }

    public static void setSelection(ItemStack stack, Selection selection) {
        mutateRoot(stack, root -> {
            NbtCompound data = new NbtCompound();
            data.putString("dimension", selection.dimensionId());
            data.putLong("first", selection.first().asLong());
            if (selection.second() != null) {
                data.putLong("second", selection.second().asLong());
            }
            writeRotation(data, selection.rotation());
            root.put(SELECTION_KEY, data);
            root.remove(PLAN_KEY);
        });
    }

    public static Optional<Plan> readPlan(ItemStack stack) {
        NbtCompound root = readRoot(stack);
        int version = root.getInt(VERSION_KEY);
        if ((version != 1 && version != FORMAT_VERSION)
                || !root.contains(PLAN_KEY, NbtElement.COMPOUND_TYPE)) {
            return Optional.empty();
        }
        NbtCompound planNbt = root.getCompound(PLAN_KEY);
        if (!planNbt.contains("id") || !planNbt.contains("size_x")
                || !planNbt.contains("size_y") || !planNbt.contains("size_z")
                || !planNbt.contains("blocks", NbtElement.LIST_TYPE)) {
            return Optional.empty();
        }
        try {
            UUID id = UUID.fromString(planNbt.getString("id"));
            int sizeX = planNbt.getInt("size_x");
            int sizeY = planNbt.getInt("size_y");
            int sizeZ = planNbt.getInt("size_z");
            if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0 || sizeX > 8 || sizeY > 8 || sizeZ > 8) {
                return Optional.empty();
            }
            NbtList blocks = planNbt.getList("blocks", NbtElement.COMPOUND_TYPE);
            if (blocks.isEmpty() || blocks.size() > 512) {
                return Optional.empty();
            }
            List<Entry> entries = new ArrayList<>(blocks.size());
            java.util.Set<Long> occupied = new java.util.HashSet<>();
            for (int i = 0; i < blocks.size(); i++) {
                NbtCompound entry = blocks.getCompound(i);
                if (!entry.contains("state", NbtElement.COMPOUND_TYPE)) {
                    return Optional.empty();
                }
                Optional<BlockState> state = readState(entry.getCompound("state"));
                if (state.isEmpty()) {
                    return Optional.empty();
                }
                BlankBlockAppearance appearance = BlankBlockAppearance.EMPTY;
                if (entry.contains("blank", NbtElement.COMPOUND_TYPE)) {
                    Optional<BlankBlockAppearance> parsedAppearance = BlankBlockItemData.readAppearance(
                            entry.getCompound("blank")
                    );
                    if (parsedAppearance.isEmpty()) {
                        return Optional.empty();
                    }
                    appearance = parsedAppearance.get();
                }
                BlockPos offset = new BlockPos(
                        entry.getInt("dx"),
                        entry.getInt("dy"),
                        entry.getInt("dz")
                );
                if (Math.abs(offset.getX()) > 7 || Math.abs(offset.getY()) > 7
                        || Math.abs(offset.getZ()) > 7 || !occupied.add(offset.asLong())) {
                    return Optional.empty();
                }
                entries.add(new Entry(offset, state.get(), appearance));
            }
            String sourceDimension = planNbt.contains("source_dimension")
                    ? planNbt.getString("source_dimension")
                    : "";
            if (!sourceDimension.isBlank() && !validDimension(sourceDimension)) {
                return Optional.empty();
            }
            BlockPos sourceFirst = planNbt.contains("source_first")
                    ? BlockPos.fromLong(planNbt.getLong("source_first"))
                    : null;
            BlockPos sourceSecond = planNbt.contains("source_second")
                    ? BlockPos.fromLong(planNbt.getLong("source_second"))
                    : null;
            return Optional.of(new Plan(
                    id,
                    sizeX,
                    sizeY,
                    sizeZ,
                    List.copyOf(entries),
                    readRotation(planNbt),
                    sourceDimension,
                    sourceFirst,
                    sourceSecond
            ));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static void writePlan(ItemStack stack, Plan plan) {
        mutateRoot(stack, root -> {
            NbtCompound planNbt = new NbtCompound();
            planNbt.putString("id", plan.id().toString());
            planNbt.putInt("size_x", plan.sizeX());
            planNbt.putInt("size_y", plan.sizeY());
            planNbt.putInt("size_z", plan.sizeZ());
            writeRotation(planNbt, plan.rotation());
            if (plan.sourceDimension() != null && !plan.sourceDimension().isBlank()) {
                planNbt.putString("source_dimension", plan.sourceDimension());
            }
            if (plan.sourceFirst() != null) {
                planNbt.putLong("source_first", plan.sourceFirst().asLong());
            }
            if (plan.sourceSecond() != null) {
                planNbt.putLong("source_second", plan.sourceSecond().asLong());
            }
            NbtList blocks = new NbtList();
            for (Entry entry : plan.entries()) {
                NbtCompound data = new NbtCompound();
                data.putInt("dx", entry.offset().getX());
                data.putInt("dy", entry.offset().getY());
                data.putInt("dz", entry.offset().getZ());
                data.put("state", writeState(entry.state()));
                if (entry.appearance() != null && !entry.appearance().isEmpty()) {
                    data.put("blank", BlankBlockItemData.writeAppearance(entry.appearance()));
                }
                blocks.add(data);
            }
            planNbt.put("blocks", blocks);
            root.put(PLAN_KEY, planNbt);
            root.remove(SELECTION_KEY);
        });
    }

    public static void clearAll(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.remove(ROOT_KEY));
    }

    private static boolean validDimension(String value) {
        return value != null && !value.isBlank() && value.length() <= 256
                && Identifier.tryParse(value) != null;
    }

    private static NbtCompound readRoot(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        if (component.getSize() > 524288) return new NbtCompound();
        NbtCompound custom = component.copyNbt();
        return custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)
                ? custom.getCompound(ROOT_KEY)
                : new NbtCompound();
    }

    private static void mutateRoot(ItemStack stack, java.util.function.Consumer<NbtCompound> mutator) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, custom -> {
            NbtCompound root = custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)
                    ? custom.getCompound(ROOT_KEY).copy()
                    : new NbtCompound();
            root.putInt(VERSION_KEY, FORMAT_VERSION);
            mutator.accept(root);
            if (root.contains(SELECTION_KEY) || root.contains(PLAN_KEY)) {
                custom.put(ROOT_KEY, root);
            } else {
                custom.remove(ROOT_KEY);
            }
        });
    }

    private static void writeRotation(NbtCompound nbt, BuildingStructureRotation rotation) {
        BuildingStructureRotation safe = rotation == null ? BuildingStructureRotation.NONE : rotation;
        nbt.putInt("rotation_x", safe.xQuarterTurns());
        nbt.putInt("rotation_y", safe.yQuarterTurns());
        nbt.putInt("rotation_z", safe.zQuarterTurns());
    }

    private static BuildingStructureRotation readRotation(NbtCompound nbt) {
        return new BuildingStructureRotation(
                nbt.getInt("rotation_x"),
                nbt.getInt("rotation_y"),
                nbt.getInt("rotation_z")
        );
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
        if (id == null) {
            return Optional.empty();
        }
        Block block = Registries.BLOCK.get(id);
        if (block == Blocks.AIR && !id.equals(Registries.BLOCK.getId(Blocks.AIR))) {
            return Optional.empty();
        }
        BlockState state = block.getDefaultState();
        if (!nbt.contains("properties", NbtElement.COMPOUND_TYPE)) {
            return Optional.of(state);
        }
        NbtCompound properties = nbt.getCompound("properties");
        for (String name : properties.getKeys()) {
            Property<?> property = block.getStateManager().getProperty(name);
            if (property == null) {
                return Optional.empty();
            }
            Optional<BlockState> parsed = applyProperty(state, property, properties.getString(name));
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
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
            BlockState state,
            Property<T> property,
            String serializedValue
    ) {
        return property.parse(serializedValue).map(value -> state.with(property, value));
    }

    public record Selection(
            String dimensionId,
            BlockPos first,
            BlockPos second,
            BuildingStructureRotation rotation
    ) {
        public Selection(String dimensionId, BlockPos first) {
            this(dimensionId, first, null, BuildingStructureRotation.NONE);
        }

        public BlockPos pos() {
            return first;
        }

        public boolean complete() {
            return second != null;
        }
    }

    public record Plan(
            UUID id,
            int sizeX,
            int sizeY,
            int sizeZ,
            List<Entry> entries,
            BuildingStructureRotation rotation,
            String sourceDimension,
            BlockPos sourceFirst,
            BlockPos sourceSecond
    ) {
        public Plan(UUID id, int sizeX, int sizeY, int sizeZ, List<Entry> entries) {
            this(
                    id,
                    sizeX,
                    sizeY,
                    sizeZ,
                    entries,
                    BuildingStructureRotation.NONE,
                    "",
                    null,
                    null
            );
        }

        public int blockCount() {
            return entries.size();
        }

        public Plan withRotation(BuildingStructureRotation newRotation) {
            return new Plan(
                    id,
                    sizeX,
                    sizeY,
                    sizeZ,
                    entries,
                    newRotation,
                    sourceDimension,
                    sourceFirst,
                    sourceSecond
            );
        }

        public Plan withSource(String dimension, BlockPos first, BlockPos second) {
            return new Plan(id, sizeX, sizeY, sizeZ, entries, rotation, dimension, first, second);
        }
    }

    public record Entry(
            BlockPos offset,
            BlockState state,
            BlankBlockAppearance appearance
    ) implements BuildingPlanTransforms.RotatableEntry {
        public Entry(BlockPos offset, BlockState state) {
            this(offset, state, BlankBlockAppearance.EMPTY);
        }
    }
}
