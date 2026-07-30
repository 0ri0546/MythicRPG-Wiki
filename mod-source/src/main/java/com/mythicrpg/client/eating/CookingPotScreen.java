package com.mythicrpg.client.eating;

import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.eating.CookingPotScreenHandler;
import com.mythicrpg.eating.CookingResult;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.Optional;

/** Native-looking handled screen for the cooking pot. */
public final class CookingPotScreen extends VanillaContainerScreen<CookingPotScreenHandler> {
    private static final int SIGNATURE_X = 18;
    private static final int COOK_X = 92;
    private static final int BUTTON_Y = 58;
    private static final int BUTTON_WIDTH = 66;
    private static final int BUTTON_HEIGHT = 18;
    private static final int PROGRESS_WIDTH = 128;

    private ButtonWidget signatureButton;
    private ButtonWidget cookButton;

    public CookingPotScreen(
            CookingPotScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title, 176, 166);
    }

    @Override
    protected void init() {
        super.init();
        signatureButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.cooking_pot.signature_short"),
                        button -> clickMachineButton(1)
                )
                .dimensions(x + SIGNATURE_X, y + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        cookButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.cooking_pot.cook"),
                        button -> clickMachineButton(0)
                )
                .dimensions(x + COOK_X, y + BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        refreshButtons();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawVanillaContainer(context);
        drawMachineSlots(context);
        drawPlayerSlots(context);
        drawProgress(context);
        drawStatus(context);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refreshButtons();
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    private void refreshButtons() {
        if (signatureButton == null || cookButton == null) {
            return;
        }
        signatureButton.visible = handler.hasSignaturePerk();
        signatureButton.active = handler.hasSignaturePerk() && handler.canPrepareSignatureClient();
        cookButton.active = handler.canStartClient();
    }

    private void clickMachineButton(int id) {
        if (client != null && client.interactionManager != null) {
            client.interactionManager.clickButton(handler.syncId, id);
        }
    }

    private void drawMachineSlots(DrawContext context) {
        for (int slot = 0; slot < 5; slot++) {
            int frameX = x + 25 + slot * 22;
            int frameY = y + 30;
            VanillaContainerUi.drawSlot(context, frameX, frameY);
            if (slot >= handler.getAllowedSlots()) {
                context.fill(frameX + 1, frameY + 1, frameX + 17, frameY + 17, 0x99555555);
                VanillaContainerUi.drawLock(context, frameX + 4, frameY + 3);
            }
        }
        VanillaContainerUi.drawSlot(context, x + 144, y + 30);
    }

    private void drawPlayerSlots(DrawContext context) {
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 83, 9, 3);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 141, 9, 1);
    }

    private void drawProgress(DrawContext context) {
        int progress = handler.getProgressWidth(PROGRESS_WIDTH);
        VanillaContainerUi.drawProgressBar(
                context,
                x + 24,
                y + 51,
                PROGRESS_WIDTH,
                3,
                progress,
                PROGRESS_WIDTH,
                0xFFF2A93B
        );
    }

    private void drawStatus(DrawContext context) {
        Text status = statusText();
        String trimmed = textRenderer.trimToWidth(status.getString(), 150);
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(trimmed),
                x + backgroundWidth / 2,
                y + 17,
                VanillaContainerUi.TEXT
        );
    }

    private Text statusText() {
        Optional<CookingResult> preview = handler.getPreview();
        if (handler.isProcessing()) {
            return Text.translatable(
                    "screen.mythicrpg.cooking_pot.processing",
                    Math.max(1, handler.getRemainingTicks() / 20)
            );
        }
        if (handler.isSignaturePrepared() && !handler.hasHeat()) {
            return Text.translatable("screen.mythicrpg.cooking_pot.no_heat");
        }
        if (handler.isSignaturePrepared() && preview.map(CookingResult::dubious).orElse(true)) {
            return Text.translatable("screen.mythicrpg.cooking_pot.signature_invalid");
        }
        if (handler.isSignaturePrepared()) {
            return Text.translatable("screen.mythicrpg.cooking_pot.signature_ready_short");
        }
        if (handler.hasResult()
                && client != null
                && client.player != null
                && !CookingPotScreenHandler.hasBowlAvailable(client.player)) {
            return Text.translatable("screen.mythicrpg.cooking_pot.bowl_required_short");
        }
        if (handler.hasResult()) {
            return Text.translatable("screen.mythicrpg.cooking_pot.ready", handler.getReadyPortions());
        }
        if (preview.isPresent() && !handler.hasHeat()) {
            return Text.translatable("screen.mythicrpg.cooking_pot.no_heat");
        }
        if (preview.isPresent()) {
            return preview.get().recipe().displayName();
        }
        return Text.translatable("screen.mythicrpg.cooking_pot.waiting_short");
    }
}
