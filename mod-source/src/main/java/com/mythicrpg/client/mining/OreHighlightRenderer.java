package com.mythicrpg.client.mining;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mythicrpg.client.ClientSkillTreeState;
import com.mythicrpg.core.BonusType;
import com.mythicrpg.core.ModBlockTags;
import com.mythicrpg.core.SkillTreeManager;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class OreHighlightRenderer {
    private static final int RESCAN_INTERVAL_TICKS = 10;
    private static final float EXPAND = 0.02F;

    private static boolean enabled;
    private static int tickCounter;
    private static Map<BlockPos, BlockState> highlightedTargets = new HashMap<>();

    private OreHighlightRenderer() {
    }

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) {
            highlightedTargets = new HashMap<>();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void onClientTick(MinecraftClient client) {
        if (client.player == null || client.world == null || !enabled) {
            highlightedTargets = new HashMap<>();
            return;
        }

        tickCounter++;
        if (tickCounter < RESCAN_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        int radius = (int) SkillTreeManager.getBonusTotalFromUnlocked(
                SkillType.MINING,
                ClientSkillTreeState.getUnlockedIds(SkillType.MINING),
                BonusType.ORE_HIGHLIGHT_RADIUS
        );
        if (radius <= 0) {
            highlightedTargets = new HashMap<>();
            return;
        }

        ClientWorld world = client.world;
        BlockPos center = client.player.getBlockPos();
        int radiusSquared = radius * radius;
        Map<BlockPos, BlockState> found = new HashMap<>();

        for (BlockPos pos : BlockPos.iterate(
                center.add(-radius, -radius, -radius),
                center.add(radius, radius, radius)
        )) {
            int dx = pos.getX() - center.getX();
            int dy = pos.getY() - center.getY();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz > radiusSquared) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (state.isIn(ModBlockTags.ORES)) {
                found.put(pos.toImmutable(), state);
            }
        }
        highlightedTargets = found;
    }

    public static void onRenderWorld(WorldRenderContext context) {
        if (highlightedTargets.isEmpty()) {
            return;
        }

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

        for (Map.Entry<BlockPos, BlockState> entry : highlightedTargets.entrySet()) {
            float[] color = colorForBlock(entry.getValue());
            drawBoxOutline(buffer, matrix, entry.getKey(), color[0], color[1], color[2], 1.0F);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    private static float[] colorForBlock(BlockState state) {
        if (state.isIn(BlockTags.DIAMOND_ORES)) return new float[]{0.4F, 0.9F, 1.0F};
        if (state.isIn(BlockTags.EMERALD_ORES)) return new float[]{0.2F, 1.0F, 0.4F};
        if (state.getBlock() == Blocks.ANCIENT_DEBRIS) return new float[]{0.6F, 0.25F, 0.2F};
        if (state.isIn(BlockTags.REDSTONE_ORES)) return new float[]{1.0F, 0.15F, 0.15F};
        if (state.isIn(BlockTags.LAPIS_ORES)) return new float[]{0.2F, 0.3F, 1.0F};
        if (state.isIn(BlockTags.IRON_ORES)) return new float[]{0.85F, 0.7F, 0.55F};
        if (state.isIn(BlockTags.GOLD_ORES)) return new float[]{1.0F, 0.85F, 0.0F};
        if (state.isIn(BlockTags.COPPER_ORES)) return new float[]{0.8F, 0.45F, 0.2F};
        if (state.isIn(BlockTags.COAL_ORES)) return new float[]{0.5F, 0.5F, 0.5F};
        if (state.getBlock() == Blocks.NETHER_QUARTZ_ORE) return new float[]{0.9F, 0.9F, 0.85F};
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    private static void drawBoxOutline(
            BufferBuilder buffer, Matrix4f matrix, BlockPos pos,
            float r, float g, float b, float a
    ) {
        float minX = pos.getX() - EXPAND;
        float minY = pos.getY() - EXPAND;
        float minZ = pos.getZ() - EXPAND;
        float maxX = pos.getX() + 1 + EXPAND;
        float maxY = pos.getY() + 1 + EXPAND;
        float maxZ = pos.getZ() + 1 + EXPAND;

        edge(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        edge(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        edge(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        edge(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        edge(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        edge(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        edge(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        edge(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        edge(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        edge(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        edge(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        edge(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private static void edge(
            BufferBuilder buffer, Matrix4f matrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float r, float g, float b, float a
    ) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a);
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a);
    }
}
