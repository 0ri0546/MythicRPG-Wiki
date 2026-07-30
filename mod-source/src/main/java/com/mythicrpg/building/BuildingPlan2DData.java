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
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Compact ItemStack data for the reusable 2D Building Plan. */
public final class BuildingPlan2DData {
    private static final String ROOT_KEY = "mythicrpg_building_plan_2d";
    private static final int FORMAT_VERSION = 2;

    private static final String VERSION_KEY = "version";
    private static final String SELECTION_KEY = "selection";
    private static final String PLAN_KEY = "plan";

    private BuildingPlan2DData() {
    }

    public static Optional<Selection> readSelection(ItemStack stack) {
        NbtCompound root = readRoot(stack);
        int version = root.getInt(VERSION_KEY);
        if ((version != 1 && version != FORMAT_VERSION)
                || !root.contains(SELECTION_KEY, NbtElement.COMPOUND_TYPE)) {
            return Optional.empty();
        }

        NbtCompound selection = root.getCompound(SELECTION_KEY);
        if (!selection.contains("dimension") || !selection.contains("normal_axis")) {
            return Optional.empty();
        }

        long firstLong;
        if (selection.contains("first")) {
            firstLong = selection.getLong("first");
        } else if (selection.contains("pos")) {
            firstLong = selection.getLong("pos");
        } else {
            return Optional.empty();
        }

        String dimension = selection.getString("dimension");
        if (!validDimension(dimension)) return Optional.empty();

        try {
            BlockPos second = selection.contains("second")
                    ? BlockPos.fromLong(selection.getLong("second"))
                    : null;
            return Optional.of(new Selection(
                    dimension,
                    BlockPos.fromLong(firstLong),
                    second,
                    Direction.Axis.valueOf(selection.getString("normal_axis").toUpperCase(Locale.ROOT)),
                    readRotation(selection)
            ));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static void setSelection(ItemStack stack, Selection selection) {
        mutateRoot(stack, root -> {
            NbtCompound data = new NbtCompound();
            data.putString("dimension", selection.dimensionId());
            data.putLong("first", selection.first().asLong());
            if (selection.second() != null) {
                data.putLong("second", selection.second().asLong());
            }
            data.putString("normal_axis", selection.normalAxis().asString());
            writeRotation(data, selection.rotation());
            root.put(SELECTION_KEY, data);
            root.remove(PLAN_KEY);
        });
    }

    public static void clearSelection(ItemStack stack) {
        mutateRoot(stack, root -> root.remove(SELECTION_KEY));
    }

    public static Optional<Plan> readPlan(ItemStack stack) {
        NbtCompound root = readRoot(stack);
        int version = root.getInt(VERSION_KEY);
        if ((version != 1 && version != FORMAT_VERSION)
                || !root.contains(PLAN_KEY, NbtElement.COMPOUND_TYPE)) {
            return Optional.empty();
        }

        NbtCompound planNbt = root.getCompound(PLAN_KEY);
        if (!planNbt.contains("id")
                || !planNbt.contains("normal_axis")
                || !planNbt.contains("size_u")
                || !planNbt.contains("size_v")
                || !planNbt.contains("blocks", NbtElement.LIST_TYPE)) {
            return Optional.empty();
        }

        try {
            UUID id = UUID.fromString(planNbt.getString("id"));
            Direction.Axis normalAxis = Direction.Axis.valueOf(
                    planNbt.getString("normal_axis").toUpperCase(Locale.ROOT)
            );
            int sizeU = planNbt.getInt("size_u");
            int sizeV = planNbt.getInt("size_v");
            if (sizeU <= 0 || sizeV <= 0 || sizeU > 12 || sizeV > 12) {
                return Optional.empty();
            }

            NbtList blocks = planNbt.getList("blocks", NbtElement.COMPOUND_TYPE);
            if (blocks.size() > 144) {
                return Optional.empty();
            }

            List<Entry> entries = new ArrayList<>(blocks.size());
            java.util.Set<Long> occupied = new java.util.HashSet<>();
            for (int index = 0; index < blocks.size(); index++) {
                NbtCompound entryNbt = blocks.getCompound(index);
                Optional<BlockState> state = readState(entryNbt.getCompound("state"));
                if (state.isEmpty()) {
                    return Optional.empty();
                }
                BlankBlockAppearance appearance = BlankBlockAppearance.EMPTY;
                if (entryNbt.contains("blank", NbtElement.COMPOUND_TYPE)) {
                    Optional<BlankBlockAppearance> parsedAppearance = BlankBlockItemData.readAppearance(
                            entryNbt.getCompound("blank")
                    );
                    if (parsedAppearance.isEmpty()) {
                        return Optional.empty();
                    }
                    appearance = parsedAppearance.get();
                }
                BlockPos offset = new BlockPos(
                        entryNbt.getInt("dx"),
                        entryNbt.getInt("dy"),
                        entryNbt.getInt("dz")
                );
                if (Math.abs(offset.getX()) > 11 || Math.abs(offset.getY()) > 11
                        || Math.abs(offset.getZ()) > 11 || !occupied.add(offset.asLong())) {
                    return Optional.empty();
                }
                entries.add(new Entry(
                        offset,
                        state.get(),
                        appearance
                ));
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
                    normalAxis,
                    sizeU,
                    sizeV,
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
            planNbt.putString("normal_axis", plan.normalAxis().asString());
            planNbt.putInt("size_u", plan.sizeU());
            planNbt.putInt("size_v", plan.sizeV());
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
                NbtCompound entryNbt = new NbtCompound();
                entryNbt.putInt("dx", entry.offset().getX());
                entryNbt.putInt("dy", entry.offset().getY());
                entryNbt.putInt("dz", entry.offset().getZ());
                entryNbt.put("state", writeState(entry.state()));
                if (entry.appearance() != null && !entry.appearance().isEmpty()) {
                    entryNbt.put("blank", BlankBlockItemData.writeAppearance(entry.appearance()));
                }
                blocks.add(entryNbt);
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
        NbtComponent component = stack.getOrDefault(
                DataComponentTypes.CUSTOM_DATA,
                NbtComponent.DEFAULT
        );
        if (component.getSize() > 262144) {
            return new NbtCompound();
        }
        NbtCompound custom = component.copyNbt();
        if (!custom.contains(ROOT_KEY, NbtElement.COMPOUND_TYPE)) {
            return new NbtCompound();
        }
        return custom.getCompound(ROOT_KEY);
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

    static NbtCompound writeState(BlockState state) {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("block", Registries.BLOCK.getId(state.getBlock()).toString());

        if (!state.getEntries().isEmpty()) {
            NbtCompound properties = new NbtCompound();
            state.getEntries().forEach((property, value) -> writePropertyUnchecked(properties, property, value));
            nbt.put("properties", properties);
        }
        return nbt;
    }

    static Optional<BlockState> readState(NbtCompound nbt) {
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
        for (String propertyName : properties.getKeys()) {
            Property<?> property = block.getStateManager().getProperty(propertyName);
            if (property == null) {
                return Optional.empty();
            }
            Optional<BlockState> parsed = applyProperty(state, property, properties.getString(propertyName));
            if (parsed.isEmpty()) {
                return Optional.empty();
            }
            state = parsed.get();
        }
        return Optional.of(state);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void writePropertyUnchecked(
            NbtCompound nbt,
            Property<?> property,
            Comparable<?> value
    ) {
        Property rawProperty = property;
        nbt.putString(property.getName(), rawProperty.name(value));
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
            Direction.Axis normalAxis,
            BuildingStructureRotation rotation
    ) {
        public Selection(String dimensionId, BlockPos first, Direction.Axis normalAxis) {
            this(dimensionId, first, null, normalAxis, BuildingStructureRotation.NONE);
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
            Direction.Axis normalAxis,
            int sizeU,
            int sizeV,
            List<Entry> entries,
            BuildingStructureRotation rotation,
            String sourceDimension,
            BlockPos sourceFirst,
            BlockPos sourceSecond
    ) {
        public Plan(UUID id, Direction.Axis normalAxis, int sizeU, int sizeV, List<Entry> entries) {
            this(
                    id,
                    normalAxis,
                    sizeU,
                    sizeV,
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
                    normalAxis,
                    sizeU,
                    sizeV,
                    entries,
                    newRotation,
                    sourceDimension,
                    sourceFirst,
                    sourceSecond
            );
        }

        public Plan withSource(String dimension, BlockPos first, BlockPos second) {
            return new Plan(id, normalAxis, sizeU, sizeV, entries, rotation, dimension, first, second);
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
