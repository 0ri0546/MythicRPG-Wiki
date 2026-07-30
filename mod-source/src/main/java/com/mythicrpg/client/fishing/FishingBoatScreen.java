
package com.mythicrpg.client.fishing;

import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.fishing.FishingBoatScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class FishingBoatScreen extends VanillaContainerScreen<FishingBoatScreenHandler> {
    public FishingBoatScreen(
            FishingBoatScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title, 176, 167);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawVanillaContainer(context);
        VanillaContainerUi.drawInsetPanel(context, x + 38, y + 18, 100, 52);
        VanillaContainerUi.drawSlotGrid(context, x + 61, y + 34, 3, 1);
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.fishing_boat.storage"),
                x + backgroundWidth / 2,
                y + 22,
                VanillaContainerUi.TEXT,
                false
        );
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 82, 9, 3);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 140, 9, 1);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
