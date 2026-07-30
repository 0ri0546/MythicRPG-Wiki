package com.mythicrpg.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Shared vanilla container visual language for MythicRPG screens.
 *
 * <p>The panel border below reproduces the four-pixel frame used by vanilla
 * container textures. Slots use Minecraft's own {@code container/slot} GUI
 * sprite, so every screen shares the exact same pixels rather than a close
 * approximation copied into several classes.</p>
 */
public final class VanillaContainerUi {

    private static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");

    public static final int TRANSPARENT = 0x00000000;
    public static final int BLACK = 0xFF000000;
    public static final int BACKGROUND = 0xFFC6C6C6;
    public static final int HIGHLIGHT = 0xFFFFFFFF;
    public static final int SHADOW = 0xFF8B8B8B;
    public static final int DARK_SHADOW = 0xFF373737;
    public static final int OUTLINE = 0xFF555555;
    public static final int TEXT = 0xFF404040;
    public static final int DISABLED_TEXT = 0xFFA0A0A0;
    public static final int SLOT_INTERIOR = SHADOW;

    public static final float SMALL_TEXT_SCALE = 0.75F;
    private static final float GHOST_RED = 0.72F;
    private static final float GHOST_GREEN = 0.72F;
    private static final float GHOST_BLUE = 0.72F;
    private static final float GHOST_ALPHA = 0.30F;

    private VanillaContainerUi() {
    }

    /** Draws the pixel-exact outer frame and flat body of a vanilla container. */
    public static void drawPanel(DrawContext context, int x, int y, int width, int height) {
        if (width < 8 || height < 8) {
            context.fill(x, y, x + width, y + height, BACKGROUND);
            return;
        }

        // Main body and vertical edges.
        context.fill(x + 3, y + 3, x + width - 3, y + height - 3, BACKGROUND);
        context.fill(x, y + 2, x + 1, y + height - 3, BLACK);
        context.fill(x + 1, y + 2, x + 3, y + height - 3, HIGHLIGHT);
        context.fill(x + width - 3, y + 2, x + width - 1, y + height - 3, OUTLINE);
        context.fill(x + width - 1, y + 3, x + width, y + height - 3, BLACK);

        // Top-left and top-right corner staircase.
        context.fill(x + 2, y, x + width - 3, y + 1, BLACK);
        context.fill(x + 1, y + 1, x + 2, y + 2, BLACK);
        context.fill(x + 2, y + 1, x + width - 3, y + 2, HIGHLIGHT);
        context.fill(x + width - 3, y + 1, x + width - 2, y + 2, BLACK);

        context.fill(x, y + 2, x + 1, y + 3, BLACK);
        context.fill(x + 1, y + 2, x + width - 3, y + 3, HIGHLIGHT);
        context.fill(x + width - 3, y + 2, x + width - 2, y + 3, BACKGROUND);
        context.fill(x + width - 2, y + 2, x + width - 1, y + 3, BLACK);

        context.fill(x + 1, y + 3, x + 4, y + 4, HIGHLIGHT);
        context.fill(x + width - 3, y + 3, x + width - 1, y + 4, OUTLINE);
        context.fill(x + width - 1, y + 3, x + width, y + 4, BLACK);

        // Bottom-left and bottom-right corner staircase.
        int bottom4 = y + height - 4;
        context.fill(x, bottom4, x + 1, bottom4 + 1, BLACK);
        context.fill(x + 1, bottom4, x + 3, bottom4 + 1, HIGHLIGHT);
        context.fill(x + 3, bottom4, x + width - 4, bottom4 + 1, BACKGROUND);
        context.fill(x + width - 4, bottom4, x + width - 1, bottom4 + 1, OUTLINE);
        context.fill(x + width - 1, bottom4, x + width, bottom4 + 1, BLACK);

        int bottom3 = y + height - 3;
        context.fill(x + 1, bottom3, x + 2, bottom3 + 1, BLACK);
        context.fill(x + 2, bottom3, x + 3, bottom3 + 1, BACKGROUND);
        context.fill(x + 3, bottom3, x + width - 1, bottom3 + 1, OUTLINE);
        context.fill(x + width - 1, bottom3, x + width, bottom3 + 1, BLACK);

        int bottom2 = y + height - 2;
        context.fill(x + 2, bottom2, x + 3, bottom2 + 1, BLACK);
        context.fill(x + 3, bottom2, x + width - 2, bottom2 + 1, OUTLINE);
        context.fill(x + width - 2, bottom2, x + width - 1, bottom2 + 1, BLACK);

        int bottom1 = y + height - 1;
        context.fill(x + 3, bottom1, x + width - 2, bottom1 + 1, BLACK);
    }

