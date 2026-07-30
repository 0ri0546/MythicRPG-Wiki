package com.mythicrpg.client;

import com.mythicrpg.core.SkillType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.LinkedHashMap;
import java.util.Map;

public class XpHud {
    private static final int BAR_WIDTH = 140;
    private static final int BAR_HEIGHT = 24;
    private static final int MARGIN = 8;
    private static final int SPACING = 4;
    private static final long DISPLAY_DURATION_MS = 4000;
    private static final long FADE_OUT_MS = 400;

    private record Entry(int level, int currentXp, int xpForNext, long lastUpdate) {}

    private static final Map<SkillType, Entry> activeEntries = new LinkedHashMap<>();

    public static void update(SkillType type, int level, int currentXp, int xpForNext) {
        activeEntries.put(type, new Entry(level, currentXp, xpForNext, System.currentTimeMillis()));
    }

    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        long now = System.currentTimeMillis();
        activeEntries.entrySet().removeIf(e -> now - e.getValue().lastUpdate() > DISPLAY_DURATION_MS);
        if (activeEntries.isEmpty()) {
            return;
        }

        var sorted = activeEntries.entrySet().stream()
                .sorted((a, b) -> Long.compare(a.getValue().lastUpdate(), b.getValue().lastUpdate()))
                .toList();

        int baseX = MARGIN;
        int baseY = context.getScaledWindowHeight() - MARGIN - BAR_HEIGHT;

        for (int i = 0; i < sorted.size(); i++) {
            var entry = sorted.get(i);
            int y = baseY - i * (BAR_HEIGHT + SPACING);
            drawBar(context, entry.getKey(), entry.getValue(), baseX, y, now);
        }
    }

    private static void drawBar(DrawContext context, SkillType type, Entry entry, int x, int y, long now) {
        float alpha = computeAlpha(entry, now);
        if (alpha <= 0f) {
            return;
        }
        int alphaInt = (int) (alpha * 255);

        int border = 0x000000 | (alphaInt << 24);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, border);

        int background = 0x2B2B2B | (alphaInt << 24);
        context.fill(x + 1, y + 1, x + BAR_WIDTH - 1, y + BAR_HEIGHT - 1, background);

        MinecraftClient client = MinecraftClient.getInstance();
        int textColor = 0xFFFFFF | (alphaInt << 24);
        context.drawText(client.textRenderer, Text.translatable("hud.mythicrpg.xp.skill_level", type.displayName(), entry.level()), x + 4, y + 3, textColor, false);

        int barX = x + 4;
        int barY = y + 15;
        int barWidth = BAR_WIDTH - 8;
        int barHeight = 5;

        int barBg = 0x373737 | (alphaInt << 24);
        context.fill(barX, barY, barX + barWidth, barY + barHeight, barBg);

        int percent;
        if (entry.xpForNext() > 0) {
            float progress = Math.min(1f, entry.currentXp() / (float) entry.xpForNext());
            percent = (int) (progress * 100);
            int filledWidth = Math.max(1, (int) (barWidth * progress));
            int barFg = 0x55FF55 | (alphaInt << 24);
            context.fill(barX, barY, barX + filledWidth, barY + barHeight, barFg);
        } else {
            percent = 100;
            int barFg = 0xFFD700 | (alphaInt << 24);
            context.fill(barX, barY, barX + barWidth, barY + barHeight, barFg);
        }

        String percentText = percent + "%";
        int percentWidth = client.textRenderer.getWidth(percentText);
        context.drawText(client.textRenderer, percentText, x + BAR_WIDTH - percentWidth - 4, y + 3, textColor, false);
    }

    private static float computeAlpha(Entry entry, long now) {
        long elapsed = now - entry.lastUpdate();
        long fadeStart = DISPLAY_DURATION_MS - FADE_OUT_MS;
        if (elapsed < fadeStart) {
            return 1f;
        }
        long fadeElapsed = elapsed - fadeStart;
        if (fadeElapsed >= FADE_OUT_MS) {
            return 0f;
        }
        return 1f - (fadeElapsed / (float) FADE_OUT_MS);
    }

}
