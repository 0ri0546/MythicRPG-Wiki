package com.mythicrpg.client.building.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mythicrpg.building.BuildingStructureRotation;
import com.mythicrpg.client.building.BlankBlockEntityRenderer;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.core.ModBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

/** Shared screen-space renderers used by Plan and Miniature screens. */
public final class BuildingStructurePreviewRenderer {
    private BuildingStructurePreviewRenderer() {
    }

    public static void render2D(
            DrawContext context,
            BuildingPreviewModel source,
            BuildingStructureRotation rotation,
            Direction.Axis sourceNormal,
            int x,
            int y,
            int width,
            int height
    ) {
        VanillaContainerUi.drawInsetPanel(context, x, y, width, height);
        BuildingPreviewModel model = safeModel(source).rotated(rotation);
        if (model.isEmpty()) {
            return;
        }

        BuildingStructureRotation safeRotation = rotation == null
                ? BuildingStructureRotation.NONE
                : rotation;
        Direction.Axis normal = safeRotation.rotateAxis(
                sourceNormal == null ? Direction.Axis.Z : sourceNormal
        );
        int cellsU = switch (normal) {
            case X -> model.sizeZ();
            case Y, Z -> model.sizeX();
        };
        int cellsV = switch (normal) {
            case X, Z -> model.sizeY();
            case Y -> model.sizeZ();
        };

        float cell = Math.min(
                (width - 12.0F) / Math.max(1, cellsU),
                (height - 12.0F) / Math.max(1, cellsV)
        );
        cell = Math.max(2.0F, Math.min(16.0F, cell));
        float contentWidth = cellsU * cell;
        float contentHeight = cellsV * cell;
        float startX = x + (width - contentWidth) / 2.0F;
        float startY = y + (height - contentHeight) / 2.0F;

        for (BuildingPreviewModel.Entry entry : model.entries()) {
            int u = switch (normal) {
                case X -> entry.z();
                case Y, Z -> entry.x();
            };
            int v = switch (normal) {
                case X, Z -> model.sizeY() - 1 - entry.y();
                case Y -> entry.z();
            };
            int drawX = Math.round(startX + u * cell);
            int drawY = Math.round(startY + v * cell);
            int cellSize = Math.max(1, Math.round(cell));
            context.fill(
                    drawX,
                    drawY,
                    drawX + cellSize,
                    drawY + cellSize,
                    0x55000000
            );

            ItemStack stack = new ItemStack(entry.state().getBlock().asItem());
            if (stack.isEmpty()) {
                continue;
            }
            context.getMatrices().push();
            context.getMatrices().translate(drawX, drawY, 100.0F);
            float iconScale = cell / 16.0F;
            context.getMatrices().scale(iconScale, iconScale, 1.0F);
            context.drawItem(stack, 0, 0);
            context.getMatrices().pop();
        }
    }

    public static void render3D(
            DrawContext context,
            BuildingPreviewModel source,
            BuildingStructureRotation rotation,
            int x,
            int y,
            int width,
            int height,
            float automaticYawDegrees
    ) {
        VanillaContainerUi.drawInsetPanel(context, x, y, width, height);
        BuildingPreviewModel model = safeModel(source).rotated(rotation);
        if (model.isEmpty()) {
            return;
        }

        int innerX = x + 3;
        int innerY = y + 3;
        int innerWidth = Math.max(1, width - 6);
        int innerHeight = Math.max(1, height - 6);
        float largest = Math.max(model.sizeX(), Math.max(model.sizeY(), model.sizeZ()));
        float scale = Math.min(innerWidth, innerHeight) / Math.max(2.0F, largest * 2.25F);

        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate consumers =
                client.getBufferBuilders().getEntityVertexConsumers();

        context.enableScissor(innerX, innerY, innerX + innerWidth, innerY + innerHeight);
        RenderSystem.enableDepthTest();
        context.getMatrices().push();
        try {
            context.getMatrices().translate(
                    innerX + innerWidth / 2.0F,
                    innerY + innerHeight * 0.68F,
                    180.0F
            );
            context.getMatrices().scale(scale, -scale, scale);
            context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(24.0F));
            context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(automaticYawDegrees));
            context.getMatrices().translate(
                    -model.sizeX() / 2.0F,
                    -model.sizeY() / 2.0F,
                    -model.sizeZ() / 2.0F
            );

