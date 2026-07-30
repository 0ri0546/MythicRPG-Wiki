package com.mythicrpg.building;

import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/** Server authority for Building structure configuration screens. */
public final class BuildingPlanUiManager {
    private BuildingPlanUiManager() {
    }

    public static void open2D(ServerPlayerEntity player, Hand hand) {
        if (player == null || !BuildingPlan2DManager.canUse(player)) {
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModItems.BUILDING_PLAN_2D)) {
            return;
        }
        BuildingPlan2DManager.clearPreviewOnly(player);
        BuildingPlan3DManager.clearPreview(player, false);

        Optional<BuildingPlan2DData.Plan> plan = BuildingPlan2DData.readPlan(stack);
        if (plan.isPresent()) {
            BuildingUiSessionManager.open(
                    player,
                    BuildingUiSessionManager.Tool.PLAN_2D,
                    hand,
                    stack,
                    Long.MIN_VALUE
            );
            send2DState(player, hand, plan.get(), "", false, true);
            return;
        }
        Optional<BuildingPlan2DData.Selection> selection = BuildingPlan2DData.readSelection(stack);
        if (selection.isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_ui.select_first")
                            .formatted(Formatting.YELLOW),
                    true
            );
            return;
        }
        BuildingUiSessionManager.open(
                player,
                BuildingUiSessionManager.Tool.PLAN_2D,
                hand,
                stack,
                Long.MIN_VALUE
        );
        send2DState(player, hand, selection.get(), "", false, true);
    }

    public static void open3D(ServerPlayerEntity player, Hand hand) {
        if (player == null || !BuildingPlan3DManager.canUse(player)) {
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModItems.BUILDING_PLAN_3D)) {
            return;
        }
        BuildingPlan3DManager.clearPreview(player, false);
        BuildingPlan2DManager.clearPreviewOnly(player);

        Optional<BuildingPlan3DData.Plan> plan = BuildingPlan3DData.readPlan(stack);
        if (plan.isPresent()) {
            BuildingUiSessionManager.open(
                    player,
                    BuildingUiSessionManager.Tool.PLAN_3D,
                    hand,
                    stack,
                    Long.MIN_VALUE
            );
            send3DState(player, hand, plan.get(), "", false, true);
            return;
        }
        Optional<BuildingPlan3DData.Selection> selection = BuildingPlan3DData.readSelection(stack);
        if (selection.isEmpty()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_ui.select_first")
                            .formatted(Formatting.YELLOW),
                    true
            );
            return;
        }
        BuildingUiSessionManager.open(
                player,
                BuildingUiSessionManager.Tool.PLAN_3D,
                hand,
                stack,
                Long.MIN_VALUE
        );
        send3DState(player, hand, selection.get(), "", false, true);
    }

    public static void openMiniature(ServerPlayerEntity player, Hand hand) {
        if (player == null || !BuildingMiniatureManager.canUse(player)) {
            return;
        }
        ItemStack stack = player.getStackInHand(hand);
        if (!stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT)) {
            return;
        }
        if (BuildingMiniatureData.readProject(stack).isPresent()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_miniature.already_finished")
                            .formatted(Formatting.YELLOW),
                    true
            );
            return;
        }
        BuildingPlan2DManager.clearPreviewOnly(player);
        BuildingPlan3DManager.clearPreview(player, false);

        Optional<BuildingMiniatureData.Selection> selection = BuildingMiniatureData.readSelection(stack);
        if (selection.isEmpty() || !selection.get().complete()) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_miniature.selection_incomplete")
                            .formatted(Formatting.YELLOW),
                    true
            );
            return;
        }
        BuildingUiSessionManager.open(
                player,
                BuildingUiSessionManager.Tool.MINIATURE,
                hand,
                stack,
                Long.MIN_VALUE
        );
        sendMiniatureState(player, hand, selection.get(), "", false, true, false);
    }

    public static void handle(ServerPlayerEntity player, BuildingPlanUiActionPayload payload) {
        Hand hand = payload.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        ItemStack stack = player.getStackInHand(hand);
        BuildingUiSessionManager.Tool sessionTool = switch (payload.toolId()) {
            case BuildingPlanUiStatePayload.TOOL_2D -> BuildingUiSessionManager.Tool.PLAN_2D;
            case BuildingPlanUiStatePayload.TOOL_3D -> BuildingUiSessionManager.Tool.PLAN_3D;
            default -> BuildingUiSessionManager.Tool.MINIATURE;
        };
        BuildingUiSessionManager.ActionCost actionCost = switch (payload.action()) {
            case BuildingPlanUiActionPayload.SAVE_DRAFT -> BuildingUiSessionManager.ActionCost.LIGHT;
            case BuildingPlanUiActionPayload.SET_LOCKED_ROTATION -> BuildingUiSessionManager.ActionCost.MUTATION;
            default -> BuildingUiSessionManager.ActionCost.HEAVY;
        };
        if (!BuildingUiSessionManager.allow(
                player,
                sessionTool,
                hand,
                stack,
                actionCost,
                payload.hashCode(),
                Long.MIN_VALUE
        )) {
            return;
        }
        if (payload.toolId() == BuildingPlanUiStatePayload.TOOL_2D) {
            if (!stack.isOf(ModItems.BUILDING_PLAN_2D) || !BuildingPlan2DManager.canUse(player)) {
                return;
            }
            handle2D(player, hand, stack, payload);
        } else if (payload.toolId() == BuildingPlanUiStatePayload.TOOL_3D) {
            if (!stack.isOf(ModItems.BUILDING_PLAN_3D) || !BuildingPlan3DManager.canUse(player)) {
                return;
            }
            handle3D(player, hand, stack, payload);
        } else {
            if (!stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT)
                    || !BuildingMiniatureManager.canUse(player)) {
                return;
            }
            handleMiniature(player, hand, stack, payload);
        }
    }

    private static void handle2D(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            BuildingPlanUiActionPayload payload
    ) {
        Direction.Axis normal = axis(payload.normalAxisId());
        BuildingStructureRotation rotation = payload.rotation();
        Optional<BuildingPlan2DData.Plan> existingPlan = BuildingPlan2DData.readPlan(stack);

        if (payload.action() == BuildingPlanUiActionPayload.SET_LOCKED_ROTATION) {
            if (existingPlan.isEmpty()) {
                send2DError(player, hand, stack, "message.mythicrpg.building_plan_ui.not_locked");
                return;
            }
            BuildingPlan2DData.Plan plan = existingPlan.get();
            if (!BuildingPlanTransforms.canRotate(plan.entries(), rotation)) {
                send2DError(player, hand, stack, "message.mythicrpg.building_plan_ui.rotation_unsupported");
                return;
            }
            BuildingPlan2DData.Plan rotated = plan.withRotation(rotation);
            BuildingPlan2DData.writePlan(stack, rotated);
            player.getInventory().markDirty();
            BuildingPlan2DManager.clearPreviewOnly(player);
            send2DState(player, hand, rotated, "message.mythicrpg.building_plan_ui.rotation_saved", false, false);
            return;
        }

        if (existingPlan.isPresent()) {
            send2DError(player, hand, stack, "message.mythicrpg.building_plan_ui.locked");
            return;
        }

        BlockPos first = payload.first();
        BlockPos second = payload.second();
        String dimension = dimensionId(player.getServerWorld());
        boolean geometryValid = valid2DGeometry(player, first, second, normal);
        BuildingPlan2DData.Selection draft = new BuildingPlan2DData.Selection(
                dimension,
                first,
                second,
                normal,
                rotation
        );

        if (payload.action() == BuildingPlanUiActionPayload.SAVE_DRAFT) {
            BuildingPlan2DData.setSelection(stack, draft);
            player.getInventory().markDirty();
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_2D,
                    dimension,
                    first,
                    second,
                    geometryValid
            );
            return;
        }

        if (!geometryValid) {
            BuildingPlan2DData.setSelection(stack, draft);
            player.getInventory().markDirty();
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_2D,
                    dimension,
                    first,
                    second,
                    false
            );
            send2DState(player, hand, draft, "message.mythicrpg.building_plan_ui.invalid_selection", true, false);
            return;
        }

        if (!canCaptureSource(player, first, second)) {
            send2DState(player, hand, draft, "message.mythicrpg.building_plan_ui.invalid_selection", true, false);
            return;
        }

        BuildingPlan2DManager.CaptureResult result = BuildingPlan2DManager.capture(
                player,
                player.getServerWorld(),
                new BuildingPlan2DData.Selection(dimension, first, normal),
                second
        );
        if (!result.success()) {
            player.sendMessage(
                    Text.translatable(result.messageKey(), result.messageArgs()).formatted(Formatting.RED),
                    true
            );
            BuildingPlan2DData.setSelection(stack, draft);
            player.getInventory().markDirty();
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_2D,
                    dimension,
                    first,
                    second,
                    false
            );
            send2DState(player, hand, draft, "message.mythicrpg.building_plan_ui.copy_failed", true, false);
            return;
        }

        BuildingPlan2DData.Plan plan = result.plan().withRotation(rotation);
        if (!BuildingPlanTransforms.canRotate(plan.entries(), rotation)) {
            BuildingPlan2DData.setSelection(stack, draft);
            player.getInventory().markDirty();
            send2DState(player, hand, draft, "message.mythicrpg.building_plan_ui.rotation_unsupported", true, false);
            return;
        }

        BuildingPlan2DData.writePlan(stack, plan);
        BuildingSelectionBoxManager.clear(player);
        player.getInventory().markDirty();
        BuildingSoundFeedback.planLocked(player);
        send2DState(player, hand, plan, "message.mythicrpg.building_plan_ui.copied", false, false);
    }

    private static void handle3D(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            BuildingPlanUiActionPayload payload
    ) {
        BuildingStructureRotation rotation = payload.rotation();
        Optional<BuildingPlan3DData.Plan> existingPlan = BuildingPlan3DData.readPlan(stack);

        if (payload.action() == BuildingPlanUiActionPayload.SET_LOCKED_ROTATION) {
            if (existingPlan.isEmpty()) {
                send3DError(player, hand, stack, "message.mythicrpg.building_plan_ui.not_locked");
                return;
            }
            BuildingPlan3DData.Plan plan = existingPlan.get();
            if (!BuildingPlanTransforms.canRotate(plan.entries(), rotation)) {
                send3DError(player, hand, stack, "message.mythicrpg.building_plan_ui.rotation_unsupported");
                return;
            }
            BuildingPlan3DData.Plan rotated = plan.withRotation(rotation);
            BuildingPlan3DData.writePlan(stack, rotated);
            player.getInventory().markDirty();
            BuildingPlan3DManager.clearPreview(player, false);
            send3DState(player, hand, rotated, "message.mythicrpg.building_plan_ui.rotation_saved", false, false);
            return;
        }

        if (existingPlan.isPresent()) {
            send3DError(player, hand, stack, "message.mythicrpg.building_plan_ui.locked");
            return;
        }

        BlockPos first = payload.first();
        BlockPos second = payload.second();
        String dimension = dimensionId(player.getServerWorld());
        boolean geometryValid = valid3DGeometry(player, first, second);
        BuildingPlan3DData.Selection draft = new BuildingPlan3DData.Selection(
                dimension,
                first,
                second,
                rotation
        );

        if (payload.action() == BuildingPlanUiActionPayload.SAVE_DRAFT) {
            BuildingPlan3DData.setSelection(stack, draft);
            player.getInventory().markDirty();
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_3D,
                    dimension,
                    first,
                    second,
                    geometryValid
            );
            return;
        }

        if (!geometryValid) {
            BuildingPlan3DData.setSelection(stack, draft);
            player.getInventory().markDirty();
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_3D,
                    dimension,
                    first,
                    second,
                    false
            );
            send3DState(player, hand, draft, "message.mythicrpg.building_plan_ui.invalid_selection", true, false);
            return;
        }

        if (!canCaptureSource(player, first, second)) {
            send3DState(player, hand, draft, "message.mythicrpg.building_plan_ui.invalid_selection", true, false);
            return;
        }

        BuildingPlan3DManager.CaptureResult result = BuildingPlan3DManager.capture(
                player,
                player.getServerWorld(),
                new BuildingPlan3DData.Selection(dimension, first),
                second
        );
        if (!result.success()) {
            player.sendMessage(
                    Text.translatable(result.messageKey(), result.messageArgs()).formatted(Formatting.RED),
                    true
            );
            BuildingPlan3DData.setSelection(stack, draft);
            player.getInventory().markDirty();
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_3D,
                    dimension,
                    first,
                    second,
                    false
            );
            send3DState(player, hand, draft, "message.mythicrpg.building_plan_ui.copy_failed", true, false);
            return;
        }

        BuildingPlan3DData.Plan plan = result.plan().withRotation(rotation);
        if (!BuildingPlanTransforms.canRotate(plan.entries(), rotation)) {
            BuildingPlan3DData.setSelection(stack, draft);
            player.getInventory().markDirty();
            send3DState(player, hand, draft, "message.mythicrpg.building_plan_ui.rotation_unsupported", true, false);
            return;
        }

        BuildingPlan3DData.writePlan(stack, plan);
        BuildingSelectionBoxManager.clear(player);
        player.getInventory().markDirty();
        BuildingSoundFeedback.planLocked(player);
        send3DState(player, hand, plan, "message.mythicrpg.building_plan_ui.copied", false, false);
    }

    private static void handleMiniature(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            BuildingPlanUiActionPayload payload
    ) {
        if (BuildingMiniatureData.readProject(stack).isPresent()) {
            sendMiniatureError(player, hand, stack, "message.mythicrpg.building_miniature.already_finished");
            return;
        }

        BlockPos first = payload.first();
        BlockPos second = payload.second();
        BuildingStructureRotation rotation = payload.rotation();
        String dimension = dimensionId(player.getServerWorld());
        boolean geometryValid = validMiniatureGeometry(player, first, second);
        BuildingMiniatureData.Selection draft = new BuildingMiniatureData.Selection(
                dimension,
                first,
                second,
                rotation
        );

        if (payload.action() == BuildingPlanUiActionPayload.SAVE_DRAFT) {
            BuildingMiniatureData.writeSelection(stack, draft);
            player.getInventory().markDirty();
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.MINIATURE,
                    dimension,
                    first,
                    second,
                    geometryValid
            );
            return;
        }

        if (payload.action() != BuildingPlanUiActionPayload.MINIATURIZE) {
            sendMiniatureState(
                    player,
                    hand,
                    draft,
                    "message.mythicrpg.building_miniature.invalid_ui_action",
                    true,
                    false,
                    false
            );
            return;
        }

        BuildingMiniatureData.writeSelection(stack, draft);
        player.getInventory().markDirty();
        if (!geometryValid) {
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.MINIATURE,
                    dimension,
                    first,
                    second,
                    false
            );
            sendMiniatureState(
                    player,
                    hand,
                    draft,
                    "message.mythicrpg.building_plan_ui.invalid_selection",
                    true,
                    false,
                    false
            );
            return;
        }

        if (!canCaptureSource(player, first, second)) {
            sendMiniatureState(
                    player,
                    hand,
                    draft,
                    "message.mythicrpg.building_plan_ui.invalid_selection",
                    true,
                    false,
                    false
            );
            return;
        }

        if (!BuildingMiniatureManager.finish(player, stack)) {
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.MINIATURE,
                    dimension,
                    first,
                    second,
                    false
            );
            sendMiniatureState(
                    player,
                    hand,
                    draft,
                    "message.mythicrpg.building_miniature.finish_failed",
                    true,
                    false,
                    false
            );
            return;
        }

        player.getInventory().markDirty();
        BuildingSoundFeedback.miniatureFinalized(player);
        sendMiniatureState(
                player,
                hand,
                draft,
                "message.mythicrpg.building_miniature.finished_ui",
                false,
                false,
                true
        );
        BuildingUiSessionManager.close(player);
    }

    private static boolean canCaptureSource(
            ServerPlayerEntity player,
            BlockPos first,
            BlockPos second
    ) {
        ServerWorld world = player.getServerWorld();
        if (player.squaredDistanceTo(
                first.getX() + 0.5D,
                first.getY() + 0.5D,
                first.getZ() + 0.5D
        ) > 64.0D * 64.0D || player.squaredDistanceTo(
                second.getX() + 0.5D,
                second.getY() + 0.5D,
                second.getZ() + 0.5D
        ) > 64.0D * 64.0D) {
            return false;
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
        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (!world.isChunkLoaded(pos) || !world.canPlayerModifyAt(player, pos)) {
                return false;
            }
        }
        return true;
    }

    private static boolean valid2DGeometry(
            ServerPlayerEntity player,
            BlockPos first,
            BlockPos second,
            Direction.Axis normal
    ) {
        if (!validWorldPositions(player.getServerWorld(), first, second)
                || coordinate(first, normal) != coordinate(second, normal)) {
            return false;
        }
        Direction.Axis[] tangents = tangentAxes(normal);
        int sizeU = Math.abs(coordinate(second, tangents[0]) - coordinate(first, tangents[0])) + 1;
        int sizeV = Math.abs(coordinate(second, tangents[1]) - coordinate(first, tangents[1])) + 1;
        int max = BuildingPlan2DManager.maxSize(player);
        return sizeU <= max && sizeV <= max;
    }

    private static boolean valid3DGeometry(ServerPlayerEntity player, BlockPos first, BlockPos second) {
        if (!validWorldPositions(player.getServerWorld(), first, second)) {
            return false;
        }
        return Math.abs(second.getX() - first.getX()) + 1 <= BuildingPlan3DManager.MAX_SIZE
                && Math.abs(second.getY() - first.getY()) + 1 <= BuildingPlan3DManager.MAX_SIZE
                && Math.abs(second.getZ() - first.getZ()) + 1 <= BuildingPlan3DManager.MAX_SIZE;
    }

    private static boolean validMiniatureGeometry(
            ServerPlayerEntity player,
            BlockPos first,
            BlockPos second
    ) {
        if (!validWorldPositions(player.getServerWorld(), first, second)) {
            return false;
        }
        return Math.abs(second.getX() - first.getX()) + 1 <= 5
                && Math.abs(second.getY() - first.getY()) + 1 <= 5
                && Math.abs(second.getZ() - first.getZ()) + 1 <= 5;
    }

    private static boolean validWorldPositions(ServerWorld world, BlockPos first, BlockPos second) {
        return first != null
                && second != null
                && world.isInBuildLimit(first)
                && world.isInBuildLimit(second)
                && world.getWorldBorder().contains(first)
                && world.getWorldBorder().contains(second);
    }

    private static void sendMiniatureError(
            ServerPlayerEntity player,
            Hand hand,
            ItemStack stack,
            String key
    ) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true);
        BuildingSoundFeedback.error(player);
        BuildingMiniatureData.readSelection(stack).ifPresent(
                selection -> sendMiniatureState(player, hand, selection, key, true, false, false)
        );
    }

    private static void send2DError(ServerPlayerEntity player, Hand hand, ItemStack stack, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true);
        BuildingSoundFeedback.error(player);
        BuildingPlan2DData.readPlan(stack).ifPresentOrElse(
                plan -> send2DState(player, hand, plan, key, true, false),
                () -> BuildingPlan2DData.readSelection(stack).ifPresent(
                        selection -> send2DState(player, hand, selection, key, true, false)
                )
        );
    }

    private static void send3DError(ServerPlayerEntity player, Hand hand, ItemStack stack, String key) {
        player.sendMessage(Text.translatable(key).formatted(Formatting.RED), true);
        BuildingSoundFeedback.error(player);
        BuildingPlan3DData.readPlan(stack).ifPresentOrElse(
                plan -> send3DState(player, hand, plan, key, true, false),
                () -> BuildingPlan3DData.readSelection(stack).ifPresent(
                        selection -> send3DState(player, hand, selection, key, true, false)
                )
        );
    }

    private static void send2DState(
            ServerPlayerEntity player,
            Hand hand,
            BuildingPlan2DData.Selection selection,
            String message,
            boolean error,
            boolean openScreen
    ) {
        BlockPos second = selection.second() == null ? selection.first() : selection.second();
        send(player, new BuildingPlanUiStatePayload(
                BuildingPlanUiStatePayload.TOOL_2D,
                handId(hand),
                openScreen,
                false,
                selection.dimensionId(),
                selection.first().asLong(),
                second.asLong(),
                true,
                selection.second() != null,
                axisId(selection.normalAxis()),
                selection.rotation().xQuarterTurns(),
                selection.rotation().yQuarterTurns(),
                selection.rotation().zQuarterTurns(),
                BuildingPlan2DManager.maxSize(player),
                message,
                error
        ));
    }

    private static void send2DState(
            ServerPlayerEntity player,
            Hand hand,
            BuildingPlan2DData.Plan plan,
            String message,
            boolean error,
            boolean openScreen
    ) {
        BlockPos first = plan.sourceFirst() == null ? BlockPos.ORIGIN : plan.sourceFirst();
        BlockPos second = plan.sourceSecond() == null ? first : plan.sourceSecond();
        send(player, new BuildingPlanUiStatePayload(
                BuildingPlanUiStatePayload.TOOL_2D,
                handId(hand),
                openScreen,
                true,
                plan.sourceDimension() == null || plan.sourceDimension().isBlank()
                        ? dimensionId(player.getServerWorld())
                        : plan.sourceDimension(),
                first.asLong(),
                second.asLong(),
                plan.sourceFirst() != null,
                plan.sourceSecond() != null,
                axisId(plan.normalAxis()),
                plan.rotation().xQuarterTurns(),
                plan.rotation().yQuarterTurns(),
                plan.rotation().zQuarterTurns(),
                BuildingPlan2DManager.maxSize(player),
                message,
                error
        ));
    }

    private static void send3DState(
            ServerPlayerEntity player,
            Hand hand,
            BuildingPlan3DData.Selection selection,
            String message,
            boolean error,
            boolean openScreen
    ) {
        BlockPos second = selection.second() == null ? selection.first() : selection.second();
        send(player, new BuildingPlanUiStatePayload(
                BuildingPlanUiStatePayload.TOOL_3D,
                handId(hand),
                openScreen,
                false,
                selection.dimensionId(),
                selection.first().asLong(),
                second.asLong(),
                true,
                selection.second() != null,
                0,
                selection.rotation().xQuarterTurns(),
                selection.rotation().yQuarterTurns(),
                selection.rotation().zQuarterTurns(),
                BuildingPlan3DManager.MAX_SIZE,
                message,
                error
        ));
    }

    private static void send3DState(
            ServerPlayerEntity player,
            Hand hand,
            BuildingPlan3DData.Plan plan,
            String message,
            boolean error,
            boolean openScreen
    ) {
        BlockPos first = plan.sourceFirst() == null ? BlockPos.ORIGIN : plan.sourceFirst();
        BlockPos second = plan.sourceSecond() == null ? first : plan.sourceSecond();
        send(player, new BuildingPlanUiStatePayload(
                BuildingPlanUiStatePayload.TOOL_3D,
                handId(hand),
                openScreen,
                true,
                plan.sourceDimension() == null || plan.sourceDimension().isBlank()
                        ? dimensionId(player.getServerWorld())
                        : plan.sourceDimension(),
                first.asLong(),
                second.asLong(),
                plan.sourceFirst() != null,
                plan.sourceSecond() != null,
                0,
                plan.rotation().xQuarterTurns(),
                plan.rotation().yQuarterTurns(),
                plan.rotation().zQuarterTurns(),
                BuildingPlan3DManager.MAX_SIZE,
                message,
                error
        ));
    }

    private static void sendMiniatureState(
            ServerPlayerEntity player,
            Hand hand,
            BuildingMiniatureData.Selection selection,
            String message,
            boolean error,
            boolean openScreen,
            boolean locked
    ) {
        BlockPos second = selection.second() == null ? selection.first() : selection.second();
        send(player, new BuildingPlanUiStatePayload(
                BuildingPlanUiStatePayload.TOOL_MINIATURE,
                handId(hand),
                openScreen,
                locked,
                selection.dimensionId(),
                selection.first().asLong(),
                second.asLong(),
                true,
                selection.second() != null,
                0,
                selection.rotation().xQuarterTurns(),
                selection.rotation().yQuarterTurns(),
                selection.rotation().zQuarterTurns(),
                5,
                message,
                error
        ));
    }

    private static void send(ServerPlayerEntity player, BuildingPlanUiStatePayload payload) {
        ServerPlayNetworking.send(player, payload);
    }

    private static int handId(Hand hand) {
        return hand == Hand.MAIN_HAND ? 0 : 1;
    }

    private static int axisId(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    public static Direction.Axis axis(int id) {
        return switch (Math.floorMod(id, 3)) {
            case 0 -> Direction.Axis.X;
            case 1 -> Direction.Axis.Y;
            default -> Direction.Axis.Z;
        };
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static Direction.Axis[] tangentAxes(Direction.Axis normal) {
        return switch (normal) {
            case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
        };
    }

    private static String dimensionId(ServerWorld world) {
        return world.getRegistryKey().getValue().toString();
    }
}