    /** Draws a recessed vanilla-style region. */
    public static void drawInsetPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, DARK_SHADOW);
        context.fill(x + 1, y + 1, x + width, y + height, HIGHLIGHT);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, SHADOW);
    }

    /** Draws Minecraft's exact 18x18 vanilla slot sprite. */
    public static void drawSlot(DrawContext context, int frameX, int frameY) {
        context.drawGuiTexture(SLOT_TEXTURE, frameX, frameY, 18, 18);
    }

    public static void drawSlotGrid(
            DrawContext context,
            int frameX,
            int frameY,
            int columns,
            int rows
    ) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                drawSlot(context, frameX + column * 18, frameY + row * 18);
            }
        }
    }

    /**
     * Draws a genuinely translucent, non-interactive item hint inside an empty slot.
     *
     * <p>The previous implementation rendered the normal item first and merely laid a
     * grey rectangle over it. Its pigments therefore remained as strong as a real stack.
     * This version fades the item itself, slightly neutralises its colours and explicitly
     * disables glint on the temporary copy so a required-item hint can never be mistaken
     * for an actual item.</p>
     */
    public static void drawGhostItem(DrawContext context, ItemStack stack, int itemX, int itemY) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack ghost = stack.copy();
        ghost.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, false);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(GHOST_RED, GHOST_GREEN, GHOST_BLUE, GHOST_ALPHA);
        try {
            context.drawItem(ghost, itemX, itemY);
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void drawArrow(DrawContext context, int x, int y, boolean active) {
        int color = active ? TEXT : DISABLED_TEXT;
        context.fill(x, y + 3, x + 12, y + 6, color);
        context.fill(x + 8, y, x + 11, y + 9, color);
        context.fill(x + 11, y + 2, x + 14, y + 7, color);
    }

    public static void drawLock(DrawContext context, int x, int y) {
        context.fill(x + 2, y, x + 8, y + 2, DISABLED_TEXT);
        context.fill(x, y + 2, x + 2, y + 6, DISABLED_TEXT);
        context.fill(x + 8, y + 2, x + 10, y + 6, DISABLED_TEXT);
        context.fill(x, y + 5, x + 10, y + 11, DISABLED_TEXT);
        context.fill(x + 4, y + 7, x + 6, y + 10, DARK_SHADOW);
    }

    public static void drawProgressBar(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            int current,
            int max,
            int fillColor
    ) {
        int safeMax = Math.max(1, max);
        int clamped = Math.max(0, Math.min(current, safeMax));
        int filled = clamped * width / safeMax;

        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, DARK_SHADOW);
        context.fill(x, y, x + width, y + height, SHADOW);
        if (filled > 0) {
            context.fill(x, y, x + filled, y + height, fillColor);
        }
        context.fill(x, y, x + width, y + 1, 0x33000000);
    }

    public static void drawSmallText(
            DrawContext context,
            TextRenderer renderer,
            Text text,
            int x,
            int y,
            int color,
            boolean shadow
    ) {
        context.getMatrices().push();
        context.getMatrices().scale(SMALL_TEXT_SCALE, SMALL_TEXT_SCALE, 1.0F);
        context.drawText(
                renderer,
                text,
                Math.round(x / SMALL_TEXT_SCALE),
                Math.round(y / SMALL_TEXT_SCALE),
                color,
                shadow
        );
        context.getMatrices().pop();
    }

    public static void drawCenteredSmallText(
            DrawContext context,
            TextRenderer renderer,
            Text text,
            int centerX,
            int y,
            int color,
            boolean shadow
    ) {
        int textWidth = renderer.getWidth(text);
        float scaledCenter = centerX / SMALL_TEXT_SCALE;
        int drawX = Math.round(scaledCenter - textWidth / 2.0F);

        context.getMatrices().push();
        context.getMatrices().scale(SMALL_TEXT_SCALE, SMALL_TEXT_SCALE, 1.0F);
        context.drawText(
                renderer,
                text,
                drawX,
                Math.round(y / SMALL_TEXT_SCALE),
                color,
                shadow
        );
        context.getMatrices().pop();
    }

    public static boolean isPointInside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
