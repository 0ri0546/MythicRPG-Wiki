package com.mythicrpg.client.building.ui;

import com.mythicrpg.client.ui.VanillaContainerUi;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Three compact integer fields representing one world-space block coordinate. */
public final class BuildingCoordinateEditor {
    private static final int FIELD_HEIGHT = 18;
    private static final int LABEL_WIDTH = 10;
    private static final int GAP = 4;
    private static final int INVALID_BORDER = 0xFFE03A3A;

    private final Text label;
    private final int x;
    private final int y;
    private final int fieldWidth;
    private final TextFieldWidget xField;
    private final TextFieldWidget yField;
    private final TextFieldWidget zField;
    private boolean suppressChanges;

    public BuildingCoordinateEditor(
            TextRenderer textRenderer,
            int x,
            int y,
            int fieldWidth,
            Text label,
            BlockPos initial,
            Consumer<ClickableWidget> registrar,
            Runnable changed
    ) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.fieldWidth = fieldWidth;

        int fieldsY = y + 14;
        xField = createField(textRenderer, x + LABEL_WIDTH, fieldsY, fieldWidth, "X", changed);
        yField = createField(
                textRenderer,
                x + LABEL_WIDTH + fieldWidth + GAP + LABEL_WIDTH,
                fieldsY,
                fieldWidth,
                "Y",
                changed
        );
        zField = createField(
                textRenderer,
                x + (LABEL_WIDTH + fieldWidth + GAP) * 2 + LABEL_WIDTH,
                fieldsY,
                fieldWidth,
                "Z",
                changed
        );

        registrar.accept(xField);
        registrar.accept(yField);
        registrar.accept(zField);
        setPosition(initial == null ? BlockPos.ORIGIN : initial);
    }

    private TextFieldWidget createField(
            TextRenderer textRenderer,
            int fieldX,
            int fieldY,
            int width,
            String axis,
            Runnable changed
    ) {
        TextFieldWidget field = new TextFieldWidget(
                textRenderer,
                fieldX,
                fieldY,
                width,
                FIELD_HEIGHT,
                Text.literal(axis)
        );
        field.setMaxLength(11);
        field.setTextPredicate(BuildingCoordinateEditor::isPotentialInteger);
        field.setChangedListener(value -> {
            if (!suppressChanges && changed != null) {
                changed.run();
            }
        });
        return field;
    }

    public void render(DrawContext context, TextRenderer textRenderer) {
        context.drawText(
                textRenderer,
                label,
                x,
                y,
                VanillaContainerUi.TEXT,
                false
        );
        drawAxisLabel(context, textRenderer, "X", x, y + 19);
        drawAxisLabel(
                context,
                textRenderer,
                "Y",
                x + LABEL_WIDTH + fieldWidth + GAP,
                y + 19
        );
        drawAxisLabel(
                context,
                textRenderer,
                "Z",
                x + (LABEL_WIDTH + fieldWidth + GAP) * 2,
                y + 19
        );

        drawInvalidBorder(context, xField);
        drawInvalidBorder(context, yField);
        drawInvalidBorder(context, zField);
    }

    private static void drawAxisLabel(
            DrawContext context,
            TextRenderer renderer,
            String axis,
            int labelX,
            int labelY
    ) {
        context.drawText(renderer, axis, labelX + 1, labelY, VanillaContainerUi.TEXT, false);
    }

    private static void drawInvalidBorder(DrawContext context, TextFieldWidget field) {
        if (parse(field.getText()).isPresent()) {
            return;
        }
        context.fill(
                field.getX() - 1,
                field.getY() - 1,
                field.getX() + field.getWidth() + 1,
                field.getY() + field.getHeight() + 1,
                INVALID_BORDER
        );
    }

    public Optional<BlockPos> position() {
        Optional<Integer> parsedX = parse(xField.getText());
        Optional<Integer> parsedY = parse(yField.getText());
        Optional<Integer> parsedZ = parse(zField.getText());
        if (parsedX.isEmpty() || parsedY.isEmpty() || parsedZ.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BlockPos(parsedX.get(), parsedY.get(), parsedZ.get()));
    }

    public boolean isValid() {
        return position().isPresent();
    }

    public void setPosition(BlockPos position) {
        BlockPos safe = position == null ? BlockPos.ORIGIN : position;
        suppressChanges = true;
        try {
            xField.setText(Integer.toString(safe.getX()));
            yField.setText(Integer.toString(safe.getY()));
            zField.setText(Integer.toString(safe.getZ()));
        } finally {
            suppressChanges = false;
        }
    }

    public void setEditable(boolean editable) {
        for (TextFieldWidget field : fields()) {
            field.setEditable(editable);
            field.active = editable;
        }
    }

    public List<TextFieldWidget> fields() {
        return List.of(xField, yField, zField);
    }

    private static boolean isPotentialInteger(String value) {
        if (value == null || value.length() > 11) {
            return false;
        }
        if (value.isEmpty() || value.equals("-")) {
            return true;
        }
        int start = value.charAt(0) == '-' ? 1 : 0;
        if (start == value.length()) {
            return false;
        }
        for (int index = start; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private static Optional<Integer> parse(String value) {
        if (value == null || value.isBlank() || value.equals("-")) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
