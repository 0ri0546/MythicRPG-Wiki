package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.BuildingPlan3DData;
import com.mythicrpg.building.BuildingPlanUiStatePayload;
import com.mythicrpg.building.BuildingStructureRotation;
import com.mythicrpg.building.BuildingUiTool;
import com.mythicrpg.core.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Vanilla-style 3D plan capture and rotation interface. */
public final class BuildingPlan3DScreen extends AbstractBuildingPlanScreen {
    public BuildingPlan3DScreen(BuildingPlanUiStatePayload state) {
        super(Text.translatable("screen.mythicrpg.building_plan_3d.title"), state);
    }

    @Override
    protected BuildingClientPlanCapture.Result captureDraft(BlockPos first, BlockPos second) {
        return BuildingClientPlanCapture.capture3D(
                client == null ? null : client.world,
                dimensionId,
                first,
                second,
                maxSize
        );
    }

    @Override
    protected BuildingPreviewModel readLockedModel(ItemStack stack) {
        return BuildingPlan3DData.readPlan(stack)
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
        return stack.isOf(ModItems.BUILDING_PLAN_3D);
    }

    @Override
    protected BuildingUiTool uiTool() {
        return BuildingUiTool.PLAN_3D;
    }
}
