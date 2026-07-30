
package com.mythicrpg.client.fishing;

import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.fishing.FishNetBlockEntity;
import com.mythicrpg.fishing.FishNetScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class FishNetScreen extends VanillaContainerScreen<FishNetScreenHandler> {
    public FishNetScreen(FishNetScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title, 176, 167);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawVanillaContainer(context);
        VanillaContainerUi.drawInsetPanel(context, x + 24, y + 18, 128, 52);

        for (int slot = 0; slot < FishNetBlockEntity.INVENTORY_SIZE; slot++) {
            int frameX = x + 43 + slot * 18;
            int frameY = y + 34;
            VanillaContainerUi.drawSlot(context, frameX, frameY);
            if (slot >= handler.capacity()) {
                context.fill(frameX + 1, frameY + 1, frameX + 17, frameY + 17, 0x99555555);
                VanillaContainerUi.drawLock(context, frameX + 4, frameY + 3);
            }
        }

        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.fish_net.capacity", handler.capacity()),
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
