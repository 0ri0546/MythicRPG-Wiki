package com.mythicrpg.client.titles;

import com.mythicrpg.client.ui.MythicInventoryTabs;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.network.TitleSelectionPayload;
import com.mythicrpg.network.TitleStatePayload;
import com.mythicrpg.network.TitleStateRequestPayload;
import com.mythicrpg.titles.TitleColor;
import com.mythicrpg.titles.TitleDefinition;
import com.mythicrpg.titles.TitleFinish;
import com.mythicrpg.titles.TitleRegistry;
import com.mythicrpg.titles.TitleTextFormatter;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Drawable;

import java.util.ArrayList;
import java.util.List;

public final class TitleSelectionScreen extends Screen {
    private static final int PANEL_WIDTH = 304;
    private static final int PANEL_HEIGHT = 210;
    private static final int COLUMN_WIDTH = 92;
    private static final int COLUMN_GAP = 4;
    private static final int ROW_HEIGHT = 18;
    private static final int VISIBLE_ROWS = 5;
    private static final int SELECTED_ROW = 2;

    private static final int TEXT = VanillaContainerUi.TEXT;
    private static final int MUTED_TEXT = 0xFF777777;
    private static final int FAINT_TEXT = 0xFF999999;
    private static final int SELECTED_FILL = 0xFF6F6F6F;
    private static final int SELECTED_BORDER = 0xFFFFFFFF;
    private static final int OUTLINE_COLOR = 0xFF202020;

    private final List<TitleTextFormatter.PaletteChoice> palettes = TitleTextFormatter.paletteChoices();
    private final List<TitleFinish> finishes = TitleFinish.DISPLAY_ORDER;

    private List<String> titleIds = List.of("");
    private int titleIndex;
    private int paletteIndex;
    private int finishIndex;
    private int focusedColumn;
    private boolean loaded;
    private boolean applying;

    private int panelX;
    private int panelY;
    private ButtonWidget applyButton;

