package com.mythicrpg.client.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

/**
 * Common base for MythicRPG handled screens that must look native to Minecraft.
 *
 * <p>It owns the exact vanilla panel and deliberately renders no permanent
 * title or player-inventory label. Concrete screens keep their validated slot
 * positions and draw only their functional upper section.</p>
 */
public abstract class VanillaContainerScreen<T extends ScreenHandler> extends HandledScreen<T> {

    protected VanillaContainerScreen(
            T handler,
            PlayerInventory inventory,
            Text title,
            int backgroundWidth,
            int backgroundHeight
    ) {
        super(handler, inventory, title);
        this.backgroundWidth = backgroundWidth;
        this.backgroundHeight = backgroundHeight;
    }

    protected final void drawVanillaContainer(DrawContext context) {
        VanillaContainerUi.drawPanel(context, x, y, backgroundWidth, backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Screen identity comes from its tab/context and slot tooltips.
        // Permanent titles and the redundant "Inventory" label stay hidden.
    }
}
