package com.mythicrpg.client.traveling;

import com.mythicrpg.client.ui.MythicInventoryTabs;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.traveling.TravelingCompassScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TravelingCompassScreen extends HandledScreen<TravelingCompassScreenHandler> {

    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int INNER_COLOR = 0xFFD6D6D6;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int SLOT_BORDER = 0xFF8B8B8B;
    private static final int SLOT_FILL = 0xFFE8E8E8;
    private static final int LOCKED_OVERLAY = 0x77000000;
    private static final int VANILLA_TEXT = 0x404040;

    private ButtonWidget searchButton;

    public TravelingCompassScreen(
            TravelingCompassScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title);
        backgroundWidth = 176;
        backgroundHeight = 166;
        titleX = 8;
        titleY = 7;
        playerInventoryTitleY = 74;
    }

    @Override
    protected void init() {
        super.init();

        searchButton = ButtonWidget.builder(
                        getSearchButtonText(),
                        button -> {
                            MinecraftClient client = MinecraftClient.getInstance();

                            if (client.interactionManager != null) {
                                client.interactionManager.clickButton(handler.syncId, 0);
                            }
                        }
                )
                .dimensions(x + 72, y + 40, 86, 20)
                .build();

        addDrawableChild(searchButton);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();

        if (searchButton != null) {
            searchButton.setMessage(getSearchButtonText());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        renderTabs(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawPanel(context);
        drawSlots(context);
        drawModuleGhost(context);

        if (handler.isSearching()) {
            context.fill(x + 30, y + 42, x + 48, y + 60, LOCKED_OVERLAY);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, titleX, titleY, VANILLA_TEXT, false);
        context.drawText(
                textRenderer,
                Text.translatable("screen.mythicrpg.monumental_compass.module"),
                8,
                24,
                VANILLA_TEXT,
                false
        );
        context.drawText(textRenderer, playerInventoryTitle, 8, playerInventoryTitleY, VANILLA_TEXT, false);

        Text hint = handler.isSearching()
                ? Text.translatable("screen.mythicrpg.monumental_compass.searching")
                        .formatted(Formatting.GOLD)
                : Text.translatable("screen.mythicrpg.monumental_compass.hint")
                        .formatted(Formatting.DARK_GRAY);

        drawSmallText(context, hint, 8, 64, VANILLA_TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MythicInventoryTabs.isOverInventoryTab(
                x, y, backgroundWidth, mouseX, mouseY
        )) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != null) {
                client.player.closeHandledScreen();
                MythicInventoryTabs.setScreenPreservingMouse(new InventoryScreen(client.player));
            }

            return true;
        }

        if (button == 0 && MythicInventoryTabs.isOverMythicCraftingTab(
                x, y, backgroundWidth, mouseX, mouseY
        )) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != null) {
                client.player.closeHandledScreen();
            }

            MythicInventoryTabs.requestOpenMythicCrafting();
            return true;
        }

        if (button == 0 && MythicInventoryTabs.isOverFossilCodexTab(
                x, y, backgroundWidth, mouseX, mouseY
        )) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.closeHandledScreen();
            }
            MythicInventoryTabs.requestOpenFossilCodex();
            return true;
        }

        if (button == 0 && MythicInventoryTabs.isOverEatingCodexTab(
                x, y, backgroundWidth, mouseX, mouseY
        )) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.closeHandledScreen();
            }
            MythicInventoryTabs.requestOpenEatingCodex();
            return true;
        }

        if (button == 0 && MythicInventoryTabs.isOverFishingCodexTab(
                x, y, backgroundWidth, mouseX, mouseY
        )) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) client.player.closeHandledScreen();
            MythicInventoryTabs.requestOpenFishingCodex();
            return true;
        }

        if (button == 0 && MythicInventoryTabs.isOverTitlesTab(
                x, y, backgroundWidth, mouseX, mouseY
        )) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.closeHandledScreen();
            }
            MythicInventoryTabs.requestOpenTitles();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private Text getSearchButtonText() {
        return Text.translatable(handler.isSearching()
                ? "screen.mythicrpg.monumental_compass.stop"
                : "screen.mythicrpg.monumental_compass.search");
    }

    private void drawPanel(DrawContext context) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, BORDER_COLOR);
        context.fill(x + 1, y + 1, x + backgroundWidth - 1, y + backgroundHeight - 1, PANEL_COLOR);

        context.fill(x + 7, y + 18, x + backgroundWidth - 7, y + 72, 0xFF9F9F9F);
        context.fill(x + 8, y + 19, x + backgroundWidth - 8, y + 71, INNER_COLOR);
    }


    private void drawModuleGhost(DrawContext context) {
        if (!handler.getSlot(0).getStack().isEmpty()) {
            return;
        }

        VanillaContainerUi.drawGhostItem(
                context,
                new ItemStack(ModItems.STRUCTURE_MODULE),
                x + 31,
                y + 43
        );
    }

    private void drawSlots(DrawContext context) {
        drawSlotFrame(context, x + 30, y + 42);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlotFrame(context, x + 8 + column * 18, y + 84 + row * 18);
            }
        }

        for (int column = 0; column < 9; column++) {
            drawSlotFrame(context, x + 8 + column * 18, y + 142);
        }
    }

    private void drawSlotFrame(DrawContext context, int slotX, int slotY) {
        context.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF777777);
        context.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, SLOT_BORDER);
        context.fill(slotX + 2, slotY + 2, slotX + 16, slotY + 16, SLOT_FILL);
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        MythicInventoryTabs.renderInventoryTab(
                context, x, y, backgroundWidth, mouseX, mouseY, false
        );
        MythicInventoryTabs.renderMythicCraftingTab(
                context, x, y, backgroundWidth, mouseX, mouseY, false
        );
        MythicInventoryTabs.renderTravelingCompassTab(
                context, x, y, backgroundWidth, mouseX, mouseY, true
        );
        MythicInventoryTabs.renderFossilCodexTab(
                context, x, y, backgroundWidth, mouseX, mouseY, false
        );
        MythicInventoryTabs.renderEatingCodexTab(
                context, x, y, backgroundWidth, mouseX, mouseY, false
        );
        MythicInventoryTabs.renderFishingCodexTab(
                context, x, y, backgroundWidth, mouseX, mouseY, false
        );
        MythicInventoryTabs.renderTitlesTab(
                context, x, y, backgroundWidth, mouseX, mouseY, false
        );
    }

    private void drawSmallText(DrawContext context, Text text, int textX, int textY, int color) {
        context.getMatrices().push();
        context.getMatrices().scale(0.75F, 0.75F, 1.0F);
        context.drawText(
                textRenderer,
                text,
                Math.round(textX / 0.75F),
                Math.round(textY / 0.75F),
                color,
                false
        );
        context.getMatrices().pop();
    }
}
