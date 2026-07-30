package com.mythicrpg.building;

import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/** Defines an ongoing project, freezes it, then places the decorative miniature entity. */
public final class BuildingMiniatureProjectItem extends Item {
    public BuildingMiniatureProjectItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.SUCCESS;
        if (!hasPerk(serverPlayer)) {
            serverPlayer.sendMessage(Text.translatable("message.mythicrpg.building_miniature.no_perk")
                    .formatted(Formatting.RED), true);
            BuildingSoundFeedback.error(serverPlayer);
            return ActionResult.FAIL;
        }

        Optional<BuildingMiniatureData.Project> project = BuildingMiniatureData.readProject(stack);
        if (project.isPresent()) {
            BuildingSelectionBoxManager.clear(serverPlayer);
            return BuildingMiniatureManager.place(serverPlayer, context, project.get())
                    ? ActionResult.SUCCESS : ActionResult.FAIL;
        }

        String dimension = context.getWorld().getRegistryKey().getValue().toString();
        BlockPos clicked = context.getBlockPos();
        Optional<BuildingMiniatureData.Selection> current = BuildingMiniatureData.readSelection(stack);
        if (current.isEmpty() || current.get().complete() || !current.get().dimensionId().equals(dimension)) {
            BuildingMiniatureData.writeFirst(stack, dimension, clicked);
            serverPlayer.getInventory().markDirty();
            BuildingSelectionBoxManager.show(
                    serverPlayer,
                    BuildingUiTool.MINIATURE,
                    dimension,
                    clicked,
                    null,
                    true
            );
            serverPlayer.sendMessage(Text.translatable("message.mythicrpg.building_miniature.first", clicked.toShortString())
                    .formatted(Formatting.AQUA), true);
            BuildingSoundFeedback.pointA(serverPlayer, clicked);
            return ActionResult.SUCCESS;
        }

        BuildingMiniatureData.Selection selection = new BuildingMiniatureData.Selection(
                dimension,
                current.get().first(),
                clicked,
                current.get().rotation()
        );
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        int sx = max.getX() - min.getX() + 1;
        int sy = max.getY() - min.getY() + 1;
        int sz = max.getZ() - min.getZ() + 1;
        boolean geometryValid = sx <= 5 && sy <= 5 && sz <= 5;
        BuildingMiniatureData.writeSelection(stack, selection);
        serverPlayer.getInventory().markDirty();
        BuildingSelectionBoxManager.show(
                serverPlayer,
                BuildingUiTool.MINIATURE,
                dimension,
                selection.first(),
                selection.second(),
                geometryValid
        );
        if (geometryValid) {
            serverPlayer.sendMessage(Text.translatable(
                    "message.mythicrpg.building_miniature.defined", sx, sy, sz
            ).formatted(Formatting.GREEN), true);
            BuildingSoundFeedback.pointB(serverPlayer, clicked);
        } else {
            serverPlayer.sendMessage(Text.translatable(
                    "message.mythicrpg.building_miniature.too_large", sx, sy, sz
            ).formatted(Formatting.RED), true);
            BuildingSoundFeedback.error(serverPlayer);
        }
        BuildingPlanUiManager.openMiniature(serverPlayer, context.getHand());
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) return TypedActionResult.success(stack);
        if (!(user instanceof ServerPlayerEntity player) || !hasPerk(player)) {
            return TypedActionResult.fail(stack);
        }
        if (BuildingMiniatureData.readProject(stack).isPresent()) {
            return TypedActionResult.pass(stack);
        }
        BuildingPlanUiManager.openMiniature(player, hand);
        return TypedActionResult.success(stack);
    }

    @Override
    public Text getName(ItemStack stack) {
        return BuildingMiniatureData.readProject(stack).isPresent()
                ? Text.translatable("item.mythicrpg.building_miniature.finished")
                : Text.translatable("item.mythicrpg.building_miniature_project");
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        Optional<BuildingMiniatureData.Project> project = BuildingMiniatureData.readProject(stack);
        if (project.isPresent()) {
            BuildingMiniatureData.Project value = project.get();
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.status_finished")
                    .formatted(Formatting.GREEN));
            BuildingStructureRotation.Size rotatedSize = value.rotatedSize();
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.size",
                    rotatedSize.x(), rotatedSize.y(), rotatedSize.z()).formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.blocks", value.blockCount())
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.author", value.authorName())
                    .formatted(Formatting.DARK_AQUA));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.place")
                    .formatted(Formatting.GREEN));
            return;
        }
        Optional<BuildingMiniatureData.Selection> selection = BuildingMiniatureData.readSelection(stack);
        if (selection.isPresent() && selection.get().complete()) {
            BlockPos min = selection.get().min();
            BlockPos max = selection.get().max();
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.status_ongoing")
                    .formatted(Formatting.YELLOW));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.size",
                    max.getX() - min.getX() + 1,
                    max.getY() - min.getY() + 1,
                    max.getZ() - min.getZ() + 1).formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.finish")
                    .formatted(Formatting.GREEN));
        } else {
            tooltip.add(Text.translatable("tooltip.mythicrpg.building_miniature.select")
                    .formatted(Formatting.GREEN));
        }
    }

    private static boolean hasPerk(ServerPlayerEntity player) {
        return SkillTreeManager.hasBonus(player, SkillType.BUILDING, BonusType.BUILD_MINIATURE);
    }
}
