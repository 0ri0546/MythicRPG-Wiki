package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.StaticDecorationEffect;
import com.mythicrpg.building.StaticDecorationUiActionPayload;
import com.mythicrpg.building.StaticDecorationUiStatePayload;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.core.ModBlocks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

/** Vanilla carousel for the closed catalog of 32 vanilla particle types. */
public final class StaticDecorationScreen extends VanillaBuildingScreen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 224;
    private static final int PREVIEW_X = 10;
    private static final int PREVIEW_Y = 24;
    private static final int PREVIEW_WIDTH = 150;
    private static final int PREVIEW_HEIGHT = 164;
    private static final int RIGHT_X = 170;
    private static final int RIGHT_WIDTH = 180;
    private static final int ROW_HEIGHT = 22;
    private static final int VISIBLE_ROWS = 5;
    private static final int SELECTED_ROW = 2;

    private final Hand hand;
    private final boolean editingBlock;
    private final BlockPos targetPos;
    private String dimensionId;
    private int selectedIndex;
    private boolean waitingForServer;
    private String statusKey;
    private boolean statusError;
    private long previewTicks;

    private ButtonWidget applyButton;

    public StaticDecorationScreen(StaticDecorationUiStatePayload state) {
        super(Text.translatable("screen.mythicrpg.static_decoration_ui.title"), PANEL_WIDTH, PANEL_HEIGHT);
        this.hand = state.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        this.editingBlock = state.editingBlock();
        this.targetPos = state.targetPos();
        this.dimensionId = state.dimensionId();
        this.selectedIndex = state.effectIndex();
        this.statusKey = state.messageKey();
        this.statusError = state.error();
    }

    @Override
    protected void initBuildingScreen() {
        StaticDecorationPreviewRenderer.clearCache();
        int rightCenter = panelX + RIGHT_X + RIGHT_WIDTH / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("▲"), button -> step(-1))
                .dimensions(rightCenter - 34, panelY + 31, 68, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("▼"), button -> step(1))
                .dimensions(rightCenter - 34, panelY + 165, 68, 18).build());

        applyButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable(editingBlock
                                ? "screen.mythicrpg.static_decoration_ui.apply"
                                : "screen.mythicrpg.static_decoration_ui.create"),
                        button -> apply()
                ).dimensions(panelX + 92, panelY + 194, 84, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(panelX + 184, panelY + 194, 84, 20).build());
    }

    @Override
    protected void renderBuildingContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawSection(context, panelX + PREVIEW_X, panelY + PREVIEW_Y,
                PREVIEW_WIDTH, PREVIEW_HEIGHT,
                Text.translatable("screen.mythicrpg.static_decoration_ui.preview"));
        drawSection(context, panelX + RIGHT_X, panelY + PREVIEW_Y,
                RIGHT_WIDTH, PREVIEW_HEIGHT,
                Text.translatable("screen.mythicrpg.static_decoration_ui.catalog"));

        StaticDecorationEffect selected = selectedEffect();
        StaticDecorationPreviewRenderer.render(
                context,
                selected,
                panelX + PREVIEW_X,
                panelY + PREVIEW_Y,
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT,
                previewTicks
        );
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.static_decoration_ui.volume"),
                panelX + PREVIEW_X + PREVIEW_WIDTH / 2,
                panelY + PREVIEW_Y + PREVIEW_HEIGHT - 13,
                VanillaContainerUi.DISABLED_TEXT,
                false
        );
        renderCarousel(context);

        if (!statusKey.isBlank()) {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.translatable(statusKey),
                    panelX + panelWidth / 2,
                    panelY + 216,
                    statusError ? 0xFFE05050 : 0xFF55AA55
            );
        }
    }

    @Override
    public void tick() {
        super.tick();
        previewTicks++;
        if (!isCurrentDimension() || !isHeldGenerator()) {
            close();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0.0D) {
            step(verticalAmount > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int rowsTop = panelY + 51;
            int left = panelX + RIGHT_X + 8;
            int right = panelX + RIGHT_X + RIGHT_WIDTH - 8;
            if (mouseX >= left && mouseX < right
                    && mouseY >= rowsTop && mouseY < rowsTop + VISIBLE_ROWS * ROW_HEIGHT) {
                int row = (int) ((mouseY - rowsTop) / ROW_HEIGHT);
                int offset = row - SELECTED_ROW;
                if (offset != 0) step(offset);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP) {
            step(-1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            step(1);
            return true;
        }
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && applyButton != null && applyButton.active) {
            apply();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean accepts(StaticDecorationUiStatePayload state) {
        return state != null
                && state.handId() == (hand == Hand.MAIN_HAND ? 0 : 1)
                && state.editingBlock() == editingBlock
                && (!editingBlock || state.targetPos().equals(targetPos));
    }

    public void acceptState(StaticDecorationUiStatePayload state) {
        if (!accepts(state)) return;
        waitingForServer = false;
        statusKey = state.messageKey();
        statusError = state.error();
        dimensionId = state.dimensionId();
        selectedIndex = state.effectIndex();
        if (state.error()) {
            applyButton.active = true;
            if (!state.openScreen()) close();
            return;
        }
        if (!state.openScreen()) {
            close();
        }
    }

    private void renderCarousel(DrawContext context) {
        int left = panelX + RIGHT_X + 8;
        int top = panelY + 51;
        int width = RIGHT_WIDTH - 16;
        int count = StaticDecorationEffect.values().length;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int offset = row - SELECTED_ROW;
            int index = Math.floorMod(selectedIndex + offset, count);
            int y = top + row * ROW_HEIGHT;
            boolean selected = row == SELECTED_ROW;
            if (selected) {
                context.fill(left, y, left + width, y + ROW_HEIGHT - 2, 0xFF6F6F6F);
                context.drawBorder(left, y, width, ROW_HEIGHT - 2, 0xFFFFFFFF);
            }
            Text label = Text.translatable(StaticDecorationEffect.byIndex(index).translationKey());
            Text fitted = fit(label, width - 10);
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    fitted,
                    left + width / 2,
                    y + 6,
                    selected ? VanillaContainerUi.TEXT : VanillaContainerUi.DISABLED_TEXT
            );
        }
    }

    private Text fit(Text text, int maxWidth) {
        String raw = text.getString();
        if (textRenderer.getWidth(raw) <= maxWidth) return text;
        String ellipsis = "…";
        int content = Math.max(1, maxWidth - textRenderer.getWidth(ellipsis));
        return Text.literal(textRenderer.trimToWidth(raw, content) + ellipsis);
    }

    private void step(int amount) {
        if (waitingForServer || amount == 0) return;
        selectedIndex = Math.floorMod(
                selectedIndex + amount,
                StaticDecorationEffect.values().length
        );
        statusKey = "";
        statusError = false;
        BuildingUiSounds.navigate();
    }

    private void apply() {
        if (waitingForServer) return;
        waitingForServer = true;
        applyButton.active = false;
        statusKey = "screen.mythicrpg.static_decoration_ui.saving";
        statusError = false;
        ClientPlayNetworking.send(new StaticDecorationUiActionPayload(
                hand == Hand.MAIN_HAND ? 0 : 1,
                editingBlock,
                targetPos.asLong(),
                selectedIndex
        ));
    }

    private StaticDecorationEffect selectedEffect() {
        return StaticDecorationEffect.byIndex(selectedIndex);
    }

    private boolean isHeldGenerator() {
        if (client == null || client.player == null) return false;
        ItemStack stack = client.player.getStackInHand(hand);
        return stack.isOf(ModBlocks.STATIC_DECORATION.asItem());
    }

    private boolean isCurrentDimension() {
        return client != null
                && client.world != null
                && client.world.getRegistryKey().getValue().toString().equals(dimensionId);
    }
}
