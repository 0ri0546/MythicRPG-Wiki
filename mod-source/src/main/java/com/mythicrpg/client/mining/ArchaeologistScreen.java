package com.mythicrpg.client.mining;

import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.mining.archaeology.ArchaeologistInteractionManager;
import com.mythicrpg.mining.archaeology.ArchaeologistScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ArchaeologistScreen extends VanillaContainerScreen<ArchaeologistScreenHandler> {

    private static final int BUTTON_X = 70;
    private static final int BUTTON_Y = 57;
    private static final int BUTTON_WIDTH = 36;
    private static final int BUTTON_HEIGHT = 18;

    private ButtonWidget analyzeButton;

    public ArchaeologistScreen(
            ArchaeologistScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title, 176, 166);
        titleX = 8;
        titleY = 6;
        playerInventoryTitleY = 74;
    }

    @Override
    protected void init() {
        super.init();
        analyzeButton = ButtonWidget.builder(
                        Text.literal("▶"),
                        button -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.interactionManager != null) {
                                client.interactionManager.clickButton(handler.syncId, 0);
                            }
                        }
                )
                .dimensions(
                        x + BUTTON_X,
                        y + BUTTON_Y,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT
                )
                .build();
        addDrawableChild(analyzeButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        ArchaeologistInteractionManager.AnalysisStatus status = handler.getStatus();
        analyzeButton.active = handler.canAnalyzeClient();
        analyzeButton.setMessage(status.isSuccess()
                ? Text.literal("✓")
                : status.isBusy()
                ? Text.literal("…")
                : status == ArchaeologistInteractionManager.AnalysisStatus.IDLE
                ? Text.literal("▶")
                : Text.literal("!"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        drawCustomTooltips(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawVanillaContainer(context);
        VanillaContainerUi.drawSlot(context, x + 43, y + 31);
        VanillaContainerUi.drawSlot(context, x + 115, y + 31);
        VanillaContainerUi.drawArrow(context, x + 80, y + 36, handler.canAnalyzeClient());

        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 83, 9, 3);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 141, 9, 1);

        if (handler.getSlot(ArchaeologistScreenHandler.INPUT_SLOT).getStack().isEmpty()) {
            VanillaContainerUi.drawGhostItem(
                    context,
                    new ItemStack(ModItems.SMALL_LAND_COMMON_SKELETON),
                    x + 44,
                    y + 32
            );
        }
        if (handler.getSlot(ArchaeologistScreenHandler.RESULT_SLOT).getStack().isEmpty()) {
            VanillaContainerUi.drawGhostItem(
                    context,
                    new ItemStack(ModItems.EXPEDITION_DOSSIER),
                    x + 116,
                    y + 32
            );
        }

        ArchaeologistInteractionManager.AnalysisStatus status = handler.getStatus();
        if (status != ArchaeologistInteractionManager.AnalysisStatus.IDLE) {
            int color = status.isSuccess()
                    ? 0xFF2C7A45
                    : status.isBusy() ? 0xFF8A6D2F : 0xFFAA3333;
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.literal(status.isSuccess() ? "✓" : status.isBusy() ? "…" : "!"),
                    x + 88,
                    y + 22,
                    color,
                    false
            );
        }
    }


    private void drawCustomTooltips(DrawContext context, int mouseX, int mouseY) {
        if (handler.getSlot(ArchaeologistScreenHandler.INPUT_SLOT).getStack().isEmpty()
                && VanillaContainerUi.isPointInside(mouseX, mouseY, x + 43, y + 31, 18, 18)) {
            context.drawTooltip(
                    textRenderer,
                    Text.translatable("tooltip.mythicrpg.archaeologist.skeleton_slot"),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (handler.getSlot(ArchaeologistScreenHandler.RESULT_SLOT).getStack().isEmpty()
                && VanillaContainerUi.isPointInside(mouseX, mouseY, x + 115, y + 31, 18, 18)) {
            context.drawTooltip(
                    textRenderer,
                    Text.translatable("tooltip.mythicrpg.archaeologist.dossier_slot"),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (VanillaContainerUi.isPointInside(
                mouseX,
                mouseY,
                x + BUTTON_X,
                y + BUTTON_Y,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        )) {
            ArchaeologistInteractionManager.AnalysisStatus status = handler.getStatus();
            Text tooltip = status == ArchaeologistInteractionManager.AnalysisStatus.IDLE
                    ? Text.translatable("tooltip.mythicrpg.archaeologist.analyze")
                    : Text.translatable(status.tooltipKey())
                            .formatted(status.isSuccess()
                                    ? Formatting.GREEN
                                    : status.isBusy() ? Formatting.YELLOW : Formatting.RED);
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }
}
