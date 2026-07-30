
package com.mythicrpg.client.fishing;

import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.fishing.FishingRodScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class FishingRodScreen extends VanillaContainerScreen<FishingRodScreenHandler> {
    public FishingRodScreen(
            FishingRodScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title, 176, 167);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawVanillaContainer(context);

        VanillaContainerUi.drawInsetPanel(context, x + 12, y + 18, 152, 52);
        context.drawItem(new ItemStack(ModItems.MYTHIC_FISHING_ROD), x + 18, y + 34);

        VanillaContainerUi.drawSlot(context, x + 43, y + 34);
        VanillaContainerUi.drawArrow(context, x + 68, y + 39, true);
        VanillaContainerUi.drawSlot(context, x + 97, y + 34);
        VanillaContainerUi.drawSlot(context, x + 119, y + 34);

        if (!handler.hasRuneSlots()) {
            drawLockedRuneSlot(context, 1, x + 98, y + 35);
            drawLockedRuneSlot(context, 2, x + 120, y + 35);
        }

        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 82, 9, 3);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 140, 9, 1);

        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.fishing_rod.bait"),
                x + 52,
                y + 22,
                VanillaContainerUi.TEXT,
                false
        );
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.fishing_rod.runes"),
                x + 117,
                y + 22,
                VanillaContainerUi.TEXT,
                false
        );

        if (handler.getSlot(0).getStack().isEmpty()) {
            VanillaContainerUi.drawGhostItem(
                    context,
                    new ItemStack(ModItems.BAIT_I),
                    x + 44,
                    y + 35
            );
        }
        if (handler.hasRuneSlots() && handler.getSlot(1).getStack().isEmpty()) {
            VanillaContainerUi.drawGhostItem(
                    context,
                    new ItemStack(ModItems.RUNE_RARITY),
                    x + 98,
                    y + 35
            );
        }
        if (handler.hasRuneSlots() && handler.getSlot(2).getStack().isEmpty()) {
            VanillaContainerUi.drawGhostItem(
                    context,
                    new ItemStack(ModItems.RUNE_SPEED),
                    x + 120,
                    y + 35
            );
        }
    }

    private void drawLockedRuneSlot(DrawContext context, int handlerSlot, int slotX, int slotY) {
        if (!handler.getSlot(handlerSlot).getStack().isEmpty()) {
            return;
        }
        context.fill(slotX, slotY, slotX + 16, slotY + 16, 0x99555555);
        VanillaContainerUi.drawLock(context, slotX + 3, slotY + 2);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);

        if (!handler.hasRuneSlots()
                && (VanillaContainerUi.isPointInside(mouseX, mouseY, x + 97, y + 34, 40, 18))) {
            context.drawTooltip(
                    textRenderer,
                    Text.translatable("tooltip.mythicrpg.fishing_rod.runes_locked"),
                    mouseX,
                    mouseY
            );
        }
    }
}
