package com.mythicrpg.client.building;

import com.mythicrpg.MythicRPG;
import com.mythicrpg.building.BlankBlockAppearance;
import com.mythicrpg.building.BlankBlockEntity;
import com.mythicrpg.building.BlankBlockMaterialRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dessine directement les six faces du Blank Block.
 *
 * Le modèle statique du bloc est invisible dans le monde afin d'éviter
 * que sa base blanche reste visible entre les faces configurées.
 */
public final class BlankBlockEntityRenderer
        implements BlockEntityRenderer<BlankBlockEntity> {

    private static final int MAX_CACHE_ENTRIES = 1024;

    private static final Map<BlankBlockAppearance, FaceQuads> CACHE =
            new LinkedHashMap<>(128, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<BlankBlockAppearance, FaceQuads> eldest
                ) {
                    return size() > MAX_CACHE_ENTRIES;
                }
            };

    private static final Map<BlankBlockEntity, LightCache> LIGHT_CACHE = new WeakHashMap<>();

    public BlankBlockEntityRenderer(
            BlockEntityRendererFactory.Context context
    ) {
    }

    public static void registerReloadListener() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
                .registerReloadListener(
                        new SimpleSynchronousResourceReloadListener() {
                            @Override
                            public Identifier getFabricId() {
                                return Identifier.of(
                                        MythicRPG.MOD_ID,
                                        "blank_block_render_cache"
                                );
                            }

                            @Override
                            public void reload(ResourceManager manager) {
                                synchronized (CACHE) {
                                    CACHE.clear();
                                }
                                synchronized (LIGHT_CACHE) {
                                    LIGHT_CACHE.clear();
                                }
                            }
                        }
                );
    }

    @Override
    public void render(
            BlankBlockEntity blockEntity,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        BlankBlockAppearance appearance = blockEntity.appearance();

        FaceQuads cached;
        synchronized (CACHE) {
            cached = CACHE.computeIfAbsent(
                    appearance,
                    BlankBlockEntityRenderer::resolveQuads
            );
        }

        VertexConsumer consumer =
                vertexConsumers.getBuffer(RenderLayer.getSolid());

        World world = blockEntity.getWorld();
        BlockPos blockPos = blockEntity.getPos();
        MatrixStack.Entry matrix = matrices.peek();

        LightCache lightCache = null;
        if (world != null) {
            synchronized (LIGHT_CACHE) {
                lightCache = LIGHT_CACHE.computeIfAbsent(blockEntity, ignored -> new LightCache());
                lightCache.beginTick(world.getTime());
            }
        }

        for (Direction face : Direction.values()) {
            List<BakedQuad> quads = cached.quads(face);
            if (quads.isEmpty()) {
                continue;
            }

            BlockPos neighborPos = blockPos.offset(face);
            if (world != null && lightCache != null && !lightCache.shouldDraw(
                    blockEntity.getCachedState(),
                    world,
                    blockPos,
                    neighborPos,
                    face
            )) {
                continue;
            }

            int faceLight = light;
            if (world != null && lightCache != null) {
                faceLight = lightCache.getOrCompute(world, neighborPos, face, light);
            }

            float shade = shade(face);

            for (BakedQuad quad : quads) {
                consumer.quad(
                        matrix,
                        quad,
                        shade,
                        shade,
                        shade,
                        1.0F,
                        faceLight,
                        overlay
                );
            }
        }
    }

    /** Renders one configured Blank Block outside a world BlockEntity, for UI previews. */
    public static void renderAppearance(
            BlankBlockAppearance appearance,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        BlankBlockAppearance safe = appearance == null ? BlankBlockAppearance.EMPTY : appearance;
        FaceQuads cached;
        synchronized (CACHE) {
            cached = CACHE.computeIfAbsent(safe, BlankBlockEntityRenderer::resolveQuads);
        }

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getSolid());
        MatrixStack.Entry matrix = matrices.peek();
        for (Direction face : Direction.values()) {
            float shade = shade(face);
            for (BakedQuad quad : cached.quads(face)) {
                consumer.quad(
                        matrix,
                        quad,
                        shade,
                        shade,
                        shade,
                        1.0F,
                        light,
                        overlay
                );
            }
        }
    }

    private static FaceQuads resolveQuads(
            BlankBlockAppearance appearance
    ) {
        EnumMap<Direction, List<BakedQuad>> quadsByFace =
                new EnumMap<>(Direction.class);

        MinecraftClient client = MinecraftClient.getInstance();

        for (Direction face : Direction.values()) {
            Identifier materialId = appearance.material(face);

            /*
             * Une face sans matériau reste blanche.
             * Elle est rendue ici plutôt que par un second cube placé dessous.
             */
            Block materialBlock = materialId == null
                    ? Blocks.WHITE_CONCRETE
                    : BlankBlockMaterialRegistry.resolve(materialId)
                    .orElse(Blocks.WHITE_CONCRETE);

            BlockState materialState = materialBlock.getDefaultState();

            BakedModel model = client
                    .getBlockRenderManager()
                    .getModel(materialState);

            List<BakedQuad> quads = model.getQuads(
                    materialState,
                    face,
                    Random.create(0L)
            );

            quadsByFace.put(face, List.copyOf(quads));
        }

        return new FaceQuads(quadsByFace);
    }

    private static float shade(Direction face) {
        return switch (face) {
            case DOWN -> 0.50F;
            case UP -> 1.00F;
            case NORTH, SOUTH -> 0.80F;
            case WEST, EAST -> 0.60F;
        };
    }

    private static final class LightCache {
        private final int[] values = new int[Direction.values().length];
        private final boolean[] computed = new boolean[Direction.values().length];
        private final boolean[] visible = new boolean[Direction.values().length];
        private final boolean[] visibilityComputed = new boolean[Direction.values().length];
        private long worldTick = Long.MIN_VALUE;

        void beginTick(long tick) {
            if (worldTick == tick) {
                return;
            }
            worldTick = tick;
            java.util.Arrays.fill(computed, false);
            java.util.Arrays.fill(visibilityComputed, false);
        }

        boolean shouldDraw(
                BlockState state,
                World world,
                BlockPos blockPos,
                BlockPos neighborPos,
                Direction face
        ) {
            int index = face.ordinal();
            if (!visibilityComputed[index]) {
                visible[index] = Block.shouldDrawSide(
                        state,
                        world,
                        blockPos,
                        face,
                        neighborPos
                );
                visibilityComputed[index] = true;
            }
            return visible[index];
        }

        int getOrCompute(World world, BlockPos neighborPos, Direction face, int fallback) {
            int index = face.ordinal();
            if (!computed[index]) {
                values[index] = world == null
                        ? fallback
                        : WorldRenderer.getLightmapCoordinates(world, neighborPos);
                computed[index] = true;
            }
            return values[index];
        }
    }

    private record FaceQuads(
            EnumMap<Direction, List<BakedQuad>> values
    ) {
        private List<BakedQuad> quads(Direction face) {
            return values.getOrDefault(face, List.of());
        }
    }
}