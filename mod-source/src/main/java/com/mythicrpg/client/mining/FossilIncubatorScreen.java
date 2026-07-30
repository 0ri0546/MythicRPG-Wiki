package com.mythicrpg.client.mining;

import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.mining.archaeology.FossilContentRegistry;
import com.mythicrpg.mining.archaeology.FossilIncubationRecipe;
import com.mythicrpg.mining.archaeology.FossilIncubatorScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.Optional;

public final class FossilIncubatorScreen extends VanillaContainerScreen<FossilIncubatorScreenHandler> {

    private static final int PROGRESS_FILL = 0xFF3FAE69;
    private static final int START_BUTTON_X = 103;
    private static final int START_BUTTON_Y = 62;
    private static final int START_BUTTON_WIDTH = 65;
    private static final int START_BUTTON_HEIGHT = 18;

    private ButtonWidget startButton;

    public FossilIncubatorScreen(
            FossilIncubatorScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title, 176, 173);
        titleX = 8;
        titleY = 6;
        playerInventoryTitleY = 80;
    }

    @Override
    protected void init() {
        super.init();
        startButton = ButtonWidget.builder(
                        Text.literal("▶"),
                        button -> {
                            MinecraftClient client = MinecraftClient.getInstance();
                            if (client.interactionManager != null) {
                                client.interactionManager.clickButton(handler.syncId, 0);
                            }
                        }
                )
                .dimensions(
                        x + START_BUTTON_X,
                        y + START_BUTTON_Y,
                        START_BUTTON_WIDTH,
                        START_BUTTON_HEIGHT
                )
                .build();
        addDrawableChild(startButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        startButton.active = handler.canStartClient();
        startButton.setMessage(handler.isProcessing()
                ? Text.literal("…")
                : handler.hasResult()
                ? Text.literal("✓")
                : Text.literal("▶"));
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

        VanillaContainerUi.drawSlotGrid(context, x + 16, y + 19, 3, 3);
        VanillaContainerUi.drawSlot(context, x + 81, y + 28);
        VanillaContainerUi.drawSlot(context, x + 81, y + 52);
        VanillaContainerUi.drawSlot(context, x + 150, y + 37);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 90, 9, 3);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 148, 9, 1);

        drawGhostItems(context);

        int progressX = x + 105;
        int progressY = y + 42;
        int progressWidth = 40;
        int total = Math.max(1, handler.getTotalTicks());
        int elapsed = handler.getTotalTicks() <= 0
                ? 0
                : Math.max(0, handler.getTotalTicks() - handler.getRemainingTicks());
        VanillaContainerUi.drawProgressBar(
                context,
                progressX,
                progressY,
                progressWidth,
                6,
                elapsed,
                total,
                PROGRESS_FILL
        );

        drawCompactStatus(context);
    }


    private void drawGhostItems(DrawContext context) {
        ItemStack fossilGhost = new ItemStack(ModItems.SMALL_LAND_COMMON_FOSSIL);
        for (int slot = 0; slot < 9; slot++) {
            if (handler.getSlot(slot).getStack().isEmpty()) {
                int column = slot % 3;
                int row = slot / 3;
                VanillaContainerUi.drawGhostItem(
                        context,
                        fossilGhost,
                        x + 17 + column * 18,
                        y + 20 + row * 18
                );
            }
        }

        if (handler.getSlot(9).getStack().isEmpty()) {
            VanillaContainerUi.drawGhostItem(
                    context,
                    new ItemStack(Items.WATER_BUCKET),
                    x + 82,
                    y + 29
            );
        }
        if (handler.getSlot(10).getStack().isEmpty()) {
            VanillaContainerUi.drawGhostItem(
                    context,
                    new ItemStack(Items.KELP),
                    x + 82,
                    y + 53
            );
        }

        if (handler.getSlot(11).getStack().isEmpty()) {
            Optional<FossilIncubationRecipe.Output> preview = handler.getPreviewOutput();
            preview.flatMap(output -> FossilContentRegistry.skeletonItem(output.family(), output.rarity()))
                    .ifPresent(item -> VanillaContainerUi.drawGhostItem(
                            context,
                            new ItemStack(item),
                            x + 151,
                            y + 38
                    ));
        }
    }

    private void drawCompactStatus(DrawContext context) {
        if (handler.isProcessing()) {
            int seconds = (handler.getRemainingTicks() + 19) / 20;
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.literal(formatDuration(seconds)),
                    x + 125,
                    y + 29,
                    VanillaContainerUi.TEXT,
                    false
            );
        } else if (handler.hasResult()) {
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.literal("✓").formatted(Formatting.GREEN),
                    x + 125,
                    y + 29,
                    0xFF2C7A45,
                    false
            );
        }
    }

    private void drawCustomTooltips(DrawContext context, int mouseX, int mouseY) {
        for (int slot = 0; slot < 9; slot++) {
            int column = slot % 3;
            int row = slot / 3;
            int slotX = x + 16 + column * 18;
            int slotY = y + 19 + row * 18;
            if (handler.getSlot(slot).getStack().isEmpty()
                    && VanillaContainerUi.isPointInside(mouseX, mouseY, slotX, slotY, 18, 18)) {
                context.drawTooltip(
                        textRenderer,
                        Text.translatable("tooltip.mythicrpg.incubator.fossil_slot"),
                        mouseX,
                        mouseY
                );
                return;
            }
        }

        if (handler.getSlot(9).getStack().isEmpty()
                && VanillaContainerUi.isPointInside(mouseX, mouseY, x + 81, y + 28, 18, 18)) {
            context.drawTooltip(
                    textRenderer,
                    Text.translatable("tooltip.mythicrpg.incubator.water_slot"),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (handler.getSlot(10).getStack().isEmpty()
                && VanillaContainerUi.isPointInside(mouseX, mouseY, x + 81, y + 52, 18, 18)) {
            context.drawTooltip(
                    textRenderer,
                    Text.translatable("tooltip.mythicrpg.incubator.kelp_slot"),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (VanillaContainerUi.isPointInside(mouseX, mouseY, x + 150, y + 37, 18, 18)) {
            Optional<FossilIncubationRecipe.Output> preview = handler.getPreviewOutput();
            if (preview.isPresent() && handler.getSlot(11).getStack().isEmpty()) {
                FossilIncubationRecipe.Output output = preview.get();
                context.drawTooltip(
                        textRenderer,
                        Text.translatable(
                                "tooltip.mythicrpg.incubator.output",
                                output.rarity().displayName(),
                                output.family().displayName()
                        ),
                        mouseX,
                        mouseY
                );
                return;
            }
        }

        if (VanillaContainerUi.isPointInside(
                mouseX,
                mouseY,
                x + START_BUTTON_X,
                y + START_BUTTON_Y,
                START_BUTTON_WIDTH,
                START_BUTTON_HEIGHT
        )) {
            Text tooltip = handler.isProcessing()
                    ? Text.translatable("tooltip.mythicrpg.incubator.processing")
                    : handler.hasResult()
                    ? Text.translatable("tooltip.mythicrpg.incubator.ready")
                    : handler.canStartClient()
                    ? Text.translatable("tooltip.mythicrpg.incubator.start")
                    : Text.translatable("tooltip.mythicrpg.incubator.incomplete");
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private static String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
    }
}
