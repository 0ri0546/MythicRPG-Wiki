package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.BuildingRotationAxis;
import com.mythicrpg.building.BuildingStructureRotation;
import com.mythicrpg.client.ui.VanillaContainerUi;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/** Three explicit +90-degree buttons with a compact current-angle readout. */
public final class BuildingRotationControls {
    private static final int BUTTON_WIDTH = 54;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    private final int x;
    private final int y;
    private final Map<BuildingRotationAxis, ButtonWidget> buttons =
            new EnumMap<>(BuildingRotationAxis.class);
    private BuildingStructureRotation rotation;

    public BuildingRotationControls(
            int x,
            int y,
            BuildingStructureRotation initial,
            Consumer<ClickableWidget> registrar,
            Consumer<BuildingRotationAxis> rotated
    ) {
        this.x = x;
        this.y = y;
        this.rotation = initial == null ? BuildingStructureRotation.NONE : initial;

        int index = 0;
        for (BuildingRotationAxis axis : BuildingRotationAxis.values()) {
            ButtonWidget button = ButtonWidget.builder(
                            buttonText(axis),
                            widget -> {
                                rotation = rotation.rotate(axis);
                                refreshMessages();
                                if (rotated != null) {
                                    rotated.accept(axis);
                                }
                            }
                    )
                    .dimensions(
                            x + index * (BUTTON_WIDTH + GAP),
                            y + 14,
                            BUTTON_WIDTH,
                            BUTTON_HEIGHT
                    )
                    .build();
            buttons.put(axis, button);
            registrar.accept(button);
            index++;
        }
    }

    public void render(DrawContext context, TextRenderer textRenderer) {
        context.drawText(
                textRenderer,
                Text.translatable("screen.mythicrpg.building_ui.rotation"),
                x,
                y,
                VanillaContainerUi.TEXT,
                false
        );
    }

    public BuildingStructureRotation rotation() {
        return rotation;
    }

    public void setRotation(BuildingStructureRotation rotation) {
        this.rotation = rotation == null ? BuildingStructureRotation.NONE : rotation;
        refreshMessages();
    }

    public void setActive(boolean active) {
        for (ButtonWidget button : buttons.values()) {
            button.active = active;
        }
    }

    private void refreshMessages() {
        for (BuildingRotationAxis axis : BuildingRotationAxis.values()) {
            buttons.get(axis).setMessage(buttonText(axis));
        }
    }

    private Text buttonText(BuildingRotationAxis axis) {
        return Text.translatable(
                "screen.mythicrpg.building_ui.rotate_axis",
                axis.name()
        );
    }
}
