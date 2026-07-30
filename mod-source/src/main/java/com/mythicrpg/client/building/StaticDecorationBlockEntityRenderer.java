package com.mythicrpg.client.building;

import com.mythicrpg.building.StaticDecorationBlockEntity;
import com.mythicrpg.building.StaticDecorationEffect;
import com.mythicrpg.client.MythicClientPreferences;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.random.Random;

/** Emits real vanilla particles from an invisible, non-ticking 1x1x1 anchor. */
public final class StaticDecorationBlockEntityRenderer
        implements BlockEntityRenderer<StaticDecorationBlockEntity> {

    public StaticDecorationBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(
            StaticDecorationBlockEntity blockEntity,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay
    ) {
        if (!MythicClientPreferences.areStaticDecorationsEnabled()
                || !(blockEntity.getWorld() instanceof ClientWorld world)) {
            return;
        }

        StaticDecorationEffect effect = blockEntity.effect();
        long tick = world.getTime();
        if (!blockEntity.shouldEmitClientParticle(tick, effect.intervalTicks())
                || !StaticDecorationRenderBudget.tryAcquire()) {
            return;
        }

        Random random = world.random;
        for (int index = 0; index < effect.particlesPerEmission(); index++) {
            emit(world, effect, blockEntity.getPos().getX() + 0.5D,
                    blockEntity.getPos().getY() + 0.5D,
                    blockEntity.getPos().getZ() + 0.5D,
                    random);
        }
    }

    private static void emit(
            ClientWorld world,
            StaticDecorationEffect effect,
            double centerX,
            double centerY,
            double centerZ,
            Random random
    ) {
        double x = centerX + (random.nextDouble() - 0.5D) * 0.62D;
        double y = centerY + (random.nextDouble() - 0.5D) * 0.62D;
        double z = centerZ + (random.nextDouble() - 0.5D) * 0.62D;
        double vx = (random.nextDouble() - 0.5D) * 0.012D;
        double vy = (random.nextDouble() - 0.35D) * 0.014D;
        double vz = (random.nextDouble() - 0.5D) * 0.012D;

        switch (effect) {
            case NOTE -> {
                // Vanilla NoteParticle interprets X speed as its note/color value.
                vx = random.nextDouble();
                vy = 0.0D;
                vz = 0.0D;
            }
            case EFFECT, INSTANT_EFFECT -> {
                // Vanilla spell particles use these parameters for their native tint/motion.
                vx = 0.25D + random.nextDouble() * 0.55D;
                vy = 0.25D + random.nextDouble() * 0.55D;
                vz = 0.25D + random.nextDouble() * 0.55D;
            }
            case ENCHANT -> {
                vx = (random.nextDouble() - 0.5D) * 0.45D;
                vy = (random.nextDouble() - 0.5D) * 0.45D;
                vz = (random.nextDouble() - 0.5D) * 0.45D;
            }
            case PORTAL, REVERSE_PORTAL -> {
                vx = (random.nextDouble() - 0.5D) * 0.18D;
                vy = (random.nextDouble() - 0.5D) * 0.18D;
                vz = (random.nextDouble() - 0.5D) * 0.18D;
            }
            default -> {
            }
        }
        world.addParticle(effect.particle(), x, y, z, vx, vy, vz);
    }

    @Override
    public int getRenderDistance() {
        return 32;
    }
}
