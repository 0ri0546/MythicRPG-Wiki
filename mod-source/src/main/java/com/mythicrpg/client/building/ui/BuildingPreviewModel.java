package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.BlankBlockAppearance;
import com.mythicrpg.building.BuildingMiniatureData;
import com.mythicrpg.building.BuildingPlan2DData;
import com.mythicrpg.building.BuildingPlan3DData;
import com.mythicrpg.building.BuildingPlanTransforms;
import com.mythicrpg.building.BuildingStructureRotation;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Immutable, client-friendly structure model shared by 2D and 3D Building previews. */
public final class BuildingPreviewModel {
    public static final BuildingPreviewModel EMPTY = new BuildingPreviewModel(1, 1, 1, List.of());

    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final List<Entry> entries;
    private final Map<BuildingStructureRotation, BuildingPreviewModel> rotatedCache = new HashMap<>();
    private List<Entry> visibleEntries;

    public BuildingPreviewModel(int sizeX, int sizeY, int sizeZ, List<Entry> entries) {
        if (sizeX < 1 || sizeY < 1 || sizeZ < 1) {
            throw new IllegalArgumentException("Preview dimensions must be positive");
        }
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public int sizeX() {
        return sizeX;
    }

    public int sizeY() {
        return sizeY;
    }

    public int sizeZ() {
        return sizeZ;
    }

    public List<Entry> entries() {
        return entries;
    }

    public int blockCount() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Returns one cached transformed model for this exact source/rotation pair.
     * Screen rendering therefore performs no structure allocation every frame.
     */
    public BuildingPreviewModel rotated(BuildingStructureRotation rotation) {
        BuildingStructureRotation safe = rotation == null
                ? BuildingStructureRotation.NONE
                : rotation;
        if (safe.equals(BuildingStructureRotation.NONE)) {
            return this;
        }
        BuildingPreviewModel cached = rotatedCache.get(safe);
        if (cached != null) {
            return cached;
        }

        BuildingStructureRotation.Size size = safe.rotatedSize(sizeX, sizeY, sizeZ);
        List<Entry> rotated = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            BlockPos offset = safe.rotateOffset(
                    new BlockPos(entry.x(), entry.y(), entry.z()),
                    sizeX,
                    sizeY,
                    sizeZ
            );
            rotated.add(new Entry(
                    offset.getX(),
                    offset.getY(),
                    offset.getZ(),
                    BuildingPlanTransforms.rotateState(entry.state(), safe).orElse(entry.state()),
                    BuildingPlanTransforms.rotateAppearance(entry.appearance(), safe)
            ));
        }
        BuildingPreviewModel result = new BuildingPreviewModel(size.x(), size.y(), size.z(), rotated);
        rotatedCache.put(safe, result);
        return result;
    }

    /**
     * Removes only opaque full cubes hidden on all six sides. Partial blocks,
     * transparent blocks and boundary blocks are always preserved.
     */
    public List<Entry> visibleEntries() {
        if (visibleEntries != null) {
            return visibleEntries;
        }
        if (entries.size() < 7) {
            visibleEntries = entries;
            return visibleEntries;
        }

        Map<Integer, Entry> occupied = new HashMap<>(entries.size() * 2);
        for (Entry entry : entries) {
            occupied.put(pack(entry.x(), entry.y(), entry.z()), entry);
        }
        visibleEntries = entries.stream()
                .filter(entry -> !isFullyEnclosedOpaqueCube(entry, occupied))
                .toList();
        return visibleEntries;
    }

