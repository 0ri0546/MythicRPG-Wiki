package com.mythicrpg.client.building;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mythicrpg.building.BuildingPlan3DPreviewPayload;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/** Lightweight wireframe preview; no client tick scan and at most 512 boxes. */
public final class BuildingPlan3DClient {
    private static final float EXPAND = 0.0125F;

    private static ClientWorld activeWorld;
    private static String dimensionId = "";
    private static boolean valid;
    private static List<BlockPos> positions = List.of();

    private BuildingPlan3DClient() {
    }

    public static void handle(BuildingPlan3DPreviewPayload payload) {
        if (payload.action() == BuildingPlan3DPreviewPayload.CLEAR) {
            clear();
            return;
        }
        dimensionId = payload.dimensionId();
        valid = payload.valid();
        positions = payload.packedPositions().stream().map(BlockPos::fromLong).toList();
    }

    public static void tick(MinecraftClient client) {
        if (client.world != activeWorld) {
            clear();
            activeWorld = client.world;
        }
    }

    public static void clear() {
        dimensionId = "";
        valid = false;
        positions = List.of();
    }

    public static void render(WorldRenderContext context) {
        if (positions.isEmpty() || context.world() == null) {
            return;
        }
        if (!context.world().getRegistryKey().getValue().toString().equals(dimensionId)) {
            return;
        }

        float red = valid ? 0.20F : 1.00F;
        float green = valid ? 1.00F : 0.18F;
        float blue = valid ? 0.35F : 0.12F;

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(
                VertexFormat.DrawMode.DEBUG_LINES,
                VertexFormats.POSITION_COLOR
        );

        for (BlockPos pos : positions) {
            drawBoxOutline(buffer, matrix, pos, red, green, blue, 0.92F);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    private static void drawBoxOutline(
            BufferBuilder buffer,
            Matrix4f matrix,
            BlockPos pos,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float minX = pos.getX() - EXPAND;
        float minY = pos.getY() - EXPAND;
        float minZ = pos.getZ() - EXPAND;
        float maxX = pos.getX() + 1.0F + EXPAND;
        float maxY = pos.getY() + 1.0F + EXPAND;
        float maxZ = pos.getZ() + 1.0F + EXPAND;

        edge(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        edge(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, red, green, blue, alpha);
        edge(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        edge(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, red, green, blue, alpha);
        edge(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        edge(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
        edge(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
        edge(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
        edge(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, red, green, blue, alpha);
        edge(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, red, green, blue, alpha);
        edge(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, red, green, blue, alpha);
        edge(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void edge(
            BufferBuilder buffer,
            Matrix4f matrix,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
    }
}
