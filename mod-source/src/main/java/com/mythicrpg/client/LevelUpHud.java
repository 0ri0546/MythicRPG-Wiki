package com.mythicrpg.client;

import com.mythicrpg.core.SkillType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class LevelUpHud {
    private static final int POPUP_WIDTH = 160;
    private static final int POPUP_HEIGHT = 32;
    private static final int MARGIN = 8;
    private static final int SPACING = 6;

    private static final long FADE_IN_MS = 300;
    private static final long HOLD_MS = 2000;
    private static final long FADE_OUT_MS = 500;
    private static final long TOTAL_DURATION_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    private record ActivePopup(SkillType skill, int level, int currentXp, int xpForNext, long startTime) {}

    private static final List<ActivePopup> activePopups = new ArrayList<>();

    public static void show(SkillType skill, int level, int currentXp, int xpForNext) {
        activePopups.add(new ActivePopup(skill, level, currentXp, xpForNext, System.currentTimeMillis()));
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        long now = System.currentTimeMillis();
        activePopups.removeIf(p -> now - p.startTime() > TOTAL_DURATION_MS);
        if (activePopups.isEmpty()) {
            return;
        }

        int baseX = context.getScaledWindowWidth() - MARGIN - POPUP_WIDTH;
        int baseY = context.getScaledWindowHeight() - MARGIN - POPUP_HEIGHT;

        for (int i = 0; i < activePopups.size(); i++) {
            ActivePopup popup = activePopups.get(i);
            int y = baseY - i * (POPUP_HEIGHT + SPACING);
            drawPopup(context, popup, baseX, y, now);
        }
    }

    private static void drawPopup(DrawContext context, ActivePopup popup, int x, int baseY, long now) {
        float alpha = computeAlpha(popup, now);
        if (alpha <= 0f) {
            return;
        }

        long elapsed = now - popup.startTime();
        int slideOffset = elapsed < FADE_IN_MS ? (int) ((1f - alpha) * 20) : 0;
        int y = baseY + slideOffset;
        int alphaInt = (int) (alpha * 255);

        drawFrame(context, x, y, POPUP_WIDTH, POPUP_HEIGHT, alphaInt);

        int iconX = x + 6;
        int iconY = y + (POPUP_HEIGHT - 16) / 2;
        context.drawItem(new net.minecraft.item.ItemStack(SkillIcons.get(popup.skill())), iconX, iconY);

        int textX = iconX + 22;
        MinecraftClient client = MinecraftClient.getInstance();
        int titleColor = 0xFFFF55 | (alphaInt << 24);
        context.drawText(
                client.textRenderer,
                Text.translatable("hud.mythicrpg.level_up", popup.skill().displayName(), popup.level()),
                textX, y + 6, titleColor, true
        );

        int barX = textX;
        int barY = y + 21;
        int barWidth = x + POPUP_WIDTH - 6 - barX;
        int barHeight = 4;

        int barBorder = 0x000000 | (alphaInt << 24);
        context.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + barHeight + 1, barBorder);

        int barBg = 0x373737 | (alphaInt << 24);
        context.fill(barX, barY, barX + barWidth, barY + barHeight, barBg);

        if (popup.xpForNext() > 0) {
            float progress = Math.min(1f, popup.currentXp() / (float) popup.xpForNext());
            int filledWidth = Math.max(1, (int) (barWidth * progress));
            int barFg = 0x55FF55 | (alphaInt << 24);
            context.fill(barX, barY, barX + filledWidth, barY + barHeight, barFg);
        } else {
            int barFg = 0xFFD700 | (alphaInt << 24);
            context.fill(barX, barY, barX + barWidth, barY + barHeight, barFg);
        }
    }

    private static void drawFrame(DrawContext context, int x, int y, int width, int height, int alphaInt) {
        int border = 0x000000 | (alphaInt << 24);
        context.fill(x, y, x + width, y + height, border);

        int background = 0x2B2B2B | (alphaInt << 24);
        context.fill(x + 2, y + 2, x + width - 2, y + height - 2, background);

        int highlight = 0x555555 | (alphaInt << 24);
        context.fill(x + 2, y + 2, x + width - 2, y + 3, highlight);
        context.fill(x + 2, y + 2, x + 3, y + height - 2, highlight);
    }

    private static float computeAlpha(ActivePopup popup, long now) {
        long elapsed = now - popup.startTime();

        if (elapsed < FADE_IN_MS) {
            return elapsed / (float) FADE_IN_MS;
        }
        if (elapsed < FADE_IN_MS + HOLD_MS) {
            return 1f;
        }

        long fadeOutElapsed = elapsed - FADE_IN_MS - HOLD_MS;
        if (fadeOutElapsed < FADE_OUT_MS) {
            return 1f - (fadeOutElapsed / (float) FADE_OUT_MS);
        }
        return 0f;
    }

}