    public TitleSelectionScreen() {
        super(Text.translatable("screen.mythicrpg.titles"));
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;

        applyButton = ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.titles.apply"),
                        button -> applySelection()
                )
                .dimensions(panelX + 72, panelY + 184, 76, 20)
                .build();
        applyButton.active = loaded && !applying;
        addDrawableChild(applyButton);

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.titles.cancel"),
                        button -> openInventory()
                )
                .dimensions(panelX + 156, panelY + 184, 76, 20)
                .build());

        if (ClientTitleState.hasReceivedState()) {
            ClientTitleState.Snapshot cached = ClientTitleState.snapshot();
            acceptState(new TitleStatePayload(
                    cached.unlockedTitleIds(),
                    cached.activeTitleId(),
                    cached.primaryColorId(),
                    cached.secondaryColorId(),
                    cached.gradient(),
                    cached.finishId()
            ));
        }
        requestState();
    }

    public void acceptState(TitleStatePayload payload) {
        ClientTitleState.update(payload);
        List<String> ordered = new ArrayList<>();
        ordered.add("");
        TitleRegistry.all().stream()
                .filter(TitleDefinition::selectable)
                .filter(definition -> payload.unlockedTitleIds().contains(definition.id()))
                .map(TitleDefinition::id)
                .forEach(ordered::add);
        titleIds = List.copyOf(ordered);

        titleIndex = Math.max(0, titleIds.indexOf(payload.activeTitleId()));
        paletteIndex = findPaletteIndex(
                payload.primaryColorId(),
                payload.secondaryColorId(),
                payload.gradient()
        );
        finishIndex = Math.max(0, finishes.indexOf(
                TitleFinish.fromId(payload.finishId()).orElse(TitleFinish.NONE)
        ));
        loaded = true;
        applying = false;
        if (applyButton != null) {
            applyButton.active = true;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        renderTabs(context, mouseX, mouseY);
        VanillaContainerUi.drawPanel(context, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        context.drawCenteredTextWithShadow(
                textRenderer,
                title,
                panelX + PANEL_WIDTH / 2,
                panelY + 7,
                TEXT
        );

        if (!loaded) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.translatable("screen.mythicrpg.titles.loading"),
                    panelX + PANEL_WIDTH / 2,
                    panelY + 98,
                    MUTED_TEXT
            );
            renderWidgets(context, mouseX, mouseY, delta);
            MythicInventoryTabs.renderTooltip(context, textRenderer, panelX, panelY, PANEL_WIDTH, mouseX, mouseY);
            return;
        }

        drawPreview(context);
        drawColumnHeaders(context);
        drawSelectionBand(context);
        drawCarousel(context, 0, mouseX, mouseY);
        drawCarousel(context, 1, mouseX, mouseY);
        drawCarousel(context, 2, mouseX, mouseY);
        drawArrowControls(context, mouseX, mouseY);

        context.drawTextWithShadow(
                textRenderer,
                Text.translatable(
                        "screen.mythicrpg.titles.unlocked_count",
                        Math.max(0, titleIds.size() - 1),
                        TitleRegistry.selectableCount()
                ),
                panelX + 10,
                panelY + 170,
                MUTED_TEXT
        );

        renderWidgets(context, mouseX, mouseY, delta);
        MythicInventoryTabs.renderTooltip(context, textRenderer, panelX, panelY, PANEL_WIDTH, mouseX, mouseY);
    }

    private void renderWidgets(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta
    ) {
        for (var child : children()) {
            if (child instanceof Drawable drawable) {
                drawable.render(context, mouseX, mouseY, delta);
            }
        }
    }

    private void drawPreview(DrawContext context) {
        int boxX = panelX + 10;
        int boxY = panelY + 22;
        int boxWidth = PANEL_WIDTH - 20;
        context.fill(boxX, boxY, boxX + boxWidth, boxY + 25, VanillaContainerUi.DARK_SHADOW);
        context.fill(boxX + 1, boxY + 1, boxX + boxWidth - 1, boxY + 24, VanillaContainerUi.SHADOW);

        String playerName = MinecraftClient.getInstance().player == null
                ? I18n.translate("generic.mythicrpg.player")
                : MinecraftClient.getInstance().player.getName().getString();
        String titleString = selectedTitleString();
        TitleTextFormatter.PaletteChoice palette = palettes.get(paletteIndex);
        TitleFinish finish = finishes.get(finishIndex);

        if (titleString.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(playerName),
                    panelX + PANEL_WIDTH / 2,
                    boxY + 8,
                    0xFFFFFFFF
            );
            return;
        }

        MutableText styledTitle = TitleTextFormatter.format(
                titleString,
                palette.primary(),
                palette.secondary(),
                palette.gradient(),
                finish,
                false
        );
        Text separatorAndName = Text.literal("  |  " + playerName).setStyle(Style.EMPTY.withColor(0xFFFFFF));
        int titleWidth = textRenderer.getWidth(styledTitle);
        int suffixWidth = textRenderer.getWidth(separatorAndName);
        int startX = panelX + (PANEL_WIDTH - titleWidth - suffixWidth) / 2;
        int textY = boxY + 8;

        if (finish == TitleFinish.BORDER) {
            Text outline = Text.literal(titleString).setStyle(Style.EMPTY.withColor(OUTLINE_COLOR));
            for (int offsetX = -1; offsetX <= 1; offsetX++) {
                for (int offsetY = -1; offsetY <= 1; offsetY++) {
                    if (offsetX == 0 && offsetY == 0) {
                        continue;
                    }
                    context.drawText(textRenderer, outline, startX + offsetX, textY + offsetY, OUTLINE_COLOR, false);
                }
            }
        }

        context.drawTextWithShadow(textRenderer, styledTitle, startX, textY, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, separatorAndName, startX + titleWidth, textY, 0xFFFFFFFF);
    }

    private void drawColumnHeaders(DrawContext context) {
        Text[] headers = {
                Text.translatable("screen.mythicrpg.titles.column.title"),
                Text.translatable("screen.mythicrpg.titles.column.color"),
                Text.translatable("screen.mythicrpg.titles.column.finish")
        };

        for (int column = 0; column < 3; column++) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    headers[column],
                    columnX(column) + COLUMN_WIDTH / 2,
                    panelY + 51,
                    focusedColumn == column ? TEXT : MUTED_TEXT
            );
        }
    }

    private void drawSelectionBand(DrawContext context) {
        int top = rowsTop() + SELECTED_ROW * ROW_HEIGHT;
        context.fill(panelX + 7, top - 1, panelX + PANEL_WIDTH - 7, top + ROW_HEIGHT + 1, SELECTED_BORDER);
        context.fill(panelX + 8, top, panelX + PANEL_WIDTH - 8, top + ROW_HEIGHT, SELECTED_FILL);
        for (int column = 1; column < 3; column++) {
            int separatorX = columnX(column) - COLUMN_GAP / 2;
            context.fill(separatorX, top + 2, separatorX + 1, top + ROW_HEIGHT - 2, VanillaContainerUi.DARK_SHADOW);
        }
    }

    private void drawCarousel(DrawContext context, int column, int mouseX, int mouseY) {
        int current = selectedIndex(column);
        int size = optionCount(column);
        int x = columnX(column);
        context.enableScissor(x + 1, rowsTop(), x + COLUMN_WIDTH - 1, rowsTop() + VISIBLE_ROWS * ROW_HEIGHT);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int offset = row - SELECTED_ROW;
            int optionIndex = Math.floorMod(current + offset, size);
            int rowY = rowsTop() + row * ROW_HEIGHT;
            int color = row == SELECTED_ROW ? 0xFFFFFFFF : (Math.abs(offset) == 1 ? FAINT_TEXT : MUTED_TEXT);
            Text option = fitCarouselText(optionText(column, optionIndex));
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    option,
                    x + COLUMN_WIDTH / 2,
                    rowY + 5,
                    color
            );
        }
        context.disableScissor();
    }

    private void drawArrowControls(DrawContext context, int mouseX, int mouseY) {
        for (int column = 0; column < 3; column++) {
            int center = columnX(column) + COLUMN_WIDTH / 2;
            boolean upHovered = inside(mouseX, mouseY, center - 9, panelY + 62, 18, 10);
            boolean downHovered = inside(mouseX, mouseY, center - 9, panelY + 164, 18, 10);
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("▲"),
                    center,
                    panelY + 62,
                    upHovered ? 0xFFFFFFFF : MUTED_TEXT
            );
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("▼"),
                    center,
                    panelY + 164,
                    downHovered ? 0xFFFFFFFF : MUTED_TEXT
            );
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MythicInventoryTabs.isOverInventoryTab(panelX, panelY, PANEL_WIDTH, mouseX, mouseY)) {
                openInventory();
                return true;
            }
            if (MythicInventoryTabs.isOverMythicCraftingTab(panelX, panelY, PANEL_WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenMythicCrafting();
                return true;
            }
            if (MythicInventoryTabs.isOverTravelingCompassTab(panelX, panelY, PANEL_WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenTravelingCompass();
                return true;
            }
            if (MythicInventoryTabs.isOverFossilCodexTab(panelX, panelY, PANEL_WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenFossilCodex();
                return true;
            }
            if (MythicInventoryTabs.isOverEatingCodexTab(panelX, panelY, PANEL_WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenEatingCodex();
                return true;
            }
            if (MythicInventoryTabs.isOverFishingCodexTab(panelX, panelY, PANEL_WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenFishingCodex();
                return true;
            }

            if (loaded) {
                for (int column = 0; column < 3; column++) {
                    int center = columnX(column) + COLUMN_WIDTH / 2;
                    if (inside(mouseX, mouseY, center - 9, panelY + 62, 18, 10)) {
                        focusedColumn = column;
                        step(column, -1);
                        return true;
                    }
                    if (inside(mouseX, mouseY, center - 9, panelY + 164, 18, 10)) {
                        focusedColumn = column;
                        step(column, 1);
                        return true;
                    }

                    int columnX = columnX(column);
                    if (inside(mouseX, mouseY, columnX, rowsTop(), COLUMN_WIDTH, VISIBLE_ROWS * ROW_HEIGHT)) {
                        focusedColumn = column;
                        int clickedRow = (int) ((mouseY - rowsTop()) / ROW_HEIGHT);
                        step(column, clickedRow - SELECTED_ROW);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (loaded) {
            for (int column = 0; column < 3; column++) {
                if (inside(mouseX, mouseY, columnX(column), panelY + 60, COLUMN_WIDTH, 116)) {
                    focusedColumn = column;
                    step(column, verticalAmount > 0.0D ? -1 : 1);
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (loaded) {
            if (keyCode == GLFW.GLFW_KEY_LEFT) {
                focusedColumn = Math.floorMod(focusedColumn - 1, 3);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_RIGHT) {
                focusedColumn = (focusedColumn + 1) % 3;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_UP) {
                step(focusedColumn, -1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                step(focusedColumn, 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                applySelection();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        openInventory();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void requestState() {
        ClientPlayNetworking.send(new TitleStateRequestPayload());
    }

    private void applySelection() {
        if (!loaded || applying) {
            return;
        }
        TitleTextFormatter.PaletteChoice palette = palettes.get(paletteIndex);
        applying = true;
        applyButton.active = false;
        ClientPlayNetworking.send(new TitleSelectionPayload(
                titleIds.get(titleIndex),
                palette.primary().id(),
                palette.secondary().id(),
                palette.gradient(),
                finishes.get(finishIndex).id()
        ));
    }

    private void openInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            MythicInventoryTabs.setScreenPreservingMouse(new InventoryScreen(client.player));
        } else {
            client.setScreen(null);
        }
    }

    private void renderTabs(DrawContext context, int mouseX, int mouseY) {
        MythicInventoryTabs.renderInventoryTab(context, panelX, panelY, PANEL_WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderMythicCraftingTab(context, panelX, panelY, PANEL_WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderTravelingCompassTab(context, panelX, panelY, PANEL_WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderFossilCodexTab(context, panelX, panelY, PANEL_WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderEatingCodexTab(context, panelX, panelY, PANEL_WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderFishingCodexTab(context, panelX, panelY, PANEL_WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderTitlesTab(context, panelX, panelY, PANEL_WIDTH, mouseX, mouseY, true);
    }

    private void step(int column, int amount) {
        if (amount == 0) {
            return;
        }
        switch (column) {
            case 0 -> titleIndex = Math.floorMod(titleIndex + amount, titleIds.size());
            case 1 -> paletteIndex = Math.floorMod(paletteIndex + amount, palettes.size());
            case 2 -> finishIndex = Math.floorMod(finishIndex + amount, finishes.size());
            default -> throw new IllegalArgumentException("Unknown title carousel column: " + column);
        }
    }

    private int selectedIndex(int column) {
        return switch (column) {
            case 0 -> titleIndex;
            case 1 -> paletteIndex;
            case 2 -> finishIndex;
            default -> 0;
        };
    }

    private int optionCount(int column) {
        return switch (column) {
            case 0 -> titleIds.size();
            case 1 -> palettes.size();
            case 2 -> finishes.size();
            default -> 1;
        };
    }

    private Text optionText(int column, int optionIndex) {
        return switch (column) {
            case 0 -> titleOptionText(titleIds.get(optionIndex));
            case 1 -> paletteText(palettes.get(optionIndex));
            case 2 -> finishText(finishes.get(optionIndex));
            default -> Text.empty();
        };
    }

    private Text fitCarouselText(Text option) {
        int maxWidth = COLUMN_WIDTH - 8;
        String raw = option.getString();

        if (textRenderer.getWidth(raw) <= maxWidth) {
            return option;
        }

        String ellipsis = "…";
        int contentWidth = Math.max(
                1,
                maxWidth - textRenderer.getWidth(ellipsis)
        );

        return Text.literal(
                textRenderer.trimToWidth(raw, contentWidth) + ellipsis
        );
    }

    private Text titleOptionText(String titleId) {
        if (titleId.isBlank()) {
            return Text.translatable("screen.mythicrpg.titles.none");
        }
        return TitleRegistry.get(titleId)
                .map(definition -> Text.translatable(definition.translationKey()))
                .orElse(Text.translatable("screen.mythicrpg.titles.none"));
    }

    private Text paletteText(TitleTextFormatter.PaletteChoice palette) {
        String label = palette.gradient()
                ? I18n.translate(
                        "screen.mythicrpg.titles.gradient",
                        I18n.translate(palette.primary().translationKey()),
                        I18n.translate(palette.secondary().translationKey())
                )
                : I18n.translate(palette.primary().translationKey());
        return TitleTextFormatter.format(
                label,
                palette.primary(),
                palette.secondary(),
                palette.gradient(),
                TitleFinish.NONE,
                false
        );
    }

    private Text finishText(TitleFinish finish) {
        return TitleTextFormatter.format(
                I18n.translate(finish.translationKey()),
                TitleColor.WHITE,
                TitleColor.WHITE,
                false,
                finish,
                true
        );
    }

    private String selectedTitleString() {
        String selectedId = titleIds.get(titleIndex);
        if (selectedId.isBlank()) {
            return "";
        }
        return TitleRegistry.get(selectedId)
                .map(definition -> I18n.translate(definition.translationKey()))
                .orElse("");
    }

    private int findPaletteIndex(String primary, String secondary, boolean gradient) {
        for (int index = 0; index < palettes.size(); index++) {
            TitleTextFormatter.PaletteChoice palette = palettes.get(index);
            if (palette.primary().id().equals(primary)
                    && palette.gradient() == gradient
                    && (!gradient || palette.secondary().id().equals(secondary))) {
                return index;
            }
        }
        return 0;
    }

    private int columnX(int column) {
        return panelX + 10 + column * (COLUMN_WIDTH + COLUMN_GAP);
    }

    private int rowsTop() {
        return panelY + 74;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
