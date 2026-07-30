package com.mythicrpg.client.building;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

/** Shared line geometry for Building world previews and selection boxes. */
public final class BuildingWorldWireframeRenderer {
    private BuildingWorldWireframeRenderer() {
    }

    public static void drawBlockOutline(
            BufferBuilder buffer,
            Matrix4f matrix,
            BlockPos pos,
            float expand,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        drawCuboidOutline(
                buffer,
                matrix,
                pos.getX() - expand,
                pos.getY() - expand,
                pos.getZ() - expand,
                pos.getX() + 1.0F + expand,
                pos.getY() + 1.0F + expand,
                pos.getZ() + 1.0F + expand,
                red,
                green,
                blue,
                alpha
        );
    }

    public static void drawSelectionOutline(
            BufferBuilder buffer,
            Matrix4f matrix,
            BlockPos first,
            BlockPos second,
            float expand,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        int minX = Math.min(first.getX(), second.getX());
        int minY = Math.min(first.getY(), second.getY());
        int minZ = Math.min(first.getZ(), second.getZ());
        int maxX = Math.max(first.getX(), second.getX()) + 1;
        int maxY = Math.max(first.getY(), second.getY()) + 1;
        int maxZ = Math.max(first.getZ(), second.getZ()) + 1;
        drawCuboidOutline(
                buffer,
                matrix,
                minX - expand,
                minY - expand,
                minZ - expand,
                maxX + expand,
                maxY + expand,
                maxZ + expand,
                red,
                green,
                blue,
                alpha
        );
    }

    public static void drawCuboidOutline(
            BufferBuilder buffer,
            Matrix4f matrix,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float red,
            float green,
            float blue,
            float alpha
    ) {
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
