package com.mythicrpg.client.fishing;

import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.client.ui.VanillaCustomScreen;
import com.mythicrpg.fishing.FishingRarity;
import com.mythicrpg.network.FishingMiniGameActionPayload;
import com.mythicrpg.network.FishingMiniGameOpenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Three calm, untimed Fishing challenges. Mythic catches can always be retried. */
public final class FishingMiniGameScreen extends VanillaCustomScreen {
    private static final int WIDTH = 310;
    private static final int HEIGHT = 204;

    private final FishingMiniGameOpenPayload payload;
    private int cursor;
    private boolean forward = true;

    private int boardMask;
    private int selectedShape;
    private int rotation;
    private boolean[] placedShapes;
    private int hoveredGridX = -1;
    private int hoveredGridY = -1;
    private ButtonWidget validateButton;

    public FishingMiniGameScreen(FishingMiniGameOpenPayload payload) {
        super(Text.translatable("screen.mythicrpg.fishing_minigame"), WIDTH, HEIGHT);
        this.payload = payload;
        this.placedShapes = new boolean[shapeTypes().length];
    }

    @Override
    protected void initVanillaScreen() {
        if (payload.gameType() == 0) {
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("screen.mythicrpg.fishing_minigame.stop"),
                    button -> sendAndClose(0, cursor)
            ).dimensions(panelX + WIDTH / 2 - 45, panelY + 162, 90, 20).build());
            return;
        }

        if (payload.gameType() == 1) {
            addDrawableChild(ButtonWidget.builder(
                    Text.translatable("screen.mythicrpg.fishing_minigame.keep"),
                    button -> sendAndClose(0, 0)
            ).dimensions(panelX + 54, panelY + 162, 90, 20).build());
            if (payload.mastery()) {
                addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.fishing_minigame.redraw"),
                        button -> sendAndClose(1, 0)
                ).dimensions(panelX + 166, panelY + 162, 90, 20).build());
            }
            return;
        }

        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.mythicrpg.fishing_minigame.reset"),
                button -> resetGrid()
        ).dimensions(panelX + 20, panelY + 172, 82, 20).build());
        addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.mythicrpg.fishing_minigame.rotate"),
                button -> rotateSelected()
        ).dimensions(panelX + 114, panelY + 172, 82, 20).build());
        validateButton = addDrawableChild(ButtonWidget.builder(
                Text.translatable("screen.mythicrpg.fishing_minigame.validate"),
                button -> sendAndClose(0, boardMask)
        ).dimensions(panelX + 208, panelY + 172, 82, 20).build());
        updateValidateButton();
    }

    @Override
    public void tick() {
        if (payload.gameType() == 0) {
            cursor += forward ? 2 : -2;
            if (cursor >= 100) {
                cursor = 100;
                forward = false;
            } else if (cursor <= 0) {
                cursor = 0;
                forward = true;
            }
        } else if (payload.gameType() == 2) {
            updateValidateButton();
        }
    }

    @Override
    protected void renderVanillaContent(DrawContext context, int mouseX, int mouseY, float delta) {
        FishingRarity rarity = FishingRarity.byRank(payload.rarityRank());
        context.drawCenteredTextWithShadow(
                textRenderer,
                rarity.displayName(),
                panelX + WIDTH / 2,
                panelY + 24,
                rarity.formatting().getColorValue() == null
                        ? VanillaContainerUi.TEXT
                        : 0xFF000000 | rarity.formatting().getColorValue()
        );

        if (payload.gameType() == 0) {
            renderPrecision(context);
        } else if (payload.gameType() == 1) {
            renderCards(context);
        } else {
            updateHoveredGrid(mouseX, mouseY);
            renderPackingPuzzle(context);
        }

        if (payload.guaranteed()) {
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.translatable("screen.mythicrpg.fishing_minigame.guaranteed"),
                    panelX + WIDTH / 2,
                    payload.gameType() == 2 ? panelY + 160 : panelY + 145,
                    0xFFC67C00,
                    false
            );
        }
    }

    private void renderPrecision(DrawContext context) {
        drawSection(
                context,
                panelX + 45,
                panelY + 52,
                WIDTH - 90,
                72,
                Text.translatable("screen.mythicrpg.fishing_minigame.precision")
        );
        int barX = panelX + 55;
        int barY = panelY + 88;
        int barWidth = 200;
        context.fill(barX, barY, barX + barWidth, barY + 12, 0xFF555555);
        context.fill(barX + payload.a() * 2, barY, barX + payload.b() * 2, barY + 12, 0xFF33AA55);
        context.fill(barX + cursor * 2 - 1, barY - 3, barX + cursor * 2 + 1, barY + 15, 0xFFFFFFFF);
    }

    private void renderCards(DrawContext context) {
        drawSection(
                context,
                panelX + 54,
                panelY + 50,
                88,
                78,
                Text.translatable("screen.mythicrpg.fishing_minigame.opponent")
        );
        drawSection(
                context,
                panelX + 168,
                panelY + 50,
                88,
                78,
                Text.translatable("screen.mythicrpg.fishing_minigame.player")
        );
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(Integer.toString(payload.a())),
                panelX + 98,
                panelY + 82,
                0xFFAA3333
        );
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal(Integer.toString(payload.b())),
                panelX + 212,
                panelY + 82,
                0xFF2C7A45
        );
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.fishing_minigame.tie_wins"),
                panelX + WIDTH / 2,
                panelY + 134,
                VanillaContainerUi.TEXT,
                false
        );
    }

    private void renderPackingPuzzle(DrawContext context) {
        int size = payload.b();
        int cell = gridCellSize();
        int startX = gridStartX();
        int startY = gridStartY();

        drawSection(
                context,
                panelX + 18,
                panelY + 38,
                172,
                124,
                Text.translatable("screen.mythicrpg.fishing_minigame.packing")
        );
        drawSection(
                context,
                panelX + 198,
                panelY + 38,
                94,
                124,
                Text.translatable("screen.mythicrpg.fishing_minigame.shapes")
        );

        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.fishing_minigame.packing_instruction"),
                panelX + 104,
                panelY + 49,
                VanillaContainerUi.TEXT,
                false
        );

        for (int index = 0; index < size * size; index++) {
            int cellX = startX + (index % size) * cell;
            int cellY = startY + (index / size) * cell;
            boolean occupied = (boardMask & (1 << index)) != 0;
            context.fill(
                    cellX + 1,
                    cellY + 1,
                    cellX + cell - 1,
                    cellY + cell - 1,
                    occupied ? 0xFF4F9A61 : 0xFF4A4A4A
            );
            context.drawBorder(cellX, cellY, cell, cell, 0xFFBDBDBD);
        }

        renderGhostPlacement(context);

        ShapeType[] shapes = shapeTypes();
        for (int index = 0; index < shapes.length; index++) {
            int boxX = panelX + 205 + (index % 2) * 40;
            int boxY = panelY + 57 + (index / 2) * 43;
            int background = placedShapes[index]
                    ? 0xFF343434
                    : index == selectedShape ? 0xFF315D3A : 0xFF595959;
            context.fill(boxX, boxY, boxX + 34, boxY + 32, background);
            context.drawBorder(boxX, boxY, 34, 32, index == selectedShape ? 0xFF8CD49A : 0xFFB0B0B0);
            drawShapePreview(
                    context,
                    shapes[index],
                    boxX + 5,
                    boxY + 6,
                    6,
                    index == selectedShape ? rotation : 0,
                    placedShapes[index] ? 0xFF777777 : 0xFF70C080
            );
            if (placedShapes[index]) {
                context.drawCenteredTextWithShadow(textRenderer, Text.literal("✓"), boxX + 27, boxY + 20, 0xFF88DD99);
            }
        }

        boolean validGhost = ghostPlacementValid();
        boolean hoveringPlacement = hoveredGridX >= 0 && hoveredGridY >= 0
                && selectedShape >= 0 && selectedShape < placedShapes.length;
        String placementKey = validGhost
                ? "screen.mythicrpg.fishing_minigame.placement_valid"
                : hoveringPlacement
                        ? "screen.mythicrpg.fishing_minigame.placement_invalid"
                        : "screen.mythicrpg.fishing_minigame.placement_hint";
        int placementColor = validGhost ? 0xFF55C66A : hoveringPlacement ? 0xFFCC6666 : 0xFFB9B9B9;
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable(placementKey),
                panelX + 104,
                panelY + 151,
                placementColor,
                false
        );
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable(
                        "screen.mythicrpg.fishing_minigame.cells",
                        Integer.bitCount(boardMask),
                        payload.a()
                ),
                panelX + 245,
                panelY + 148,
                VanillaContainerUi.TEXT,
                false
        );
    }

    private void renderGhostPlacement(DrawContext context) {
        if (selectedShape < 0 || selectedShape >= placedShapes.length || placedShapes[selectedShape]
                || hoveredGridX < 0 || hoveredGridY < 0) {
            return;
        }
        int size = payload.b();
        int cell = gridCellSize();
        int color = ghostPlacementValid() ? 0xAA55CC77 : 0xAACC5555;
        for (int[] point : rotated(shapeTypes()[selectedShape], rotation)) {
            int x = hoveredGridX + point[0];
            int y = hoveredGridY + point[1];
            if (x < 0 || x >= size || y < 0 || y >= size) continue;
            int px = gridStartX() + x * cell;
            int py = gridStartY() + y * cell;
            context.fill(px + 2, py + 2, px + cell - 2, py + cell - 2, color);
        }
    }

    private void drawShapePreview(
            DrawContext context,
            ShapeType shape,
            int x,
            int y,
            int cell,
            int previewRotation,
            int color
    ) {
        for (int[] point : rotated(shape, previewRotation)) {
            int px = x + point[0] * cell;
            int py = y + point[1] * cell;
            context.fill(px, py, px + cell - 1, py + cell - 1, color);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (payload.gameType() == 2) {
            if (selectShape(mouseX, mouseY, button)) return true;
            if (clickGrid(mouseX, mouseY, button)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (payload.gameType() == 2) {
            if (keyCode == GLFW.GLFW_KEY_R) {
                rotateSelected();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                resetGrid();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean selectShape(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        ShapeType[] shapes = shapeTypes();
        for (int index = 0; index < shapes.length; index++) {
            int boxX = panelX + 205 + (index % 2) * 40;
            int boxY = panelY + 57 + (index / 2) * 43;
            if (mouseX >= boxX && mouseX < boxX + 34
                    && mouseY >= boxY && mouseY < boxY + 32
                    && !placedShapes[index]) {
                selectedShape = index;
                rotation = 0;
                return true;
            }
        }
        return false;
    }

    private boolean clickGrid(double mouseX, double mouseY, int button) {
        updateHoveredGrid((int) mouseX, (int) mouseY);
        if (hoveredGridX < 0 || hoveredGridY < 0) return false;
        if (button == 1) {
            rotateSelected();
            return true;
        }
        if (button != 0 || !ghostPlacementValid()) return true;

        int placement = placementMask(
                shapeTypes()[selectedShape],
                rotation,
                hoveredGridX,
                hoveredGridY,
                payload.b()
        );
        boardMask |= placement;
        placedShapes[selectedShape] = true;
        selectNextShape();
        updateValidateButton();
        return true;
    }

    private void updateHoveredGrid(int mouseX, int mouseY) {
        int size = payload.b();
        int cell = gridCellSize();
        int relativeX = mouseX - gridStartX();
        int relativeY = mouseY - gridStartY();
        if (relativeX < 0 || relativeY < 0
                || relativeX >= size * cell || relativeY >= size * cell) {
            hoveredGridX = -1;
            hoveredGridY = -1;
            return;
        }
        hoveredGridX = relativeX / cell;
        hoveredGridY = relativeY / cell;
    }

    private boolean ghostPlacementValid() {
        if (selectedShape < 0 || selectedShape >= placedShapes.length || placedShapes[selectedShape]
                || hoveredGridX < 0 || hoveredGridY < 0) {
            return false;
        }
        int placement = placementMask(
                shapeTypes()[selectedShape],
                rotation,
                hoveredGridX,
                hoveredGridY,
                payload.b()
        );
        return placement != 0 && (boardMask & placement) == 0;
    }

    private void rotateSelected() {
        if (selectedShape >= 0 && selectedShape < placedShapes.length && !placedShapes[selectedShape]) {
            rotation = Math.floorMod(rotation + 1, 4);
        }
    }

    private void selectNextShape() {
        for (int offset = 1; offset <= placedShapes.length; offset++) {
            int index = Math.floorMod(selectedShape + offset, placedShapes.length);
            if (!placedShapes[index]) {
                selectedShape = index;
                rotation = 0;
                return;
            }
        }
        selectedShape = -1;
        rotation = 0;
    }

    private void resetGrid() {
        boardMask = 0;
        selectedShape = 0;
        rotation = 0;
        hoveredGridX = -1;
        hoveredGridY = -1;
        placedShapes = new boolean[shapeTypes().length];
        updateValidateButton();
    }

    private void updateValidateButton() {
        if (validateButton == null) return;
        boolean allPlaced = true;
        for (boolean placed : placedShapes) {
            if (!placed) {
                allPlaced = false;
                break;
            }
        }
        validateButton.active = allPlaced && Integer.bitCount(boardMask) == payload.a();
    }

    private int gridCellSize() {
        return payload.b() == 5 ? 18 : 22;
    }

    private int gridStartX() {
        int width = payload.b() * gridCellSize();
        return panelX + 104 - width / 2;
    }

    private int gridStartY() {
        int height = payload.b() * gridCellSize();
        return panelY + 104 - height / 2;
    }

    private ShapeType[] shapeTypes() {
        return switch (FishingRarity.byRank(payload.rarityRank())) {
            case EPIC -> new ShapeType[]{ShapeType.DOMINO, ShapeType.DOMINO, ShapeType.DOMINO};
            case LEGENDARY -> new ShapeType[]{ShapeType.DOMINO, ShapeType.EL, ShapeType.LINE};
            case MYTHIC -> new ShapeType[]{
                    ShapeType.DOMINO,
                    ShapeType.DOMINO,
                    ShapeType.EL,
                    ShapeType.LINE
            };
            default -> new ShapeType[]{ShapeType.DOMINO, ShapeType.DOMINO, ShapeType.DOMINO};
        };
    }

    private static int placementMask(
            ShapeType shape,
            int rotation,
            int anchorX,
            int anchorY,
            int boardSize
    ) {
        int mask = 0;
        for (int[] point : rotated(shape, rotation)) {
            int x = anchorX + point[0];
            int y = anchorY + point[1];
            if (x < 0 || x >= boardSize || y < 0 || y >= boardSize) return 0;
            mask |= 1 << (y * boardSize + x);
        }
        return mask;
    }

    private static int[][] rotated(ShapeType shape, int rotation) {
        int[][] source = switch (shape) {
            case DOMINO -> new int[][]{{0, 0}, {1, 0}};
            case EL -> new int[][]{{0, 0}, {0, 1}, {1, 1}};
            case LINE -> new int[][]{{0, 0}, {1, 0}, {2, 0}};
        };

        int[][] result = new int[source.length][2];
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int turns = Math.floorMod(rotation, 4);
        for (int index = 0; index < source.length; index++) {
            int x = source[index][0];
            int y = source[index][1];
            for (int turn = 0; turn < turns; turn++) {
                int oldX = x;
                x = -y;
                y = oldX;
            }
            result[index][0] = x;
            result[index][1] = y;
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
        }
        for (int[] point : result) {
            point[0] -= minX;
            point[1] -= minY;
        }
        return result;
    }

    private void sendAndClose(int action, int value) {
        ClientPlayNetworking.send(new FishingMiniGameActionPayload(action, value));
        close();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    private enum ShapeType {
        DOMINO,
        EL,
        LINE
    }
}
