package com.mythicrpg.building;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/** Reusable selection, configuration and placement tool unlocked by Building perks 2 and 3. */
public final class BuildingPlan2DItem extends Item {
    public BuildingPlan2DItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity player)
                || !(context.getWorld() instanceof ServerWorld world)) {
            return ActionResult.PASS;
        }

        ItemStack stack = context.getStack();
        if (player.isSneaking()) {
            if (BuildingPlan2DManager.cancelInteractiveState(player)) {
                BuildingSelectionBoxManager.clear(player);
                return ActionResult.CONSUME;
            }
            clearData(player, stack);
            return ActionResult.CONSUME;
        }

        if (!BuildingPlan2DManager.canUse(player)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_2d.locked")
                            .formatted(Formatting.RED),
                    true
            );
            BuildingSoundFeedback.error(player);
            return ActionResult.FAIL;
        }

        Optional<BuildingPlan2DData.Plan> plan = BuildingPlan2DData.readPlan(stack);
        if (plan.isPresent()) {
            BuildingSelectionBoxManager.clear(player);
            BlockPos anchor = context.getBlockPos().offset(context.getSide()).toImmutable();
            BuildingPlan2DManager.previewOrConfirm(player, world, plan.get(), anchor);
            return ActionResult.CONSUME;
        }

        Optional<BuildingPlan2DData.Selection> current = BuildingPlan2DData.readSelection(stack);
        if (current.isEmpty()) {
            BuildingPlan2DData.Selection selection = new BuildingPlan2DData.Selection(
                    world.getRegistryKey().getValue().toString(),
                    context.getBlockPos().toImmutable(),
                    context.getSide().getAxis()
            );
            BuildingPlan2DData.setSelection(stack, selection);
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_2D,
                    selection.dimensionId(),
                    selection.first(),
                    null,
                    true
            );
            player.getInventory().markDirty();
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.building_plan_2d.first_corner",
                            BuildingPlan2DManager.maxSize(player),
                            BuildingPlan2DManager.maxSize(player)
                    ).formatted(Formatting.AQUA),
                    true
            );
            BuildingSoundFeedback.pointA(player, selection.first());
            return ActionResult.CONSUME;
        }

        BuildingPlan2DData.Selection previous = current.get();
        String currentDimension = world.getRegistryKey().getValue().toString();
        if (!previous.dimensionId().equals(currentDimension)) {
            BuildingPlan2DData.Selection selection = new BuildingPlan2DData.Selection(
                    currentDimension,
                    context.getBlockPos().toImmutable(),
                    context.getSide().getAxis()
            );
            BuildingPlan2DData.setSelection(stack, selection);
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_2D,
                    selection.dimensionId(),
                    selection.first(),
                    null,
                    true
            );
            player.getInventory().markDirty();
            player.sendMessage(
                    Text.translatable(
                            "message.mythicrpg.building_plan_2d.first_corner",
                            BuildingPlan2DManager.maxSize(player),
                            BuildingPlan2DManager.maxSize(player)
                    ).formatted(Formatting.AQUA),
                    true
            );
            BuildingSoundFeedback.pointA(player, selection.first());
            return ActionResult.CONSUME;
        }
        BlockPos second = context.getBlockPos().toImmutable();
        BuildingPlan2DData.Selection selection = new BuildingPlan2DData.Selection(
                previous.dimensionId(),
                previous.first(),
                second,
                previous.normalAxis(),
                previous.rotation()
        );
        BuildingPlan2DData.setSelection(stack, selection);
        boolean valid = basicSelectionValid(selection, BuildingPlan2DManager.maxSize(player));
        BuildingSelectionBoxManager.show(
                player,
                BuildingUiTool.PLAN_2D,
                selection.dimensionId(),
                selection.first(),
                selection.second(),
                valid
        );
        player.getInventory().markDirty();
        if (valid) BuildingSoundFeedback.pointB(player, second);
        else BuildingSoundFeedback.error(player);
        BuildingPlanUiManager.open2D(player, context.getHand());
        return ActionResult.CONSUME;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        if (player.isSneaking()) {
            if (BuildingPlan2DManager.cancelInteractiveState(player)) {
                BuildingSelectionBoxManager.clear(player);
                return TypedActionResult.success(stack);
            }
            clearData(player, stack);
            return TypedActionResult.success(stack);
        }

        BuildingPlanUiManager.open2D(player, hand);
        return TypedActionResult.success(stack);
    }

    private static boolean basicSelectionValid(BuildingPlan2DData.Selection selection, int maxSize) {
        if (selection.second() == null || maxSize <= 0) {
            return false;
        }
        Direction.Axis normal = selection.normalAxis();
        if (coordinate(selection.first(), normal) != coordinate(selection.second(), normal)) {
            return false;
        }
        Direction.Axis[] tangents = switch (normal) {
            case X -> new Direction.Axis[]{Direction.Axis.Y, Direction.Axis.Z};
            case Y -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z};
            case Z -> new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Y};
        };
        return Math.abs(coordinate(selection.second(), tangents[0])
                - coordinate(selection.first(), tangents[0])) + 1 <= maxSize
                && Math.abs(coordinate(selection.second(), tangents[1])
                - coordinate(selection.first(), tangents[1])) + 1 <= maxSize;
    }

    private static int coordinate(BlockPos pos, Direction.Axis axis) {
        return switch (axis) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    private static void clearData(ServerPlayerEntity player, ItemStack stack) {
        boolean hadData = BuildingPlan2DData.readSelection(stack).isPresent()
                || BuildingPlan2DData.readPlan(stack).isPresent();
        BuildingPlan2DData.clearAll(stack);
        BuildingSelectionBoxManager.clear(player);
        player.getInventory().markDirty();
        player.sendMessage(
                Text.translatable(
                        hadData
                                ? "message.mythicrpg.building_plan_2d.cleared"
                                : "message.mythicrpg.building_plan_2d.already_empty"
                ).formatted(Formatting.YELLOW),
                true
        );
    }

    @Override
    public void appendTooltip(
            ItemStack stack,
            TooltipContext context,
            List<Text> tooltip,
            TooltipType type
    ) {
        Optional<BuildingPlan2DData.Plan> plan = BuildingPlan2DData.readPlan(stack);
        if (plan.isPresent()) {
            BuildingPlan2DData.Plan data = plan.get();
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.building_plan_2d.saved",
                    data.sizeU(),
                    data.sizeV(),
                    data.blockCount()
            ).formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_ui.open_locked")
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_2d.place")
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_2d.confirm")
                    .formatted(Formatting.GREEN));
        } else if (BuildingPlan2DData.readSelection(stack).isPresent()) {
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_ui.open_draft")
                    .formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_2d.select_first")
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_2d.face_plane")
                    .formatted(Formatting.DARK_GRAY));
        }
        tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_2d.clear")
                .formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_2d.no_xp")
                .formatted(Formatting.DARK_AQUA));
    }
}
