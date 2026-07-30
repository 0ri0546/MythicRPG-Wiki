package com.mythicrpg.client.traveling;

import com.mythicrpg.traveling.GrapplingHookConfig;
import com.mythicrpg.traveling.GrapplingHookVisualPayload;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/** Client-side continuous rope renderer for the Grappling Hook. */
public final class GrapplingHookClient {
    private static final int ROPE_SEGMENTS = 24;
    private static final float ROPE_HALF_WIDTH = 0.0125F;
    private static final Map<Integer, VisualGrapple> ACTIVE = new HashMap<>();
    private static ClientWorld activeWorld;

    private GrapplingHookClient() {
    }

    public static void handle(GrapplingHookVisualPayload payload) {
        if (payload.action() == GrapplingHookVisualPayload.STOP) {
            ACTIVE.remove(payload.playerEntityId());
            return;
        }

        ACTIVE.put(
                payload.playerEntityId(),
                new VisualGrapple(
                        payload.action(),
                        payload.startedAtWorldTick(),
                        payload.launch(),
                        payload.anchor(),
                        payload.destination()
                )
        );
    }

    public static void tick(MinecraftClient client) {
        if (client.world != activeWorld) {
            ACTIVE.clear();
            activeWorld = client.world;
        }
    }

    public static void render(WorldRenderContext context) {
        if (ACTIVE.isEmpty() || context.world() == null || context.consumers() == null) {
            return;
        }

        float tickDelta = context.tickCounter().getTickDelta(false);
        Vec3d cameraPos = context.camera().getPos();
        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();

        for (Map.Entry<Integer, VisualGrapple> entry : ACTIVE.entrySet()) {
            Entity player = context.world().getEntityById(entry.getKey());
            if (!(player instanceof PlayerEntity)) {
                continue;
            }

            VisualGrapple grapple = entry.getValue();
            Vec3d ropeStart = getRopeStart(player, context, tickDelta);
            Vec3d ropeEnd = grapple.getVisibleTip(context.world().getTime(), tickDelta);

            if (ropeStart.squaredDistanceTo(ropeEnd) < 1.0E-4D) {
                continue;
            }

            renderRope(
                    context,
                    matrices,
                    consumers,
                    cameraPos,
                    ropeStart,
                    ropeEnd
            );
        }
    }

    private static Vec3d getRopeStart(
            Entity entity,
            WorldRenderContext context,
            float tickDelta
    ) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (entity == client.player && client.options.getPerspective().isFirstPerson()) {
            Vec3d look = entity.getRotationVec(tickDelta);
            Vec3d right = look.crossProduct(new Vec3d(0.0D, 1.0D, 0.0D));
            if (right.lengthSquared() > 1.0E-6D) {
                right = right.normalize();
            }

            double handSign = client.player.getMainArm() == Arm.RIGHT ? -1.0D : 1.0D;
            return context.camera().getPos()
                    .add(look.multiply(0.34D))
                    .add(right.multiply(0.26D * handSign))
                    .add(0.0D, -0.24D, 0.0D);
        }

