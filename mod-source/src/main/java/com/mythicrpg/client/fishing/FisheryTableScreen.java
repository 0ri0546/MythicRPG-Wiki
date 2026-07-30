package com.mythicrpg.client.fishing;

import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.fishing.FisheryTableScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Native-looking fishery table with an explicit extraction transaction. */
public final class FisheryTableScreen extends VanillaContainerScreen<FisheryTableScreenHandler> {
    private ButtonWidget transformButton;

    public FisheryTableScreen(FisheryTableScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title, 176, 167);
    }

    @Override
    protected void init() {
        super.init();
        transformButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.mythicrpg.fishery_table.transform"),
                button -> {
                    if (client != null && client.interactionManager != null) {
                        client.interactionManager.clickButton(handler.syncId, 0);
                    }
                }
        ).dimensions(x + 56, y + 59, 64, 18).build());
        refreshButton();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawVanillaContainer(context);
        VanillaContainerUi.drawInsetPanel(context, x + 30, y + 18, 116, 64);
        VanillaContainerUi.drawSlot(context, x + 51, y + 34);
        VanillaContainerUi.drawArrow(context, x + 77, y + 39, true);
        VanillaContainerUi.drawSlot(context, x + 105, y + 34);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 82, 9, 3);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 140, 9, 1);

        if (handler.getSlot(0).getStack().isEmpty()) {
            VanillaContainerUi.drawGhostItem(context, new ItemStack(ModItems.FISHING_CATCH), x + 52, y + 35);
        }
        if (handler.getSlot(1).getStack().isEmpty()) {
            ItemStack preview = handler.previewOutput();
            if (!preview.isEmpty()) VanillaContainerUi.drawGhostItem(context, preview, x + 106, y + 35);
        }

        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.fishery_table.extract"),
                x + backgroundWidth / 2,
                y + 22,
                VanillaContainerUi.TEXT,
                false
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refreshButton();
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void refreshButton() {
        if (transformButton != null) transformButton.active = handler.canTransformClient();
    }
}
