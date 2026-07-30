package com.mythicrpg.client.building;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mythicrpg.building.ArchitectCompassData;
import com.mythicrpg.client.ClientSkillTreeState;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillType;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Cached, static client-only circle guide for the Architect's Compass. */
public final class ArchitectCompassClient {
    private static final float EXPAND = 0.016F;
    private static ClientWorld activeWorld;
    private static CacheKey cacheKey;
    private static List<BlockPos> positions = List.of();
    private static ItemStack lastStack;
    private static NbtComponent lastData;

    private ArchitectCompassClient() {}

    public static void tick(MinecraftClient client) {
        if (client.world != activeWorld) {
            clear();
            activeWorld = client.world;
        }
        if (client.player == null || client.world == null) {
            clear();
            return;
        }
        if (!ClientSkillTreeState.isUnlocked(SkillType.BUILDING, 14)) {
            clearCacheOnly();
            return;
        }

        ItemStack stack = client.player.getMainHandStack().isOf(ModItems.ARCHITECT_COMPASS)
                ? client.player.getMainHandStack()
                : client.player.getOffHandStack().isOf(ModItems.ARCHITECT_COMPASS)
                        ? client.player.getOffHandStack()
                        : ItemStack.EMPTY;
        if (stack.isEmpty()) {
            clearCacheOnly();
            return;
        }
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (stack == lastStack && data == lastData) {
            return;
        }
        lastStack = stack;
        lastData = data;

        ArchitectCompassData.State state = ArchitectCompassData.read(stack);
        String dimensionId = client.world.getRegistryKey().getValue().toString();
        if (!state.hasCenter() || !dimensionId.equals(state.dimensionId())) {
            clearCacheOnly();
            return;
        }

        CacheKey next = new CacheKey(
                dimensionId,
                state.center().asLong(),
                state.radius(),
                state.plane()
        );
        if (!Objects.equals(next, cacheKey)) {
            cacheKey = next;
            positions = buildCircle(state.center(), state.radius(), state.plane());
        }
    }

    public static void clear() {
        activeWorld = null;
        clearCacheOnly();
    }

    private static void clearCacheOnly() {
        cacheKey = null;
        positions = List.of();
        lastStack = null;
        lastData = null;
    }

    private static List<BlockPos> buildCircle(
            BlockPos center,
            int radius,
            ArchitectCompassData.Plane plane
    ) {
        Set<Long> packed = new LinkedHashSet<>();
        int x = radius;
        int y = 0;
        int decision = 1 - radius;
        while (x >= y) {
            addSymmetric(packed, center, plane, x, y);
            y++;
            if (decision <= 0) {
                decision += 2 * y + 1;
            } else {
                x--;
                decision += 2 * (y - x) + 1;
            }
        }
        return packed.stream().map(BlockPos::fromLong).toList();
    }

    private static void addSymmetric(
            Set<Long> packed,
            BlockPos center,
            ArchitectCompassData.Plane plane,
            int a,
            int b
    ) {
        add(packed, center, plane, a, b);
        add(packed, center, plane, b, a);
        add(packed, center, plane, -b, a);
        add(packed, center, plane, -a, b);
        add(packed, center, plane, -a, -b);
        add(packed, center, plane, -b, -a);
        add(packed, center, plane, b, -a);
        add(packed, center, plane, a, -b);
    }

    private static void add(
            Set<Long> packed,
            BlockPos center,
            ArchitectCompassData.Plane plane,
            int a,
            int b
    ) {
        BlockPos pos = switch (plane) {
            case HORIZONTAL -> center.add(a, 0, b);
            case VERTICAL_X -> center.add(0, a, b);
            case VERTICAL_Z -> center.add(a, b, 0);
        };
        packed.add(pos.asLong());
    }

    public static void render(WorldRenderContext context) {
        if (positions.isEmpty() || context.world() == null || cacheKey == null) return;
        if (!context.world().getRegistryKey().getValue().toString().equals(cacheKey.dimensionId())) return;

        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2.0F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BlockPos pos : positions) drawBoxOutline(buffer, matrix, pos, 0.22F, 0.82F, 1.0F, 0.9F);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    private static void drawBoxOutline(
            BufferBuilder buffer, Matrix4f matrix, BlockPos pos,
            float red, float green, float blue, float alpha
    ) {
        float minX = pos.getX() - EXPAND;
        float minY = pos.getY() - EXPAND;
        float minZ = pos.getZ() - EXPAND;
        float maxX = pos.getX() + 1.0F + EXPAND;
        float maxY = pos.getY() + 1.0F + EXPAND;
        float maxZ = pos.getZ() + 1.0F + EXPAND;
        edge(buffer, matrix, minX,minY,minZ, maxX,minY,minZ, red,green,blue,alpha);
        edge(buffer, matrix, maxX,minY,minZ, maxX,minY,maxZ, red,green,blue,alpha);
        edge(buffer, matrix, maxX,minY,maxZ, minX,minY,maxZ, red,green,blue,alpha);
        edge(buffer, matrix, minX,minY,maxZ, minX,minY,minZ, red,green,blue,alpha);
        edge(buffer, matrix, minX,maxY,minZ, maxX,maxY,minZ, red,green,blue,alpha);
        edge(buffer, matrix, maxX,maxY,minZ, maxX,maxY,maxZ, red,green,blue,alpha);
        edge(buffer, matrix, maxX,maxY,maxZ, minX,maxY,maxZ, red,green,blue,alpha);
        edge(buffer, matrix, minX,maxY,maxZ, minX,maxY,minZ, red,green,blue,alpha);
        edge(buffer, matrix, minX,minY,minZ, minX,maxY,minZ, red,green,blue,alpha);
        edge(buffer, matrix, maxX,minY,minZ, maxX,maxY,minZ, red,green,blue,alpha);
        edge(buffer, matrix, maxX,minY,maxZ, maxX,maxY,maxZ, red,green,blue,alpha);
        edge(buffer, matrix, minX,minY,maxZ, minX,maxY,maxZ, red,green,blue,alpha);
    }

    private static void edge(
            BufferBuilder buffer, Matrix4f matrix,
            float x1,float y1,float z1,float x2,float y2,float z2,
            float red,float green,float blue,float alpha
    ) {
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha);
    }

    private record CacheKey(
            String dimensionId,
            long center,
            int radius,
            ArchitectCompassData.Plane plane
    ) {}
}
