package com.mythicrpg.client.building;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mythicrpg.building.BuildingMiniatureData;
import com.mythicrpg.building.BuildingPlan2DData;
import com.mythicrpg.building.BuildingPlan3DData;
import com.mythicrpg.building.BuildingSelectionBoxPayload;
import com.mythicrpg.building.BuildingUiTool;
import com.mythicrpg.core.ModItems;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Renders one bounded selection box, plus distinct A/B corner markers. */
public final class BuildingSelectionBoxClient {
    private static final float BOX_EXPAND = 0.025F;
    private static final float CORNER_EXPAND = 0.055F;

    private static ClientWorld activeWorld;
    private static BuildingUiTool tool;
    private static String dimensionId = "";
    private static BlockPos first;
    private static BlockPos second;
    private static boolean hasSecond;
    private static boolean valid;
    private static boolean uiOwned;
    private static ItemStack lastMainStack;
    private static ItemStack lastOffStack;
    private static NbtComponent lastMainData;
    private static NbtComponent lastOffData;

    private BuildingSelectionBoxClient() {
    }

    public static void handle(BuildingSelectionBoxPayload payload) {
        if (payload.action() == BuildingSelectionBoxPayload.CLEAR) {
            clearSelection();
            return;
        }
        BuildingUiTool.fromNetworkId(payload.toolId()).ifPresentOrElse(value -> {
            tool = value;
            dimensionId = payload.dimensionId();
            first = payload.first();
            second = payload.second();
            hasSecond = payload.hasSecond();
            valid = payload.valid();
        }, BuildingSelectionBoxClient::clearSelection);
    }

    public static void tick(MinecraftClient client) {
        if (client.world != activeWorld) {
            clear();
            activeWorld = client.world;
        }
        if (client.player == null) {
            return;
        }
        if (uiOwned) {
            return;
        }
        ItemStack main = client.player.getMainHandStack();
        ItemStack off = client.player.getOffHandStack();
        if (!heldStacksChanged(main, off)) {
            return;
        }

        clearSelectionStateOnly();
        restoreFromHeldStack(main);
        if (tool == null) {
            restoreFromHeldStack(off);
        }
    }

    public static void clear() {
        activeWorld = null;
        invalidateHeldCache();
        clearSelection();
    }

    private static void clearSelection() {
        clearSelectionStateOnly();
        uiOwned = false;
    }

    private static void clearSelectionStateOnly() {
        tool = null;
        dimensionId = "";
        first = null;
        second = null;
        hasSecond = false;
        valid = false;
    }

    public static void setUiSelection(
            BuildingUiTool selectedTool,
            String selectedDimension,
            BlockPos selectedFirst,
            BlockPos selectedSecond,
            boolean selectedValid
    ) {
        uiOwned = true;
        setLocal(selectedTool, selectedDimension, selectedFirst, selectedSecond, selectedValid);
    }

    public static void clearUiSelection() {
        uiOwned = true;
        tool = null;
        dimensionId = "";
        first = null;
        second = null;
        hasSecond = false;
        valid = false;
    }

    public static void releaseUiOwnership() {
        uiOwned = false;
        invalidateHeldCache();
    }


    private static void restoreFromHeldStack(ItemStack stack) {
        if (stack.isOf(ModItems.BUILDING_PLAN_2D)) {
            BuildingPlan2DData.readSelection(stack).ifPresent(selection -> setLocal(
                    BuildingUiTool.PLAN_2D,
                    selection.dimensionId(),
                    selection.first(),
                    selection.complete() ? selection.second() : null,
                    true
            ));
            return;
        }
        if (stack.isOf(ModItems.BUILDING_PLAN_3D)) {
            BuildingPlan3DData.readSelection(stack).ifPresent(selection -> setLocal(
                    BuildingUiTool.PLAN_3D,
                    selection.dimensionId(),
                    selection.first(),
                    selection.complete() ? selection.second() : null,
                    true
            ));
            return;
        }
        if (stack.isOf(ModItems.BUILDING_MINIATURE_PROJECT)) {
            BuildingMiniatureData.readSelection(stack).ifPresent(selection -> setLocal(
                    BuildingUiTool.MINIATURE,
                    selection.dimensionId(),
                    selection.first(),
                    selection.complete() ? selection.second() : null,
                    true
            ));
        }
    }

    private static void setLocal(
            BuildingUiTool selectedTool,
            String selectedDimension,
            BlockPos selectedFirst,
            BlockPos selectedSecond,
            boolean selectedValid
    ) {
        tool = selectedTool;
        dimensionId = selectedDimension == null ? "" : selectedDimension;
        first = selectedFirst;
        second = selectedSecond == null ? selectedFirst : selectedSecond;
        hasSecond = selectedSecond != null;
        valid = selectedValid;
    }


    private static boolean heldStacksChanged(ItemStack main, ItemStack off) {
        NbtComponent mainData = main.get(DataComponentTypes.CUSTOM_DATA);
        NbtComponent offData = off.get(DataComponentTypes.CUSTOM_DATA);
        boolean changed = main != lastMainStack
                || off != lastOffStack
                || mainData != lastMainData
                || offData != lastOffData;
        lastMainStack = main;
        lastOffStack = off;
        lastMainData = mainData;
        lastOffData = offData;
        return changed;
    }

    private static void invalidateHeldCache() {
        lastMainStack = null;
        lastOffStack = null;
        lastMainData = null;
        lastOffData = null;
    }


    public static void render(WorldRenderContext context) {
        if (tool == null || first == null || context.world() == null) {
            return;
        }
        if (!context.world().getRegistryKey().getValue().toString().equals(dimensionId)) {
            return;
        }

        BlockPos end = hasSecond && second != null ? second : first;
        float red = valid ? 0.18F : 1.00F;
        float green = valid ? 0.92F : 0.16F;
        float blue = valid ? 0.32F : 0.12F;

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        RenderSystem.disableDepthTest();
        try {
            matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            RenderSystem.lineWidth(2.5F);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(
                    VertexFormat.DrawMode.DEBUG_LINES,
                    VertexFormats.POSITION_COLOR
            );

            BuildingWorldWireframeRenderer.drawSelectionOutline(
                    buffer, matrix, first, end, BOX_EXPAND, red, green, blue, 0.96F
            );
            BuildingWorldWireframeRenderer.drawBlockOutline(
                    buffer, matrix, first, CORNER_EXPAND, 0.18F, 0.78F, 1.0F, 1.0F
            );
            if (hasSecond && !first.equals(end)) {
                BuildingWorldWireframeRenderer.drawBlockOutline(
                        buffer, matrix, end, CORNER_EXPAND, 0.88F, 0.36F, 1.0F, 1.0F
                );
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            RenderSystem.enableDepthTest();
            matrices.pop();
        }
    }
}
