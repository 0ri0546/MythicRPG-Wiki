package com.mythicrpg.client.crafting;

import com.mythicrpg.client.ui.MythicInventoryTabs;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.crafting.MythicCraftingScreenHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Shared screen for vanilla, infinite and portable MythicRPG crafting.
 * Slot positions and gameplay are intentionally unchanged; only the common
 * vanilla visual language is applied here.
 */
public class MythicCraftingScreen extends VanillaContainerScreen<MythicCraftingScreenHandler> {

    private static final int LOCKED_OVERLAY = 0x88000000;
    private static final int DURABILITY_FILL = 0xFF6AAA64;
    private static final int CRAFT_CHARGE_FILL = 0xFF80AA20;

    private static final int DURABILITY_X = 18;
    private static final int DURABILITY_Y = 118;
    private static final int CRAFT_CHARGE_X = 18;
    private static final int CRAFT_CHARGE_Y = 136;
    private static final int BAR_WIDTH = 140;
    private static final int BAR_HEIGHT = 7;

    public MythicCraftingScreen(
            MythicCraftingScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title, 176, 240);
        playerInventoryTitleY = 144;
        titleX = 8;
        titleY = 7;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        renderTabs(context, mouseX, mouseY);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        drawCustomTooltips(context, mouseX, mouseY);
        MythicInventoryTabs.renderTooltip(
                context,
                textRenderer,
                x,
                y,
                backgroundWidth,
                mouseX,
                mouseY
        );
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawVanillaContainer(context);
        drawSlotGrid(context);
        drawProgressBars(context);
        drawTransformationLock(context);