        return entity.getLeashPos(tickDelta);
    }

    private static void renderRope(
            WorldRenderContext context,
            MatrixStack matrices,
            VertexConsumerProvider consumers,
            Vec3d cameraPos,
            Vec3d start,
            Vec3d end
    ) {
        float dx = (float) (end.x - start.x);
        float dy = (float) (end.y - start.y);
        float dz = (float) (end.z - start.z);
        float horizontalLengthSquared = dx * dx + dz * dz;

        if (horizontalLengthSquared < 1.0E-8F) {
            horizontalLengthSquared = 1.0E-8F;
        }

        float perpendicularScale = MathHelper.inverseSqrt(horizontalLengthSquared)
                * ROPE_HALF_WIDTH;
        float offsetX = dz * perpendicularScale;
        float offsetZ = dx * perpendicularScale;
        float totalLength = (float) start.distanceTo(end);
        float sag = Math.min(0.38F, totalLength * 0.025F);

        BlockPos startPos = BlockPos.ofFloored(start);
        BlockPos endPos = BlockPos.ofFloored(end);
        int startBlockLight = context.world().getLightLevel(LightType.BLOCK, startPos);
        int endBlockLight = context.world().getLightLevel(LightType.BLOCK, endPos);
        int startSkyLight = context.world().getLightLevel(LightType.SKY, startPos);
        int endSkyLight = context.world().getLightLevel(LightType.SKY, endPos);

        matrices.push();
        matrices.translate(
                start.x - cameraPos.x,
                start.y - cameraPos.y,
                start.z - cameraPos.z
        );

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer consumer = consumers.getBuffer(RenderLayer.getLeash());

        for (int segment = 0; segment <= ROPE_SEGMENTS; segment++) {
            renderRopeSegment(
                    consumer,
                    matrix,
                    dx,
                    dy,
                    dz,
                    startBlockLight,
                    endBlockLight,
                    startSkyLight,
                    endSkyLight,
                    0.025F,
                    0.025F,
                    offsetX,
                    offsetZ,
                    sag,
                    segment,
                    false
            );
        }

        for (int segment = ROPE_SEGMENTS; segment >= 0; segment--) {
            renderRopeSegment(
                    consumer,
                    matrix,
                    dx,
                    dy,
                    dz,
                    startBlockLight,
                    endBlockLight,
                    startSkyLight,
                    endSkyLight,
                    0.025F,
                    0.0F,
                    offsetX,
                    offsetZ,
                    sag,
                    segment,
                    true
            );
        }

        matrices.pop();
    }

    private static void renderRopeSegment(
            VertexConsumer consumer,
            Matrix4f matrix,
            float dx,
            float dy,
            float dz,
            int startBlockLight,
            int endBlockLight,
            int startSkyLight,
            int endSkyLight,
            float verticalOffsetA,
            float verticalOffsetB,
            float offsetX,
            float offsetZ,
            float sag,
            int segment,
            boolean reverse
    ) {
        float progress = segment / (float) ROPE_SEGMENTS;
        int blockLight = (int) MathHelper.lerp(progress, startBlockLight, endBlockLight);
        int skyLight = (int) MathHelper.lerp(progress, startSkyLight, endSkyLight);
        int packedLight = LightmapTextureManager.pack(blockLight, skyLight);

        float alternatingShade = segment % 2 == (reverse ? 1 : 0) ? 0.70F : 1.0F;
        float red = 0.50F * alternatingShade;
        float green = 0.40F * alternatingShade;
        float blue = 0.30F * alternatingShade;

        float x = dx * progress;
        float y = dy * progress - MathHelper.sin(progress * (float) Math.PI) * sag;
        float z = dz * progress;

        consumer.vertex(matrix, x - offsetZ, y + verticalOffsetB, z + offsetX)
                .color(red, green, blue, 1.0F)
                .light(packedLight);
        consumer.vertex(matrix, x + offsetZ, y + verticalOffsetA - verticalOffsetB, z - offsetX)
                .color(red, green, blue, 1.0F)
                .light(packedLight);
    }

    private record VisualGrapple(
            int action,
            long startedAtWorldTick,
            Vec3d launch,
            Vec3d anchor,
            Vec3d destination
    ) {
        private Vec3d getVisibleTip(long worldTick, float tickDelta) {
            if (action == GrapplingHookVisualPayload.START_PULLING) {
                return anchor;
            }

            Vec3d path = anchor.subtract(launch);
            double pathLength = path.length();
            if (pathLength <= 1.0E-6D) {
                return anchor;
            }

            double elapsedTicks = Math.max(
                    0.0D,
                    worldTick + tickDelta - startedAtWorldTick
            );
            double travelled = elapsedTicks
                    * GrapplingHookConfig.TRAVEL_SPEED_BLOCKS_PER_TICK;
            double progress = Math.min(1.0D, travelled / pathLength);
            return launch.lerp(anchor, progress);
        }
    }
}
