package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.*;
import com.mythicrpg.core.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Bounded client-side capture used only for responsive screen previews. */
public final class BuildingClientPlanCapture {
    private BuildingClientPlanCapture() {
    }

    public static Result capture2D(
            ClientWorld world,
            String dimensionId,
            BlockPos first,
            BlockPos second,
            Direction.Axis normal,
            int maxSize
    ) {
        if (world == null || first == null || second == null || normal == null) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.invalid_coordinates");
        }
        if (!world.getRegistryKey().getValue().toString().equals(dimensionId)) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.wrong_dimension");
        }
        if (coordinate(first, normal) != coordinate(second, normal)) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.not_flat");
        }

        Direction.Axis[] tangents = tangentAxes(normal);
        int deltaU = coordinate(second, tangents[0]) - coordinate(first, tangents[0]);
        int deltaV = coordinate(second, tangents[1]) - coordinate(first, tangents[1]);
        int sizeU = Math.abs(deltaU) + 1;
        int sizeV = Math.abs(deltaV) + 1;
        if (sizeU > maxSize || sizeV > maxSize) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.too_large");
        }

        int stepU = deltaU < 0 ? -1 : 1;
        int stepV = deltaV < 0 ? -1 : 1;
        List<BuildingPlan2DData.Entry> entries = new ArrayList<>(sizeU * sizeV);
        for (int u = 0; u < sizeU; u++) {
            for (int v = 0; v < sizeV; v++) {
                BlockPos offset = offset(tangents[0], u * stepU)
                        .add(offset(tangents[1], v * stepV));
                BlockPos sourcePos = first.add(offset);
                CaptureBlock captured = captureBlock(world, sourcePos);
                if (!captured.valid()) {
                    return Result.invalid(captured.messageKey());
                }
                if (captured.state() != null && !captured.state().isAir()) {
                    entries.add(new BuildingPlan2DData.Entry(
                            offset,
                            captured.state(),
                            captured.appearance()
                    ));
                }
            }
        }
        if (entries.isEmpty()) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.empty");
        }
        BuildingPlan2DData.Plan plan = new BuildingPlan2DData.Plan(
                UUID.randomUUID(),
                normal,
                sizeU,
                sizeV,
                List.copyOf(entries)
        );
        return Result.valid(BuildingPreviewModel.from(plan));
    }

    public static Result capture3D(
            ClientWorld world,
            String dimensionId,
            BlockPos first,
            BlockPos second,
            int maxSize
    ) {
        if (world == null || first == null || second == null) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.invalid_coordinates");
        }
        if (!world.getRegistryKey().getValue().toString().equals(dimensionId)) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.wrong_dimension");
        }

        int deltaX = second.getX() - first.getX();
        int deltaY = second.getY() - first.getY();
        int deltaZ = second.getZ() - first.getZ();
        int sizeX = Math.abs(deltaX) + 1;
        int sizeY = Math.abs(deltaY) + 1;
        int sizeZ = Math.abs(deltaZ) + 1;
        if (sizeX > maxSize || sizeY > maxSize || sizeZ > maxSize) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.too_large");
        }

        int stepX = deltaX < 0 ? -1 : 1;
        int stepY = deltaY < 0 ? -1 : 1;
        int stepZ = deltaZ < 0 ? -1 : 1;
        List<BuildingPlan3DData.Entry> entries = new ArrayList<>(sizeX * sizeY * sizeZ);
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos offset = new BlockPos(x * stepX, y * stepY, z * stepZ);
                    CaptureBlock captured = captureBlock(world, first.add(offset));
                    if (!captured.valid()) {
                        return Result.invalid(captured.messageKey());
                    }
                    if (captured.state() != null && !captured.state().isAir()) {
                        entries.add(new BuildingPlan3DData.Entry(
                                offset,
                                captured.state(),
                                captured.appearance()
                        ));
                    }
                }
            }
        }
        if (entries.isEmpty()) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.empty");
        }
        BuildingPlan3DData.Plan plan = new BuildingPlan3DData.Plan(
                UUID.randomUUID(),
                sizeX,
                sizeY,
                sizeZ,
                List.copyOf(entries)
        );
        return Result.valid(BuildingPreviewModel.from(plan));
    }

    public static Result captureMiniature(
            ClientWorld world,
            String dimensionId,
            BlockPos first,
            BlockPos second,
            int maxSize
    ) {
        if (world == null || first == null || second == null) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.invalid_coordinates");
        }
        if (!world.getRegistryKey().getValue().toString().equals(dimensionId)) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.wrong_dimension");
        }

        BlockPos min = new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
        );
        int sizeX = max.getX() - min.getX() + 1;
        int sizeY = max.getY() - min.getY() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;
        if (sizeX > maxSize || sizeY > maxSize || sizeZ > maxSize) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.too_large");
        }

        List<BuildingPreviewModel.Entry> entries = new ArrayList<>(sizeX * sizeY * sizeZ);
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!world.isChunkLoaded(pos)) {
                        return Result.invalid("screen.mythicrpg.building_plan_ui.unloaded");
                    }
                    if (!world.isInBuildLimit(pos) || !world.getWorldBorder().contains(pos)) {
                        return Result.invalid("screen.mythicrpg.building_plan_ui.outside_world");
                    }

                    BlockState state = world.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    if (world.getBlockEntity(pos) != null
                            || !BuildingMiniatureData.isSupportedState(state)) {
                        return Result.invalid("screen.mythicrpg.building_miniature_ui.unsupported_block");
                    }
                    entries.add(new BuildingPreviewModel.Entry(
                            x - min.getX(),
                            y - min.getY(),
                            z - min.getZ(),
                            state,
                            BlankBlockAppearance.EMPTY
                    ));
                }
            }
        }
        if (entries.isEmpty()) {
            return Result.invalid("screen.mythicrpg.building_plan_ui.empty");
        }
        return Result.valid(new BuildingPreviewModel(sizeX, sizeY, sizeZ, entries));
    }

    private static CaptureBlock captureBlock(ClientWorld world, BlockPos pos) {
        if (!world.isChunkLoaded(pos)) {
            return CaptureBlock.invalid("screen.mythicrpg.building_plan_ui.unloaded");
        }
        if (!world.isInBuildLimit(pos) || !world.getWorldBorder().contains(pos)) {
            return CaptureBlock.invalid("screen.mythicrpg.building_plan_ui.outside_world");
        }

        BlockState state = world.getBlockState(pos);
        if (state.isAir()) {
            return CaptureBlock.valid(state, BlankBlockAppearance.EMPTY);
        }

        BlankBlockAppearance appearance = BlankBlockAppearance.EMPTY;
        var blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null) {
            if (state.isOf(ModBlocks.BLANK_BLOCK) && blockEntity instanceof BlankBlockEntity blank) {
                appearance = blank.appearance();
            } else {
                return CaptureBlock.invalid("screen.mythicrpg.building_plan_ui.unsupported_block");
            }
        }

        if (!state.getFluidState().isEmpty()
                || !BuildingBlockCatalog.isEligible(state.getBlock())
                || state.getBlock().asItem() == Items.AIR
                || !BlankBlockMaterialRegistry.isValid(appearance)) {
            return CaptureBlock.invalid("screen.mythicrpg.building_plan_ui.unsupported_block");
        }
        return CaptureBlock.valid(state, appearance);
    }

    private static Direction.Axis[] tangentAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static BlockPos offset(Direction.Axis axis, int amount) {
        return switch (axis) {
            case X -> new BlockPos(amount, 0, 0);
            case Y -> new BlockPos(0, amount, 0);
            case Z -> new BlockPos(0, 0, amount);
        };
    }

    public record Result(boolean valid, BuildingPreviewModel model, String messageKey) {
        public static Result valid(BuildingPreviewModel model) {
            return new Result(true, model, "");
        }

        public static Result invalid(String messageKey) {
            return new Result(false, BuildingPreviewModel.EMPTY, messageKey);
        }
    }

    private record CaptureBlock(
            boolean valid,
            BlockState state,
            BlankBlockAppearance appearance,
            String messageKey
    ) {
        private static CaptureBlock valid(BlockState state, BlankBlockAppearance appearance) {
            return new CaptureBlock(true, state, appearance, "");
        }

        private static CaptureBlock invalid(String key) {
            return new CaptureBlock(false, null, BlankBlockAppearance.EMPTY, key);
        }
    }
}
