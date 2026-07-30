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
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/** Reusable WorldEdit-like selector, configuration and placement tool unlocked by perk 4. */
public final class BuildingPlan3DItem extends Item {
    public BuildingPlan3DItem(Settings settings) {
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
            if (BuildingPlan3DManager.cancelInteractiveState(player)) {
                BuildingSelectionBoxManager.clear(player);
                return ActionResult.CONSUME;
            }
            clearData(player, stack);
            return ActionResult.CONSUME;
        }
        if (!BuildingPlan3DManager.canUse(player)) {
            player.sendMessage(
                    Text.translatable("message.mythicrpg.building_plan_3d.locked")
                            .formatted(Formatting.RED),
                    true
            );
            BuildingSoundFeedback.error(player);
            return ActionResult.FAIL;
        }

        Optional<BuildingPlan3DData.Plan> plan = BuildingPlan3DData.readPlan(stack);
        if (plan.isPresent()) {
            BuildingSelectionBoxManager.clear(player);
            BlockPos anchor = context.getBlockPos().offset(context.getSide()).toImmutable();
            BuildingPlan3DManager.previewOrConfirm(player, world, plan.get(), anchor);
            return ActionResult.CONSUME;
        }

        Optional<BuildingPlan3DData.Selection> current = BuildingPlan3DData.readSelection(stack);
        if (current.isEmpty()) {
            BuildingPlan3DData.Selection selection = new BuildingPlan3DData.Selection(
                    world.getRegistryKey().getValue().toString(),
                    context.getBlockPos().toImmutable()
            );
            BuildingPlan3DData.setSelection(stack, selection);
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_3D,
                    selection.dimensionId(),
                    selection.first(),
                    null,
                    true
            );
            player.getInventory().markDirty();
            player.sendMessage(Text.translatable(
                    "message.mythicrpg.building_plan_3d.first_corner",
                    BuildingPlan3DManager.MAX_SIZE,
                    BuildingPlan3DManager.MAX_SIZE,
                    BuildingPlan3DManager.MAX_SIZE
            ).formatted(Formatting.AQUA), true);
            BuildingSoundFeedback.pointA(player, selection.first());
            return ActionResult.CONSUME;
        }

        BuildingPlan3DData.Selection previous = current.get();
        String currentDimension = world.getRegistryKey().getValue().toString();
        if (!previous.dimensionId().equals(currentDimension)) {
            BuildingPlan3DData.Selection selection = new BuildingPlan3DData.Selection(
                    currentDimension,
                    context.getBlockPos().toImmutable()
            );
            BuildingPlan3DData.setSelection(stack, selection);
            BuildingSelectionBoxManager.show(
                    player,
                    BuildingUiTool.PLAN_3D,
                    selection.dimensionId(),
                    selection.first(),
                    null,
                    true
            );
            player.getInventory().markDirty();
            player.sendMessage(Text.translatable(
                    "message.mythicrpg.building_plan_3d.first_corner",
                    BuildingPlan3DManager.MAX_SIZE,
                    BuildingPlan3DManager.MAX_SIZE,
                    BuildingPlan3DManager.MAX_SIZE
            ).formatted(Formatting.AQUA), true);
            BuildingSoundFeedback.pointA(player, selection.first());
            return ActionResult.CONSUME;
        }
        BlockPos second = context.getBlockPos().toImmutable();
        BuildingPlan3DData.Selection selection = new BuildingPlan3DData.Selection(
                previous.dimensionId(),
                previous.first(),
                second,
                previous.rotation()
        );
        BuildingPlan3DData.setSelection(stack, selection);
        boolean valid = basicSelectionValid(selection);
        BuildingSelectionBoxManager.show(
                player,
                BuildingUiTool.PLAN_3D,
                selection.dimensionId(),
                selection.first(),
                selection.second(),
                valid
        );
        player.getInventory().markDirty();
        if (valid) BuildingSoundFeedback.pointB(player, second);
        else BuildingSoundFeedback.error(player);
        BuildingPlanUiManager.open3D(player, context.getHand());
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
            if (BuildingPlan3DManager.cancelInteractiveState(player)) {
                BuildingSelectionBoxManager.clear(player);
                return TypedActionResult.success(stack);
            }
            clearData(player, stack);
            return TypedActionResult.success(stack);
        }
        BuildingPlanUiManager.open3D(player, hand);
        return TypedActionResult.success(stack);
    }

    private static boolean basicSelectionValid(BuildingPlan3DData.Selection selection) {
        if (selection.second() == null) {
            return false;
        }
        BlockPos first = selection.first();
        BlockPos second = selection.second();
        return Math.abs(second.getX() - first.getX()) + 1 <= BuildingPlan3DManager.MAX_SIZE
                && Math.abs(second.getY() - first.getY()) + 1 <= BuildingPlan3DManager.MAX_SIZE
                && Math.abs(second.getZ() - first.getZ()) + 1 <= BuildingPlan3DManager.MAX_SIZE;
    }

    private static void clearData(ServerPlayerEntity player, ItemStack stack) {
        boolean hadData = BuildingPlan3DData.readSelection(stack).isPresent()
                || BuildingPlan3DData.readPlan(stack).isPresent();
        BuildingPlan3DData.clearAll(stack);
        BuildingSelectionBoxManager.clear(player);
        player.getInventory().markDirty();
        player.sendMessage(Text.translatable(hadData
                        ? "message.mythicrpg.building_plan_3d.cleared"
                        : "message.mythicrpg.building_plan_3d.already_empty")
                .formatted(Formatting.YELLOW), true);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        Optional<BuildingPlan3DData.Plan> plan = BuildingPlan3DData.readPlan(stack);
        if (plan.isPresent()) {
            BuildingPlan3DData.Plan data = plan.get();
            tooltip.add(Text.translatable(
                    "tooltip.mythicrpg.building_plan_3d.saved",
                    data.sizeX(), data.sizeY(), data.sizeZ(), data.blockCount()
            ).formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_ui.open_locked")
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_3d.place")
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_3d.confirm")
                    .formatted(Formatting.GREEN));
        } else if (BuildingPlan3DData.readSelection(stack).isPresent()) {
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_ui.open_draft")
                    .formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_3d.select_first")
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_3d.volume")
                    .formatted(Formatting.DARK_GRAY));
        }
        tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_3d.clear")
                .formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("tooltip.mythicrpg.building_plan_3d.no_xp")
                .formatted(Formatting.DARK_AQUA));
    }
}