            for (BuildingPreviewModel.Entry entry : model.visibleEntries()) {
                context.getMatrices().push();
                try {
                    context.getMatrices().translate(entry.x(), entry.y(), entry.z());
                    if (entry.state().isOf(ModBlocks.BLANK_BLOCK)) {
                        BlankBlockEntityRenderer.renderAppearance(
                                entry.appearance(),
                                context.getMatrices(),
                                consumers,
                                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                                OverlayTexture.DEFAULT_UV
                        );
                    } else {
                        client.getBlockRenderManager().renderBlockAsEntity(
                                entry.state(),
                                context.getMatrices(),
                                consumers,
                                LightmapTextureManager.MAX_LIGHT_COORDINATE,
                                OverlayTexture.DEFAULT_UV
                        );
                    }
                } finally {
                    context.getMatrices().pop();
                }
            }
            consumers.draw();
        } finally {
            context.getMatrices().pop();
            RenderSystem.disableDepthTest();
            context.disableScissor();
        }
    }

    /**
     * Renders a miniature as a rigid model. The selected blocks keep their exact
     * BlockStates while the complete block geometry is rotated around X/Y/Z.
     * This allows decorative tilts that cannot exist as real placed blocks.
     */
    public static void renderMiniature3D(
            DrawContext context,
            BuildingPreviewModel source,
            BuildingStructureRotation rotation,
            int x,
            int y,
            int width,
            int height,
            float automaticYawDegrees
    ) {
        VanillaContainerUi.drawInsetPanel(context, x, y, width, height);
        BuildingPreviewModel model = safeModel(source);
        if (model.isEmpty()) {
            return;
        }
        BuildingStructureRotation safeRotation = rotation == null
                ? BuildingStructureRotation.NONE
                : rotation;
        BuildingStructureRotation.Size rotatedSize = safeRotation.rotatedSize(
                model.sizeX(),
                model.sizeY(),
                model.sizeZ()
        );

        int innerX = x + 3;
        int innerY = y + 3;
        int innerWidth = Math.max(1, width - 6);
        int innerHeight = Math.max(1, height - 6);
        float largest = Math.max(rotatedSize.x(), Math.max(rotatedSize.y(), rotatedSize.z()));
        float scale = Math.min(innerWidth, innerHeight) / Math.max(2.0F, largest * 2.25F);

        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate consumers =
                client.getBufferBuilders().getEntityVertexConsumers();

        context.enableScissor(innerX, innerY, innerX + innerWidth, innerY + innerHeight);
        RenderSystem.enableDepthTest();
        context.getMatrices().push();
        try {
            context.getMatrices().translate(
                    innerX + innerWidth / 2.0F,
                    innerY + innerHeight * 0.68F,
                    180.0F
            );
            context.getMatrices().scale(scale, -scale, scale);
            context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(24.0F));
            context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(automaticYawDegrees));
            context.getMatrices().translate(
                    -rotatedSize.x() / 2.0F,
                    -rotatedSize.y() / 2.0F,
                    -rotatedSize.z() / 2.0F
            );

            for (BuildingPreviewModel.Entry entry : model.visibleEntries()) {
                net.minecraft.util.math.BlockPos offset = safeRotation.rotateOffset(
                        new net.minecraft.util.math.BlockPos(entry.x(), entry.y(), entry.z()),
                        model.sizeX(),
                        model.sizeY(),
                        model.sizeZ()
                );
                context.getMatrices().push();
                try {
                    context.getMatrices().translate(offset.getX(), offset.getY(), offset.getZ());
                    applyRigidBlockRotation(context, safeRotation);
                    client.getBlockRenderManager().renderBlockAsEntity(
                            entry.state(),
                            context.getMatrices(),
                            consumers,
                            LightmapTextureManager.MAX_LIGHT_COORDINATE,
                            OverlayTexture.DEFAULT_UV
                    );
                } finally {
                    context.getMatrices().pop();
                }
            }
            consumers.draw();
        } finally {
            context.getMatrices().pop();
            RenderSystem.disableDepthTest();
            context.disableScissor();
        }
    }

    private static void applyRigidBlockRotation(
            DrawContext context,
            BuildingStructureRotation rotation
    ) {
        if (rotation.equals(BuildingStructureRotation.NONE)) {
            return;
        }
        context.getMatrices().translate(0.5F, 0.5F, 0.5F);
        // MatrixStack post-multiplies: reverse the logical X -> Y -> Z order.
        if (rotation.zQuarterTurns() != 0) {
            context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    rotation.zQuarterTurns() * 90.0F
            ));
        }
        if (rotation.yQuarterTurns() != 0) {
            context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                    rotation.yQuarterTurns() * 90.0F
            ));
        }
        if (rotation.xQuarterTurns() != 0) {
            context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    rotation.xQuarterTurns() * 90.0F
            ));
        }
        context.getMatrices().translate(-0.5F, -0.5F, -0.5F);
    }

    private static BuildingPreviewModel safeModel(BuildingPreviewModel model) {
        return model == null ? BuildingPreviewModel.EMPTY : model;
    }
}