    private boolean isFullyEnclosedOpaqueCube(Entry entry, Map<Integer, Entry> occupied) {
        if (!entry.state().isOpaqueFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
            return false;
        }
        for (Direction direction : Direction.values()) {
            int x = entry.x() + direction.getOffsetX();
            int y = entry.y() + direction.getOffsetY();
            int z = entry.z() + direction.getOffsetZ();
            if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) {
                return false;
            }
            Entry neighbor = occupied.get(pack(x, y, z));
            if (neighbor == null
                    || !neighbor.state().isOpaqueFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)) {
                return false;
            }
        }
        return true;
    }

    private int pack(int x, int y, int z) {
        return x + y * sizeX + z * sizeX * sizeY;
    }

    public static BuildingPreviewModel from(BuildingPlan2DData.Plan plan) {
        if (plan == null || plan.entries().isEmpty()) {
            return EMPTY;
        }

        DirectionAxes axes = DirectionAxes.forNormal(plan.normalAxis());
        int shiftU = hasNegativeCoordinate(plan.entries(), axes.u()) ? plan.sizeU() - 1 : 0;
        int shiftV = hasNegativeCoordinate(plan.entries(), axes.v()) ? plan.sizeV() - 1 : 0;
        int sizeX = axisSize(Direction.Axis.X, plan.normalAxis(), axes, plan.sizeU(), plan.sizeV());
        int sizeY = axisSize(Direction.Axis.Y, plan.normalAxis(), axes, plan.sizeU(), plan.sizeV());
        int sizeZ = axisSize(Direction.Axis.Z, plan.normalAxis(), axes, plan.sizeU(), plan.sizeV());

        List<Entry> entries = plan.entries().stream()
                .map(entry -> {
                    int u = coordinate(entry.offset(), axes.u()) + shiftU;
                    int v = coordinate(entry.offset(), axes.v()) + shiftV;
                    return new Entry(
                            coordinateForAxis(Direction.Axis.X, plan.normalAxis(), axes, u, v),
                            coordinateForAxis(Direction.Axis.Y, plan.normalAxis(), axes, u, v),
                            coordinateForAxis(Direction.Axis.Z, plan.normalAxis(), axes, u, v),
                            entry.state(),
                            entry.appearance()
                    );
                })
                .toList();
        return new BuildingPreviewModel(sizeX, sizeY, sizeZ, entries);
    }

    public static BuildingPreviewModel from(BuildingPlan3DData.Plan plan) {
        if (plan == null || plan.entries().isEmpty()) {
            return EMPTY;
        }

        int shiftX = plan.entries().stream().anyMatch(entry -> entry.offset().getX() < 0)
                ? plan.sizeX() - 1 : 0;
        int shiftY = plan.entries().stream().anyMatch(entry -> entry.offset().getY() < 0)
                ? plan.sizeY() - 1 : 0;
        int shiftZ = plan.entries().stream().anyMatch(entry -> entry.offset().getZ() < 0)
                ? plan.sizeZ() - 1 : 0;
        List<Entry> entries = plan.entries().stream()
                .map(entry -> new Entry(
                        entry.offset().getX() + shiftX,
                        entry.offset().getY() + shiftY,
                        entry.offset().getZ() + shiftZ,
                        entry.state(),
                        entry.appearance()
                ))
                .toList();
        return new BuildingPreviewModel(plan.sizeX(), plan.sizeY(), plan.sizeZ(), entries);
    }

    public static BuildingPreviewModel from(BuildingMiniatureData.Project project) {
        if (project == null || project.entries().isEmpty()) {
            return EMPTY;
        }
        List<Entry> entries = project.entries().stream()
                .map(entry -> new Entry(
                        entry.x(),
                        entry.y(),
                        entry.z(),
                        entry.state(),
                        BlankBlockAppearance.EMPTY
                ))
                .toList();
        return new BuildingPreviewModel(
                project.sizeX(),
                project.sizeY(),
                project.sizeZ(),
                entries
        );
    }

    private static boolean hasNegativeCoordinate(
            List<BuildingPlan2DData.Entry> entries,
            Direction.Axis axis
    ) {
        return entries.stream().anyMatch(entry -> coordinate(entry.offset(), axis) < 0);
    }

    private static int axisSize(
            Direction.Axis target,
            Direction.Axis normal,
            DirectionAxes axes,
            int sizeU,
            int sizeV
    ) {
        if (target == normal) {
            return 1;
        }
        return target == axes.u() ? sizeU : sizeV;
    }

    private static int coordinateForAxis(
            Direction.Axis target,
            Direction.Axis normal,
            DirectionAxes axes,
            int u,
            int v
    ) {
        if (target == normal) {
            return 0;
        }
        return target == axes.u() ? u : v;
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    public record Entry(
            int x,
            int y,
            int z,
            BlockState state,
            BlankBlockAppearance appearance
    ) implements BuildingPlanTransforms.RotatableEntry {
        public Entry {
            if (state == null) {
                throw new IllegalArgumentException("Preview block state cannot be null");
            }
            appearance = appearance == null ? BlankBlockAppearance.EMPTY : appearance;
        }
    }

    private record DirectionAxes(Direction.Axis u, Direction.Axis v) {
        private static DirectionAxes forNormal(Direction.Axis normal) {
            return switch (normal) {
                case X -> new DirectionAxes(Direction.Axis.Y, Direction.Axis.Z);
                case Y -> new DirectionAxes(Direction.Axis.X, Direction.Axis.Z);
                case Z -> new DirectionAxes(Direction.Axis.X, Direction.Axis.Y);
            };
        }
    }
}