        VanillaContainerUi.drawArrow(context, x + 88, y + 37, true);
        VanillaContainerUi.drawArrow(
                context,
                x + 80,
                y + 96,
                handler.isTransformationUnlocked()
        );
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MythicInventoryTabs.isOverInventoryTab(x, y, backgroundWidth, mouseX, mouseY)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.closeHandledScreen();
                MythicInventoryTabs.setScreenPreservingMouse(new InventoryScreen(client.player));
            }
            return true;
        }

        if (button == 0 && MythicInventoryTabs.isOverTravelingCompassTab(
                x, y, backgroundWidth, mouseX, mouseY
        )) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.closeHandledScreen();
            }
            MythicInventoryTabs.requestOpenTravelingCompass();
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

    private void drawSlotGrid(DrawContext context) {
        VanillaContainerUi.drawSlotGrid(context, x + 29, y + 23, 3, 3);
        VanillaContainerUi.drawSlot(context, x + 123, y + 31);
        VanillaContainerUi.drawSlot(context, x + 47, y + 91);
        VanillaContainerUi.drawSlot(context, x + 111, y + 91);

        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 155, 9, 3);
        VanillaContainerUi.drawSlotGrid(context, x + 7, y + 213, 9, 1);
    }

    private void drawProgressBars(DrawContext context) {
        if (handler.stationHasFiniteDurability()) {
            int current = Math.max(0, handler.getStationDurability());
            int max = Math.max(1, handler.getStationMaxDurability());
            VanillaContainerUi.drawProgressBar(
                    context,
                    x + DURABILITY_X,
                    y + DURABILITY_Y,
                    BAR_WIDTH,
                    BAR_HEIGHT,
                    current,
                    max,
                    DURABILITY_FILL
            );
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.literal(current + " / " + max),
                    x + DURABILITY_X + BAR_WIDTH / 2,
                    y + DURABILITY_Y - 8,
                    VanillaContainerUi.TEXT,
                    false
            );
        } else {
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.literal("∞"),
                    x + DURABILITY_X + BAR_WIDTH / 2,
                    y + DURABILITY_Y - 8,
                    VanillaContainerUi.TEXT,
                    false
            );
            VanillaContainerUi.drawProgressBar(
                    context,
                    x + DURABILITY_X,
                    y + DURABILITY_Y,
                    BAR_WIDTH,
                    BAR_HEIGHT,
                    1,
                    1,
                    DURABILITY_FILL
            );
        }

        int charge = Math.max(0, Math.min(100, handler.getCraftChargePercent()));
        VanillaContainerUi.drawProgressBar(
                context,
                x + CRAFT_CHARGE_X,
                y + CRAFT_CHARGE_Y,
                BAR_WIDTH,
                BAR_HEIGHT,
                charge,
                100,
                CRAFT_CHARGE_FILL
        );
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.literal(charge + "%"),
                x + CRAFT_CHARGE_X + BAR_WIDTH / 2,
                y + CRAFT_CHARGE_Y - 8,
                VanillaContainerUi.TEXT,
                false
        );
    }

    private void drawTransformationLock(DrawContext context) {
        if (handler.isTransformationUnlocked()) {
            return;
        }

        context.fill(x + 47, y + 91, x + 65, y + 109, LOCKED_OVERLAY);
        context.fill(x + 111, y + 91, x + 129, y + 109, LOCKED_OVERLAY);
        VanillaContainerUi.drawLock(context, x + 82, y + 91);
    }

    private void drawCustomTooltips(DrawContext context, int mouseX, int mouseY) {
        if (VanillaContainerUi.isPointInside(
                mouseX, mouseY,
                x + DURABILITY_X - 1,
                y + DURABILITY_Y - 9,
                BAR_WIDTH + 2,
                BAR_HEIGHT + 10
        )) {
            Text tooltip = handler.stationHasFiniteDurability()
                    ? Text.translatable(
                            "tooltip.mythicrpg.ui.station_durability",
                            handler.getStationDurability(),
                            Math.max(1, handler.getStationMaxDurability())
                    )
                    : Text.translatable("tooltip.mythicrpg.ui.station_infinite");
            context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            return;
        }

        if (VanillaContainerUi.isPointInside(
                mouseX, mouseY,
                x + CRAFT_CHARGE_X - 1,
                y + CRAFT_CHARGE_Y - 9,
                BAR_WIDTH + 2,
                BAR_HEIGHT + 10
        )) {
            context.drawTooltip(
                    textRenderer,
                    Text.translatable(
                            "tooltip.mythicrpg.ui.craft_charge",
                            handler.getCraftChargePercent()
                    ),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (!handler.isTransformationUnlocked()) {
            boolean input = VanillaContainerUi.isPointInside(mouseX, mouseY, x + 47, y + 91, 18, 18);
            boolean output = VanillaContainerUi.isPointInside(mouseX, mouseY, x + 111, y + 91, 18, 18);
            boolean lock = VanillaContainerUi.isPointInside(mouseX, mouseY, x + 78, y + 88, 18, 18);
            if (input || output || lock) {
                context.drawTooltip(
                        textRenderer,
                        Text.translatable("tooltip.mythicrpg.ui.transformation_locked")
                                .formatted(Formatting.RED),
                        mouseX,
                        mouseY
                );
            }
        }
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        MythicInventoryTabs.renderInventoryTab(context, x, y, backgroundWidth, mouseX, mouseY, false);
        MythicInventoryTabs.renderMythicCraftingTab(context, x, y, backgroundWidth, mouseX, mouseY, true);
        MythicInventoryTabs.renderTravelingCompassTab(context, x, y, backgroundWidth, mouseX, mouseY, false);
        MythicInventoryTabs.renderFossilCodexTab(context, x, y, backgroundWidth, mouseX, mouseY, false);
        MythicInventoryTabs.renderEatingCodexTab(context, x, y, backgroundWidth, mouseX, mouseY, false);
        MythicInventoryTabs.renderFishingCodexTab(context, x, y, backgroundWidth, mouseX, mouseY, false);
        MythicInventoryTabs.renderTitlesTab(context, x, y, backgroundWidth, mouseX, mouseY, false);
    }
}
