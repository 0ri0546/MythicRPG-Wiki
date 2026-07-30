package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.BuildingPlan2DData;
import com.mythicrpg.building.BuildingPlanUiStatePayload;
import com.mythicrpg.building.BuildingStructureRotation;
import com.mythicrpg.building.BuildingUiTool;
import com.mythicrpg.core.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Vanilla-style 2D plan capture interface with a shared rotating 3D preview. */
public final class BuildingPlan2DScreen extends AbstractBuildingPlanScreen {
    public BuildingPlan2DScreen(BuildingPlanUiStatePayload state) {
        super(Text.translatable("screen.mythicrpg.building_plan_2d.title"), state);
    }

    @Override
    protected BuildingClientPlanCapture.Result captureDraft(BlockPos first, BlockPos second) {
        return BuildingClientPlanCapture.capture2D(
                client == null ? null : client.world,
                dimensionId,
                first,
                second,
                normalAxis,
                maxSize
        );
    }

    @Override
    protected BuildingPreviewModel readLockedModel(ItemStack stack) {
        return BuildingPlan2DData.readPlan(stack)
                .map(BuildingPreviewModel::from)
                .orElse(BuildingPreviewModel.EMPTY);
    }

    @Override
    protected void renderPlanPreview(
            DrawContext context,
            BuildingPreviewModel model,
            BuildingStructureRotation rotation,
            int x,
            int y,
            int width,
            int height,
            float automaticYaw
    ) {
        BuildingStructurePreviewRenderer.render3D(
                context,
                model,
                rotation,
                x,
                y,
                width,
                height,
                automaticYaw
        );
    }

    @Override
    protected boolean isHeldToolValid(ItemStack stack) {
        return stack.isOf(ModItems.BUILDING_PLAN_2D);
    }

    @Override
    protected BuildingUiTool uiTool() {
        return BuildingUiTool.PLAN_2D;
    }
}
