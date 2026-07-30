package com.mythicrpg.client.ui;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Shared vanilla-styled base for non-container MythicRPG screens.
 *
 * <p>The background is rendered once, followed by a single vanilla panel and the
 * screen's children. This is the validated path used by Building and avoids the
 * double blur caused by calling {@code super.render(...)} after a custom pass.</p>
 */
public abstract class VanillaCustomScreen extends Screen {
    protected final int panelWidth;
    protected final int panelHeight;
    protected int panelX;
    protected int panelY;

    protected VanillaCustomScreen(Text title, int panelWidth, int panelHeight) {
        super(title);
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    @Override
    protected final void init() {
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        initVanillaScreen();
    }

    protected abstract void initVanillaScreen();

    /** Content drawn after the panel and before vanilla widgets. */
    protected abstract void renderVanillaContent(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    );

    /** Optional content drawn behind the panel, such as inventory tabs. */
    protected void renderBehindPanel(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
    }

    /** Optional content drawn after widgets, such as item tooltips. */
    protected void renderAfterWidgets(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
    }

    @Override
    public final void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        renderBehindPanel(context, mouseX, mouseY, delta);
        VanillaContainerUi.drawPanel(context, panelX, panelY, panelWidth, panelHeight);
        context.drawCenteredTextWithShadow(
                textRenderer,
                title,
                panelX + panelWidth / 2,
                panelY + 7,
                VanillaContainerUi.TEXT
        );
        renderVanillaContent(context, mouseX, mouseY, delta);
        renderWidgets(context, mouseX, mouseY, delta);
        renderAfterWidgets(context, mouseX, mouseY, delta);
    }

    protected final void drawSection(
            DrawContext context,
            int x,
            int y,
            int width,
            int height,
            Text label
    ) {
        VanillaContainerUi.drawInsetPanel(context, x, y, width, height);
        if (label != null) {
            context.drawText(
                    textRenderer,
                    label,
                    x + 5,
                    y + 4,
                    VanillaContainerUi.TEXT,
                    false
            );
        }
    }

    protected final void renderWidgets(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        for (var child : children()) {
            if (child instanceof Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
