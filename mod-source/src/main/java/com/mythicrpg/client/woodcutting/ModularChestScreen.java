package com.mythicrpg.client.woodcutting;

import com.mythicrpg.client.ui.VanillaContainerScreen;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.mixin.client.SlotPositionAccessor;
import com.mythicrpg.woodcutting.chest.ModularChestScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

/** Vanilla-style modular chest with a continuous vertically scrollable storage grid. */
public final class ModularChestScreen extends VanillaContainerScreen<ModularChestScreenHandler> {

    private static final int VISIBLE_ROWS = 6;
    private static final int COLUMNS = 9;

    private static final int STORAGE_FRAME_X = 8;
    private static final int STORAGE_FRAME_Y = 18;
    private static final int STORAGE_ITEM_X = 9;
    private static final int STORAGE_ITEM_Y = 19;

    private static final int SCROLLBAR_X = 173;
    private static final int SCROLLBAR_Y = 18;
    private static final int SCROLLBAR_WIDTH = 9;
    private static final int SCROLLBAR_HEIGHT = 108;
    private static final int MIN_THUMB_HEIGHT = 16;

    private static final int MODULE_FRAME_X = 188;
    private static final int MODULE_FIRST_DOUBLE_Y = 37;
    private static final int MODULE_SECOND_DOUBLE_Y = 73;
    private static final int MODULE_SINGLE_Y = 55;

    private static final int PLAYER_FRAME_X = 8;
    private static final int PLAYER_FRAME_Y = 140;
    private static final int HOTBAR_FRAME_Y = 198;

    private int rowOffset;
    private int lastCapacity = -1;
    private int lastChestCount = -1;
    private boolean draggingScrollbar;
    private double scrollbarGrabOffset;

    public ModularChestScreen(
            ModularChestScreenHandler handler,
            PlayerInventory inventory,
            Text title
    ) {
        super(handler, inventory, title, 214, 222);
        titleX = 8;
        titleY = 6;
        playerInventoryTitleY = 130;
    }

