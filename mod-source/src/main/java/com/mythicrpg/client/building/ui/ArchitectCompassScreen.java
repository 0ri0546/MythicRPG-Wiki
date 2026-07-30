package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.ArchitectCompassData;
import com.mythicrpg.building.ArchitectCompassUiActionPayload;
import com.mythicrpg.building.ArchitectCompassUiStatePayload;
import com.mythicrpg.client.ClientSkillTreeState;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.core.SkillType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.OptionalInt;

/** Vanilla-style interface for the Architect's Compass center, radius and axis. */
public final class ArchitectCompassScreen extends VanillaBuildingScreen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 224;
    private static final int PREVIEW_X_OFFSET = 10;
    private static final int PREVIEW_Y_OFFSET = 24;
    private static final int PREVIEW_WIDTH = 150;
    private static final int PREVIEW_HEIGHT = 164;
    private static final int RIGHT_X_OFFSET = 170;
    private static final int RIGHT_WIDTH = 180;
    private static final int INVALID_BORDER = 0xFFE03A3A;
    private static final int GUIDE_COLOR = 0xFF38B8E8;
    private static final int CENTER_COLOR = 0xFFFFD45A;

    private final Hand hand;
    private String dimensionId;
    private BlockPos initialCenter;
    private int initialRadius;
    private int axisId;
    private boolean waitingForServer;
    private boolean suppressChanges;
    private String statusKey;
    private boolean statusError;

    private BuildingCoordinateEditor centerEditor;
    private TextFieldWidget radiusField;
    private ButtonWidget axisXButton;
    private ButtonWidget axisYButton;
    private ButtonWidget axisZButton;
    private ButtonWidget applyButton;

    public ArchitectCompassScreen(ArchitectCompassUiStatePayload state) {
        super(Text.translatable("screen.mythicrpg.architect_compass_ui.title"), PANEL_WIDTH, PANEL_HEIGHT);
        this.hand = state.handId() == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND;
        this.dimensionId = state.dimensionId();
        this.initialCenter = state.center();
        this.initialRadius = state.radius();
        this.axisId = state.axisId();
        this.statusKey = state.messageKey();
        this.statusError = state.error();
    }

    @Override
    protected void initBuildingScreen() {
        int rightX = panelX + RIGHT_X_OFFSET + 8;
        centerEditor = new BuildingCoordinateEditor(
                textRenderer,
                rightX,
                panelY + 34,
                42,
                Text.translatable("screen.mythicrpg.architect_compass_ui.center"),
                initialCenter,
                this::addDrawableChild,
                this::onValuesChanged
        );

        radiusField = new TextFieldWidget(
                textRenderer,
                rightX,
                panelY + 98,
                72,
                18,
                Text.translatable("screen.mythicrpg.architect_compass_ui.radius")
        );
        radiusField.setMaxLength(2);
        radiusField.setTextPredicate(ArchitectCompassScreen::isPotentialRadius);
        radiusField.setText(Integer.toString(initialRadius));
        radiusField.setChangedListener(value -> {
            if (!suppressChanges) {
                onValuesChanged();
            }
        });
        addDrawableChild(radiusField);

        int axisY = panelY + 140;
        axisXButton = addDrawableChild(ButtonWidget.builder(
                        Text.literal("X"),
                        button -> selectAxis(0)
                ).dimensions(rightX, axisY, 50, 20).build());
        axisYButton = addDrawableChild(ButtonWidget.builder(
                        Text.literal("Y"),
                        button -> selectAxis(1)
                ).dimensions(rightX + 56, axisY, 50, 20).build());
        axisZButton = addDrawableChild(ButtonWidget.builder(
                        Text.literal("Z"),
                        button -> selectAxis(2)
                ).dimensions(rightX + 112, axisY, 50, 20).build());

        applyButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.architect_compass_ui.apply"),
                        button -> apply()
                ).dimensions(panelX + 92, panelY + 194, 84, 20).build());
        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.cancel"),
                        button -> close()
                ).dimensions(panelX + 184, panelY + 194, 84, 20).build());

        refreshAxisButtons();
        refreshValidity();
    }

    @Override
    protected void renderBuildingContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawSection(
                context,
                panelX + PREVIEW_X_OFFSET,
                panelY + PREVIEW_Y_OFFSET,
                PREVIEW_WIDTH,
                PREVIEW_HEIGHT,
                Text.translatable("screen.mythicrpg.architect_compass_ui.preview")
        );
        drawSection(
                context,
                panelX + RIGHT_X_OFFSET,
                panelY + PREVIEW_Y_OFFSET,
                RIGHT_WIDTH,
                PREVIEW_HEIGHT,
                null
        );

        centerEditor.render(context, textRenderer);
        context.drawText(
                textRenderer,
                Text.translatable("screen.mythicrpg.architect_compass_ui.radius"),
                panelX + RIGHT_X_OFFSET + 8,
                panelY + 84,
                VanillaContainerUi.TEXT,
                false
        );
        context.drawText(
                textRenderer,
                Text.translatable("screen.mythicrpg.architect_compass_ui.axis"),
                panelX + RIGHT_X_OFFSET + 8,
                panelY + 126,
                VanillaContainerUi.TEXT,
                false
        );
        drawRadiusInvalidBorder(context);
        drawPreview(context);

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
        if (!isHeldToolValid()
                || !isCurrentDimension()
                || !ClientSkillTreeState.isUnlocked(SkillType.BUILDING, 14)) {
            close();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && applyButton != null && applyButton.active) {
            apply();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean accepts(ArchitectCompassUiStatePayload state) {
        return state != null && state.handId() == (hand == Hand.MAIN_HAND ? 0 : 1);
    }

    public void acceptState(ArchitectCompassUiStatePayload state) {
        if (!accepts(state)) {
            return;
        }
        waitingForServer = false;
        statusKey = state.messageKey();
        statusError = state.error();
        dimensionId = state.dimensionId();
        initialCenter = state.center();
        initialRadius = state.radius();
        axisId = state.axisId();

        if (!state.openScreen() && !state.error()) {
            close();
            return;
        }

        if (centerEditor != null) {
            suppressChanges = true;
            try {
                centerEditor.setPosition(state.center());
                radiusField.setText(Integer.toString(state.radius()));
            } finally {
                suppressChanges = false;
            }
            refreshAxisButtons();
            setControlsActive(true);
            refreshValidity();
        }
    }

    private void onValuesChanged() {
        statusKey = "";
        statusError = false;
        refreshValidity();
    }

    private void selectAxis(int selectedAxis) {
        axisId = Math.floorMod(selectedAxis, 3);
        BuildingUiSounds.rotate();
        statusKey = "";
        statusError = false;
        refreshAxisButtons();
        refreshValidity();
    }

    private void refreshAxisButtons() {
        if (axisXButton == null) {
            return;
        }
        axisXButton.setMessage(Text.literal(axisId == 0 ? "[X]" : "X"));
        axisYButton.setMessage(Text.literal(axisId == 1 ? "[Y]" : "Y"));
        axisZButton.setMessage(Text.literal(axisId == 2 ? "[Z]" : "Z"));
    }

    private void refreshValidity() {
        if (applyButton != null) {
            applyButton.active = !waitingForServer
                    && centerEditor != null
                    && centerEditor.isValid()
                    && radius().isPresent();
        }
    }

    private void apply() {
        if (waitingForServer || centerEditor == null) {
            return;
        }
        BlockPos center = centerEditor.position().orElse(null);
        OptionalInt radius = radius();
        if (center == null || radius.isEmpty()) {
            refreshValidity();
            return;
        }

        waitingForServer = true;
        setControlsActive(false);
        statusKey = "screen.mythicrpg.architect_compass_ui.saving";
        statusError = false;
        ClientPlayNetworking.send(new ArchitectCompassUiActionPayload(
                hand == Hand.MAIN_HAND ? 0 : 1,
                center.getX(),
                center.getY(),
                center.getZ(),
                radius.getAsInt(),
                axisId
        ));
    }

    private void setControlsActive(boolean active) {
        centerEditor.setEditable(active);
        radiusField.setEditable(active);
        radiusField.active = active;
        axisXButton.active = active;
        axisYButton.active = active;
        axisZButton.active = active;
        applyButton.active = active && centerEditor.isValid() && radius().isPresent();
    }

    private OptionalInt radius() {
        if (radiusField == null) {
            return OptionalInt.empty();
        }
        try {
            int value = Integer.parseInt(radiusField.getText());
            if (value < ArchitectCompassData.MIN_RADIUS || value > ArchitectCompassData.MAX_RADIUS) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(value);
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    private void drawRadiusInvalidBorder(DrawContext context) {
        if (radiusField == null || radius().isPresent()) {
            return;
        }
        context.fill(
                radiusField.getX() - 1,
                radiusField.getY() - 1,
                radiusField.getX() + radiusField.getWidth() + 1,
                radiusField.getY() + radiusField.getHeight() + 1,
                INVALID_BORDER
        );
    }

    private void drawPreview(DrawContext context) {
        int centerX = panelX + PREVIEW_X_OFFSET + PREVIEW_WIDTH / 2;
        int centerY = panelY + PREVIEW_Y_OFFSET + 79;
        int axisColor = 0xFF707070;

        switch (axisId) {
            case 0 -> context.fill(centerX - 52, centerY - 1, centerX + 53, centerY + 1, axisColor);
            case 1 -> context.fill(centerX - 1, centerY - 49, centerX + 1, centerY + 50, axisColor);
            default -> {
                for (int offset = -42; offset <= 42; offset++) {
                    int x = centerX + offset;
                    int y = centerY - offset / 2;
                    context.fill(x, y, x + 2, y + 2, axisColor);
                }
            }
        }

        for (int index = 0; index < 96; index++) {
            double angle = Math.PI * 2.0D * index / 96.0D;
            double cosine = Math.cos(angle);
            double sine = Math.sin(angle);
            int x;
            int y;
            if (axisId == 0) {
                x = centerX + (int) Math.round(sine * 28.0D);
                y = centerY + (int) Math.round(-cosine * 45.0D + sine * 9.0D);
            } else if (axisId == 1) {
                x = centerX + (int) Math.round(cosine * 48.0D + sine * 16.0D);
                y = centerY + (int) Math.round(sine * 18.0D);
            } else {
                x = centerX + (int) Math.round(cosine * 43.0D);
                y = centerY + (int) Math.round(-sine * 43.0D);
            }
            context.fill(x - 1, y - 1, x + 2, y + 2, GUIDE_COLOR);
        }
        context.fill(centerX - 3, centerY - 3, centerX + 4, centerY + 4, CENTER_COLOR);

        Text axis = Text.translatable(
                "screen.mythicrpg.architect_compass_ui.axis_value",
                axisName(axisId)
        );
        context.drawCenteredTextWithShadow(
                textRenderer,
                axis,
                centerX,
                panelY + PREVIEW_Y_OFFSET + 132,
                VanillaContainerUi.TEXT
        );

        BlockPos center = centerEditor == null
                ? initialCenter
                : centerEditor.position().orElse(initialCenter);
        int radius = radius().orElse(ArchitectCompassData.DEFAULT_RADIUS);
        Text summary = Text.translatable(
                "screen.mythicrpg.architect_compass_ui.summary",
                center.getX(),
                center.getY(),
                center.getZ(),
                radius
        );
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                summary,
                centerX,
                panelY + PREVIEW_Y_OFFSET + 148,
                VanillaContainerUi.DISABLED_TEXT,
                false
        );
    }

    private static Text axisName(int axisId) {
        return Text.literal(switch (Math.floorMod(axisId, 3)) {
            case 0 -> "X";
            case 1 -> "Y";
            default -> "Z";
        });
    }

    private boolean isHeldToolValid() {
        ItemStack stack = heldStack();
        return stack.isOf(ModItems.ARCHITECT_COMPASS);
    }

    private ItemStack heldStack() {
        if (client == null || client.player == null) {
            return ItemStack.EMPTY;
        }
        return client.player.getStackInHand(hand);
    }

    private boolean isCurrentDimension() {
        return client != null
                && client.world != null
                && client.world.getRegistryKey().getValue().toString().equals(dimensionId);
    }

    private static boolean isPotentialRadius(String value) {
        if (value == null || value.length() > 2) {
            return false;
        }
        if (value.isEmpty()) {
            return true;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
