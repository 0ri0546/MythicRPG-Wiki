package com.mythicrpg.client.building.ui;

import com.mythicrpg.client.ui.VanillaCustomScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Vanilla-styled base retained for Building configuration screens. */
public abstract class VanillaBuildingScreen extends VanillaCustomScreen {
    protected VanillaBuildingScreen(Text title, int panelWidth, int panelHeight) {
        super(title, panelWidth, panelHeight);
    }

    @Override
    protected final void initVanillaScreen() {
        initBuildingScreen();
    }

    @Override
    protected final void renderVanillaContent(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        renderBuildingContent(context, mouseX, mouseY, delta);
    }

    protected abstract void initBuildingScreen();

    protected abstract void renderBuildingContent(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    );
}