    @Override
    protected void init() {
        super.init();
        refreshSlotPositions(true);
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        refreshSlotPositions(false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        refreshSlotPositions(false);
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
        drawEmptyModuleTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(
                textRenderer,
                title,
                titleX,
                titleY,
                VanillaContainerUi.TEXT,
                false
        );
        context.drawText(
                textRenderer,
                playerInventoryTitle,
                8,
                playerInventoryTitleY,
                VanillaContainerUi.TEXT,
                false
        );
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        drawVanillaContainer(context);

        int visibleRows = Math.min(VISIBLE_ROWS, totalRows());
        VanillaContainerUi.drawSlotGrid(
                context,
                x + STORAGE_FRAME_X,
                y + STORAGE_FRAME_Y,
                COLUMNS,
                visibleRows
        );

        if (handler.getChestCount() == 1) {
            VanillaContainerUi.drawSlot(
                    context,
                    x + MODULE_FRAME_X,
                    y + MODULE_SINGLE_Y
            );
        } else {
            VanillaContainerUi.drawSlot(
                    context,
                    x + MODULE_FRAME_X,
                    y + MODULE_FIRST_DOUBLE_Y
            );
            VanillaContainerUi.drawSlot(
                    context,
                    x + MODULE_FRAME_X,
                    y + MODULE_SECOND_DOUBLE_Y
            );
        }

        VanillaContainerUi.drawSlotGrid(
                context,
                x + PLAYER_FRAME_X,
                y + PLAYER_FRAME_Y,
                9,
                3
        );
        VanillaContainerUi.drawSlotGrid(
                context,
                x + PLAYER_FRAME_X,
                y + HOTBAR_FRAME_Y,
                9,
                1
        );

        if (maxRowOffset() > 0) {
            drawScrollbar(context);
        }
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (maxRowOffset() > 0
                && verticalAmount != 0.0D
                && isOverStorageOrScrollbar(mouseX, mouseY)) {
            setRowOffset(rowOffset + (verticalAmount > 0.0D ? -1 : 1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && maxRowOffset() > 0) {
            int trackX = x + SCROLLBAR_X;
            int trackY = y + SCROLLBAR_Y;
            int thumbY = scrollbarThumbY();
            int thumbHeight = scrollbarThumbHeight();

            if (VanillaContainerUi.isPointInside(
                    mouseX,
                    mouseY,
                    trackX,
                    trackY,
                    SCROLLBAR_WIDTH,
                    SCROLLBAR_HEIGHT
            )) {
                draggingScrollbar = true;
                if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                    scrollbarGrabOffset = mouseY - thumbY;
                } else {
                    scrollbarGrabOffset = thumbHeight / 2.0D;
                    updateScrollFromMouse(mouseY);
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(
            double mouseX,
            double mouseY,
            int button,
            double deltaX,
            double deltaY
    ) {
        if (draggingScrollbar && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            updateScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverStorageOrScrollbar(double mouseX, double mouseY) {
        return VanillaContainerUi.isPointInside(
                mouseX,
                mouseY,
                x + STORAGE_FRAME_X,
                y + STORAGE_FRAME_Y,
                COLUMNS * 18,
                VISIBLE_ROWS * 18
        ) || VanillaContainerUi.isPointInside(
                mouseX,
                mouseY,
                x + SCROLLBAR_X,
                y + SCROLLBAR_Y,
                SCROLLBAR_WIDTH,
                SCROLLBAR_HEIGHT
        );
    }

    private void drawScrollbar(DrawContext context) {
        int trackX = x + SCROLLBAR_X;
        int trackY = y + SCROLLBAR_Y;
        int thumbY = scrollbarThumbY();
        int thumbHeight = scrollbarThumbHeight();

        context.fill(
                trackX,
                trackY,
                trackX + SCROLLBAR_WIDTH,
                trackY + SCROLLBAR_HEIGHT,
                VanillaContainerUi.DARK_SHADOW
        );
        context.fill(
                trackX + 1,
                trackY + 1,
                trackX + SCROLLBAR_WIDTH - 1,
                trackY + SCROLLBAR_HEIGHT - 1,
                VanillaContainerUi.SLOT_INTERIOR
        );
        context.fill(
                trackX + 1,
                thumbY,
                trackX + SCROLLBAR_WIDTH - 1,
                thumbY + thumbHeight,
                VanillaContainerUi.BACKGROUND
        );
        context.fill(
                trackX + 1,
                thumbY,
                trackX + SCROLLBAR_WIDTH - 2,
                thumbY + 1,
                VanillaContainerUi.HIGHLIGHT
        );
        context.fill(
                trackX + SCROLLBAR_WIDTH - 2,
                thumbY + 1,
                trackX + SCROLLBAR_WIDTH - 1,
                thumbY + thumbHeight,
                VanillaContainerUi.OUTLINE
        );
    }

    private void drawEmptyModuleTooltip(DrawContext context, int mouseX, int mouseY) {
        int firstY = handler.getChestCount() == 1
                ? MODULE_SINGLE_Y
                : MODULE_FIRST_DOUBLE_Y;
        if (handler.getSlot(handler.getModuleStart()).getStack().isEmpty()
                && VanillaContainerUi.isPointInside(
                mouseX,
                mouseY,
                x + MODULE_FRAME_X,
                y + firstY,
                18,
                18
        )) {
            context.drawTooltip(
                    textRenderer,
                    Text.translatable("tooltip.mythicrpg.chest_module.slot")
                            .formatted(Formatting.AQUA),
                    mouseX,
                    mouseY
            );
            return;
        }

        if (handler.getChestCount() > 1
                && handler.getSlot(handler.getModuleStart() + 1).getStack().isEmpty()
                && VanillaContainerUi.isPointInside(
                mouseX,
                mouseY,
                x + MODULE_FRAME_X,
                y + MODULE_SECOND_DOUBLE_Y,
                18,
                18
        )) {
            context.drawTooltip(
                    textRenderer,
                    Text.translatable("tooltip.mythicrpg.chest_module.slot")
                            .formatted(Formatting.AQUA),
                    mouseX,
                    mouseY
            );
        }
    }

    private void refreshSlotPositions(boolean force) {
        int capacity = handler.getCapacity();
        int chestCount = handler.getChestCount();
        int clampedOffset = Math.max(0, Math.min(rowOffset, maxRowOffset()));
        boolean changed = force
                || capacity != lastCapacity
                || chestCount != lastChestCount
                || clampedOffset != rowOffset;

        rowOffset = clampedOffset;
        if (!changed) {
            return;
        }

        lastCapacity = capacity;
        lastChestCount = chestCount;

        for (int storageSlot = 0;
             storageSlot < handler.getStorageSlotCount();
             storageSlot++) {
            Slot slot = handler.getSlot(ModularChestScreenHandler.STORAGE_START + storageSlot);
            int logicalRow = storageSlot / COLUMNS;
            int column = storageSlot % COLUMNS;
            boolean visible = storageSlot < capacity
                    && logicalRow >= rowOffset
                    && logicalRow < rowOffset + VISIBLE_ROWS;

            setSlotPosition(
                    slot,
                    visible ? STORAGE_ITEM_X + column * 18 : -10_000,
                    visible ? STORAGE_ITEM_Y + (logicalRow - rowOffset) * 18 : -10_000
            );
        }

        Slot firstModule = handler.getSlot(handler.getModuleStart());
        setSlotPosition(
                firstModule,
                189,
                chestCount == 1 ? MODULE_SINGLE_Y + 1 : MODULE_FIRST_DOUBLE_Y + 1
        );

        if (chestCount > 1) {
            Slot secondModule = handler.getSlot(handler.getModuleStart() + 1);
            setSlotPosition(
                    secondModule,
                    189,
                    MODULE_SECOND_DOUBLE_Y + 1
            );
        }
    }

    private void setRowOffset(int requested) {
        int clamped = Math.max(0, Math.min(maxRowOffset(), requested));
        if (clamped != rowOffset) {
            rowOffset = clamped;
            refreshSlotPositions(true);
        }
    }

    private void updateScrollFromMouse(double mouseY) {
        int thumbHeight = scrollbarThumbHeight();
        int travel = SCROLLBAR_HEIGHT - thumbHeight;
        if (travel <= 0) {
            setRowOffset(0);
            return;
        }

        double top = mouseY - (y + SCROLLBAR_Y) - scrollbarGrabOffset;
        double ratio = Math.max(0.0D, Math.min(1.0D, top / travel));
        setRowOffset((int) Math.round(ratio * maxRowOffset()));
    }

    private int totalRows() {
        return Math.max(1, (handler.getCapacity() + COLUMNS - 1) / COLUMNS);
    }

    private int maxRowOffset() {
        return Math.max(0, totalRows() - VISIBLE_ROWS);
    }

    private int scrollbarThumbHeight() {
        return Math.max(
                MIN_THUMB_HEIGHT,
                SCROLLBAR_HEIGHT * VISIBLE_ROWS / totalRows()
        );
    }

    private int scrollbarThumbY() {
        int thumbHeight = scrollbarThumbHeight();
        int travel = SCROLLBAR_HEIGHT - thumbHeight;
        if (maxRowOffset() <= 0) {
            return y + SCROLLBAR_Y;
        }
        return y + SCROLLBAR_Y + travel * rowOffset / maxRowOffset();
    }

    private static void setSlotPosition(Slot slot, int x, int y) {
        SlotPositionAccessor accessor = (SlotPositionAccessor) slot;
        accessor.mythicrpg$setX(x);
        accessor.mythicrpg$setY(y);
    }
}
