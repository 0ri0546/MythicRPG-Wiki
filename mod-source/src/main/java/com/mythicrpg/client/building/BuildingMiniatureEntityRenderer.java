package com.mythicrpg.client.building;

import com.mythicrpg.building.BuildingMiniatureData;
import com.mythicrpg.building.BuildingMiniatureEntity;
import com.mythicrpg.building.BuildingStructureRotation;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.EmptyBlockView;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Renders a whole 5x5x5 project from one entity and one bounded parsed-data cache entry. */
public final class BuildingMiniatureEntityRenderer extends EntityRenderer<BuildingMiniatureEntity> {
    private static final int MAX_CACHE_ENTRIES = 256;
    private static final float BLOCK_SCALE = 0.16F;

    private static final Map<UUID, RenderData> CACHE =
            new LinkedHashMap<>(64, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, RenderData> eldest) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            };

    private final BlockRenderManager blockRenderManager;

    public BuildingMiniatureEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.blockRenderManager = context.getBlockRenderManager();
        this.shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(
            BuildingMiniatureEntity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ
    ) {
        return entity.squaredDistanceTo(cameraX, cameraY, cameraZ)
                <= 48.0D * 48.0D
                && super.shouldRender(
                entity,
                frustum,
                cameraX,
                cameraY,
                cameraZ
        );
    }

    @Override
    public void render(BuildingMiniatureEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        Optional<RenderData> optional = renderData(entity);
        if (optional.isEmpty()) return;
        RenderData data = optional.get();
        if (!BuildingMiniatureRenderBudget.tryAcquire(data.visibleEntries().size())) return;
        BuildingMiniatureData.Project project = data.project();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
        matrices.scale(BLOCK_SCALE, BLOCK_SCALE, BLOCK_SCALE);

        BuildingStructureRotation.Size rotatedSize = project.rotatedSize();
        double centerY = rotatedSize.y() / 2.0D;
        matrices.translate(0.0D, centerY + 0.08D, 0.0D);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.rollZDegrees()));
        matrices.translate(-rotatedSize.x() / 2.0D, -centerY, -rotatedSize.z() / 2.0D);

        for (RenderEntry entry : data.visibleEntries()) {
            matrices.push();
            matrices.translate(entry.x(), entry.y(), entry.z());
            applyProjectBlockRotation(matrices, project.rotation());
            blockRenderManager.renderBlockAsEntity(entry.state(), matrices, vertexConsumers, light,
                    net.minecraft.client.render.OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private static void applyProjectBlockRotation(
            MatrixStack matrices,
            BuildingStructureRotation rotation
    ) {
        if (rotation == null || rotation.equals(BuildingStructureRotation.NONE)) {
            return;
        }
        matrices.translate(0.5D, 0.5D, 0.5D);
        // MatrixStack post-multiplies: reverse the logical X -> Y -> Z order.
        if (rotation.zQuarterTurns() != 0) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(
                    rotation.zQuarterTurns() * 90.0F
            ));
        }
        if (rotation.yQuarterTurns() != 0) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                    rotation.yQuarterTurns() * 90.0F
            ));
        }
        if (rotation.xQuarterTurns() != 0) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(
                    rotation.xQuarterTurns() * 90.0F
            ));
        }
        matrices.translate(-0.5D, -0.5D, -0.5D);
    }

    private static Optional<RenderData> renderData(BuildingMiniatureEntity entity) {
        UUID entityId = entity.getUuid();
        synchronized (CACHE) {
            RenderData cached = CACHE.get(entityId);
            if (cached != null) return Optional.of(cached);
        }

        Optional<BuildingMiniatureData.Project> decoded = BuildingMiniatureData.readProject(entity.miniatureStack());
        if (decoded.isEmpty()) return Optional.empty();
        RenderData prepared = prepare(decoded.get());

        synchronized (CACHE) {
            RenderData cached = CACHE.get(entityId);
            if (cached != null) return Optional.of(cached);
            CACHE.put(entityId, prepared);
            return Optional.of(prepared);
        }
    }

    /** Removes only fully enclosed opaque cubes; partial and translucent blocks are always retained. */
    private static RenderData prepare(BuildingMiniatureData.Project project) {
        Map<Integer, BuildingMiniatureData.Entry> occupied = new HashMap<>();
        for (BuildingMiniatureData.Entry entry : project.entries()) {
            occupied.put(pack(entry.x(), entry.y(), entry.z()), entry);
        }

        List<RenderEntry> visible = project.entries().stream()
                .filter(entry -> !isFullyEnclosedOpaqueCube(entry, occupied, project))
                .map(entry -> {
                    BlockPos offset = project.rotation().rotateOffset(
                            new BlockPos(entry.x(), entry.y(), entry.z()),
                            project.sizeX(),
                            project.sizeY(),
                            project.sizeZ()
                    );
                    return new RenderEntry(
                            entry.state(),
                            offset.getX(),
                            offset.getY(),
                            offset.getZ()
                    );
                })
                .toList();
        return new RenderData(project, visible);
    }

    private static boolean isFullyEnclosedOpaqueCube(
            BuildingMiniatureData.Entry entry,
            Map<Integer, BuildingMiniatureData.Entry> occupied,
            BuildingMiniatureData.Project project
    ) {
        BlockPos origin = BlockPos.ORIGIN;
        if (!entry.state().isOpaqueFullCube(EmptyBlockView.INSTANCE, origin)) return false;
        for (Direction direction : Direction.values()) {
            int x = entry.x() + direction.getOffsetX();
            int y = entry.y() + direction.getOffsetY();
            int z = entry.z() + direction.getOffsetZ();
            if (x < 0 || y < 0 || z < 0
                    || x >= project.sizeX() || y >= project.sizeY() || z >= project.sizeZ()) {
                return false;
            }
            BuildingMiniatureData.Entry neighbor = occupied.get(pack(x, y, z));
            if (neighbor == null
                    || !neighbor.state().isOpaqueFullCube(EmptyBlockView.INSTANCE, origin)) {
                return false;
            }
        }
        return true;
    }

    private static int pack(int x, int y, int z) {
        return x + y * 5 + z * 25;
    }

    public static void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }

    private record RenderEntry(BlockState state, int x, int y, int z) {
    }

    private record RenderData(
            BuildingMiniatureData.Project project,
            List<RenderEntry> visibleEntries
    ) {
    }

    @Override
    public Identifier getTexture(BuildingMiniatureEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
