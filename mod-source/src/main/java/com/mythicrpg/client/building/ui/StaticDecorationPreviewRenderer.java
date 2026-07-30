package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.StaticDecorationEffect;
import com.mythicrpg.mixin.client.MinecraftClientParticleManagerAccessor;
import com.mythicrpg.mixin.client.ParticleManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.texture.Sprite;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Map;

/** Animated GUI preview using Minecraft's already-loaded vanilla particle sprites. */
public final class StaticDecorationPreviewRenderer {
    private StaticDecorationPreviewRenderer() {
    }

    public static void render(
            DrawContext context,
            StaticDecorationEffect effect,
            int x,
            int y,
            int width,
            int height,
            long ticks
    ) {
        int left = x + 8;
        int top = y + 18;
        int right = x + width - 8;
        int bottom = y + height - 12;

        // One-block guide: fixed 1x1x1 volume, not a configurable shape.
        int guide = 0x556A6A6A;
        context.drawBorder(left + 12, top + 10, right - left - 24, bottom - top - 22, guide);
        context.drawBorder(left + 20, top + 2, right - left - 24, bottom - top - 22, guide);
        context.drawHorizontalLine(left + 12, left + 20, top + 10, guide);
        context.drawHorizontalLine(right - 13, right - 5, top + 10, guide);
        context.drawHorizontalLine(left + 12, left + 20, bottom - 12, guide);
        context.drawHorizontalLine(right - 13, right - 5, bottom - 12, guide);

        SpriteProvider provider = spriteProvider(effect);
        if (provider == null) return;

        int centerX = x + width / 2;
        int centerY = y + height / 2 + 4;
        int usableWidth = Math.max(24, width - 46);
        int usableHeight = Math.max(24, height - 54);
        int count = previewCount(effect);
        int animationLength = 20;

        for (int index = 0; index < count; index++) {
            float phase = MathHelper.fractionalPart((ticks * 0.035F) + index * 0.173F);
            float angle = (ticks * 0.045F) + index * 2.39996F;
            float radius = 0.20F + 0.24F * (0.5F + 0.5F * MathHelper.sin(index * 1.73F));
            int px = centerX + Math.round(MathHelper.cos(angle) * usableWidth * radius);
            int py = centerY + Math.round((0.5F - phase) * usableHeight * 0.72F
                    + MathHelper.sin(angle * 0.7F) * 5.0F);
            int size = 8 + Math.floorMod(index * 3, 7);
            float life = 1.0F - Math.abs(phase * 2.0F - 1.0F);
            float alpha = 0.28F + 0.72F * life;
            int age = Math.floorMod((int) ticks + index * 3, animationLength);
            Sprite sprite = provider.getSprite(age, animationLength);
            if (sprite == null) continue;
            context.drawSprite(px - size / 2, py - size / 2, 0, size, size,
                    sprite, 1.0F, 1.0F, 1.0F, alpha);
        }
    }

    /** Kept for the screen lifecycle; providers themselves are owned by vanilla. */
    public static void clearCache() {
        // No local atlas or sprite cache anymore.
    }

    private static int previewCount(StaticDecorationEffect effect) {
        return switch (effect) {
            case CAMPFIRE_COSY_SMOKE, HEART, NOTE -> 7;
            case PORTAL, REVERSE_PORTAL, ENCHANT, SPORE_BLOSSOM_AIR -> 14;
            default -> 10;
        };
    }

    private static SpriteProvider spriteProvider(StaticDecorationEffect effect) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || effect == null) return null;

        ParticleManager particleManager =
                ((MinecraftClientParticleManagerAccessor) client).mythicrpg$getParticleManager();
        if (particleManager == null) return null;

        Map<Identifier, ?> providers =
                ((ParticleManagerAccessor) particleManager).mythicrpg$getSpriteAwareFactories();
        if (providers == null || providers.isEmpty()) return null;

        Identifier particleId = Registries.PARTICLE_TYPE.getId(effect.particle());
        Object provider = providers.get(particleId);
        return provider instanceof SpriteProvider spriteProvider ? spriteProvider : null;
    }
}
