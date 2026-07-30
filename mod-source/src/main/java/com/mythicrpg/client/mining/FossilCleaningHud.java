package com.mythicrpg.client.mining;

import com.mythicrpg.mining.archaeology.FossilBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.joml.Vector3f;

/** Small progress bar rendered below the crosshair while brushing a fossil. */
public final class FossilCleaningHud {

    private static final int BAR_WIDTH = 54;
    private static final int BAR_HEIGHT = 6;

    private FossilCleaningHud() {
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null
                || client.world == null
                || client.options.hudHidden
                || client.currentScreen != null
                || !client.player.isUsingItem()
                || !client.player.getActiveItem().isOf(Items.BRUSH)
                || !(client.crosshairTarget instanceof BlockHitResult blockHit)
                || blockHit.getType() != HitResult.Type.BLOCK
                || !(client.world.getBlockEntity(blockHit.getBlockPos()) instanceof FossilBlockEntity fossil)) {
            return;
        }

        int required = Math.max(1, fossil.requiredCleaningTicks());
        float progress = Math.min(1.0F, fossil.cleaningProgressTicks() / (float) required);
        int filled = Math.round((BAR_WIDTH - 2) * progress);
        int x = client.getWindow().getScaledWidth() / 2 - BAR_WIDTH / 2;
        int y = client.getWindow().getScaledHeight() / 2 + 11;

        Vector3f rarityColor = fossil.rarity().particleColor();
        int red = Math.max(0, Math.min(255, Math.round(rarityColor.x() * 255.0F)));
        int green = Math.max(0, Math.min(255, Math.round(rarityColor.y() * 255.0F)));
        int blue = Math.max(0, Math.min(255, Math.round(rarityColor.z() * 255.0F)));
        int fillColor = 0xFF000000 | red << 16 | green << 8 | blue;

        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0xB0000000);
        context.drawBorder(x, y, BAR_WIDTH, BAR_HEIGHT, 0xFFD0D0D0);
        if (filled > 0) {
            context.fill(x + 1, y + 1, x + 1 + filled, y + BAR_HEIGHT - 1, fillColor);
        }
    }
}
