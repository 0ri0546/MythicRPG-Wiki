package com.mythicrpg.client.eating;

import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.client.ui.VanillaCustomScreen;
import com.mythicrpg.eating.CulinaryIngredientRegistry;
import com.mythicrpg.eating.SignatureBonus;
import com.mythicrpg.eating.SignatureDishCreatePayload;
import com.mythicrpg.eating.SignatureDishOpenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/** Vanilla-style editor for the player's single signature-dish profile. */
public final class SignatureDishScreen extends VanillaCustomScreen {
    private static final int PANEL_WIDTH = 304;
    private static final int PANEL_HEIGHT = 239;
    private static final int GRID_X_OFFSET = 71;
    private static final int GRID_Y_OFFSET = 82;
    private static final int SELECTED_Y_OFFSET = 166;

    private final int handId;
    private final String initialName;
    private final List<Identifier> selectedIngredients = new ArrayList<>();
    private final List<ButtonWidget> bonusButtons = new ArrayList<>();
    private Identifier icon;
    private SignatureBonus bonus;
    private TextFieldWidget nameField;
    private ButtonWidget confirmButton;
    private ItemStack hoveredStack = ItemStack.EMPTY;

    public SignatureDishScreen(SignatureDishOpenPayload payload) {
        super(Text.translatable("screen.mythicrpg.signature_dish"), PANEL_WIDTH, PANEL_HEIGHT);
        handId = payload.handId();
        initialName = payload.ingredientIds().isEmpty()
                ? Text.translatable("dish.mythicrpg.signature_dish").getString()
                : payload.name();
        bonus = SignatureBonus.byOrdinal(payload.bonusId());
        for (String rawId : payload.ingredientIds()) {
            Identifier id = Identifier.tryParse(rawId);
            if (id != null
                    && Registries.ITEM.containsId(id)
                    && !selectedIngredients.contains(id)
                    && selectedIngredients.size() < 5) {
                selectedIngredients.add(id);
            }
        }
        Identifier requestedIcon = Identifier.tryParse(payload.iconId());
        icon = requestedIcon != null && selectedIngredients.contains(requestedIcon)
                ? requestedIcon
                : (selectedIngredients.isEmpty() ? null : selectedIngredients.getFirst());
    }

    @Override
    protected void initVanillaScreen() {
        nameField = new TextFieldWidget(
                textRenderer,
                panelX + 24,
                panelY + 30,
                256,
                18,
                Text.translatable("screen.mythicrpg.signature_dish.name")
        );
        nameField.setMaxLength(32);
        nameField.setText(initialName);
        nameField.setChangedListener(value -> updateConfirmButton());
        addDrawableChild(nameField);

        bonusButtons.clear();
        int bonusX = panelX + 24;
        for (SignatureBonus value : SignatureBonus.values()) {
            ButtonWidget button = addDrawableChild(ButtonWidget.builder(
                            bonusButtonText(value),
                            ignored -> {
                                bonus = value;
                                refreshBonusButtons();
                            }
                    )
                    .dimensions(bonusX, panelY + 56, 82, 20)
                    .build());
            bonusButtons.add(button);
            bonusX += 87;
        }

        confirmButton = addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.signature_dish.save"),
                        ignored -> saveConfiguration()
                )
                .dimensions(panelX + 58, panelY + 211, 88, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), ignored -> close())
                .dimensions(panelX + 158, panelY + 211, 88, 20)
                .build());

        refreshBonusButtons();
        updateConfirmButton();
        setInitialFocus(nameField);
    }

    @Override
    protected void renderVanillaContent(DrawContext context, int mouseX, int mouseY, float delta) {
        hoveredStack = ItemStack.EMPTY;
        context.drawText(
                textRenderer,
                Text.translatable("screen.mythicrpg.signature_dish.name"),
                panelX + 24,
                panelY + 20,
                VanillaContainerUi.TEXT,
                false
        );

        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.signature_dish.inventory_help_short"),
                panelX + PANEL_WIDTH / 2,
                panelY + 75,
                VanillaContainerUi.DISABLED_TEXT,
                false
        );

        drawSection(context, panelX + 16, panelY + 79, PANEL_WIDTH - 32, 80, null);
        drawSection(context, panelX + 16, panelY + 162, PANEL_WIDTH - 32, 27, null);
        drawInventory(context, mouseX, mouseY);
        drawSelectedIngredients(context, mouseX, mouseY);

        VanillaContainerUi.drawSmallText(
                context,
                textRenderer,
                Text.literal(selectedIngredients.size() + "/5"),
                panelX + 24,
                panelY + 193,
                selectedIngredients.size() >= 2 ? 0xFF3F7F3F : 0xFF9F3F3F,
                false
        );
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.signature_dish.duration_short"),
                panelX + PANEL_WIDTH / 2,
                panelY + 193,
                VanillaContainerUi.DISABLED_TEXT,
                false
        );
    }

    @Override
    protected void renderAfterWidgets(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!hoveredStack.isEmpty()) {
            context.drawItemTooltip(textRenderer, hoveredStack, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        int clickedSlot = inventorySlotAt(mouseX, mouseY);
        if (clickedSlot < 0 || client == null || client.player == null) {
            return false;
        }
        ItemStack stack = client.player.getInventory().getStack(clickedSlot);
        if (stack.isEmpty() || !CulinaryIngredientRegistry.isCulinaryIngredient(stack)) {
            return true;
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        if (button == 1) {
            if (selectedIngredients.contains(itemId)) {
                icon = itemId;
            }
            updateConfirmButton();
            return true;
        }
        if (button != 0) {
            return false;
        }

        if (selectedIngredients.contains(itemId)) {
            selectedIngredients.remove(itemId);
            if (itemId.equals(icon)) {
                icon = selectedIngredients.isEmpty() ? null : selectedIngredients.getFirst();
            }
        } else if (selectedIngredients.size() < 5) {
            selectedIngredients.add(itemId);
            if (icon == null) {
                icon = itemId;
            }
        }
        updateConfirmButton();
        return true;
    }

    private void saveConfiguration() {
        if (!isConfigurationValid()) {
            return;
        }
        ClientPlayNetworking.send(new SignatureDishCreatePayload(
                handId,
                nameField.getText(),
                bonus.ordinal(),
                icon.toString(),
                selectedIngredients.stream().map(Identifier::toString).toList()
        ));
        close();
    }

    private boolean isConfigurationValid() {
        return selectedIngredients.size() >= 2
                && selectedIngredients.size() <= 5
                && icon != null
                && selectedIngredients.contains(icon)
                && nameField != null
                && !nameField.getText().isBlank();
    }

    private void updateConfirmButton() {
        if (confirmButton != null) {
            confirmButton.active = isConfigurationValid();
        }
    }

    private Text bonusButtonText(SignatureBonus value) {
        Text label = Text.translatable("screen.mythicrpg.signature_dish.bonus_short." + value.id());
        return value == bonus
                ? Text.literal("[ ").append(label).append(Text.literal(" ]")).formatted(Formatting.GOLD)
                : label;
    }

    private void refreshBonusButtons() {
        for (int index = 0; index < bonusButtons.size(); index++) {
            bonusButtons.get(index).setMessage(bonusButtonText(SignatureBonus.values()[index]));
        }
    }

    private int inventorySlotAt(double mouseX, double mouseY) {
        int startX = panelX + GRID_X_OFFSET;
        int startY = panelY + GRID_Y_OFFSET;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int slotX = startX + column * 18;
                int slotY = startY + row * 18;
                if (VanillaContainerUi.isPointInside(mouseX, mouseY, slotX, slotY, 18, 18)) {
                    return inventorySlot(row, column);
                }
            }
        }
        return -1;
    }

    private static int inventorySlot(int row, int column) {
        return row < 3 ? 9 + row * 9 + column : column;
    }

    private void drawInventory(DrawContext context, int mouseX, int mouseY) {
        if (client == null || client.player == null) {
            return;
        }
        int startX = panelX + GRID_X_OFFSET;
        int startY = panelY + GRID_Y_OFFSET;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 9; column++) {
                int slot = inventorySlot(row, column);
                int slotX = startX + column * 18;
                int slotY = startY + row * 18;
                VanillaContainerUi.drawSlot(context, slotX, slotY);
                ItemStack stack = client.player.getInventory().getStack(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                context.drawItem(stack, slotX + 1, slotY + 1);
                context.drawItemInSlot(textRenderer, stack, slotX + 1, slotY + 1);
                Identifier itemId = Registries.ITEM.getId(stack.getItem());
                if (selectedIngredients.contains(itemId)) {
                    drawSelectionBorder(context, slotX, slotY, itemId.equals(icon));
                }
                if (VanillaContainerUi.isPointInside(mouseX, mouseY, slotX, slotY, 18, 18)) {
                    hoveredStack = stack;
                }
            }
        }
    }

    private void drawSelectedIngredients(DrawContext context, int mouseX, int mouseY) {
        int startX = panelX + (PANEL_WIDTH - 5 * 18) / 2;
        int slotY = panelY + SELECTED_Y_OFFSET;
        for (int index = 0; index < 5; index++) {
            int slotX = startX + index * 18;
            VanillaContainerUi.drawSlot(context, slotX, slotY);
            if (index >= selectedIngredients.size()) {
                continue;
            }
            Identifier ingredient = selectedIngredients.get(index);
            ItemStack stack = new ItemStack(Registries.ITEM.get(ingredient));
            context.drawItem(stack, slotX + 1, slotY + 1);
            context.drawItemInSlot(textRenderer, stack, slotX + 1, slotY + 1);
            drawSelectionBorder(context, slotX, slotY, ingredient.equals(icon));
            if (VanillaContainerUi.isPointInside(mouseX, mouseY, slotX, slotY, 18, 18)) {
                hoveredStack = stack;
            }
        }
    }

    private static void drawSelectionBorder(DrawContext context, int x, int y, boolean selectedIcon) {
        int color = selectedIcon ? 0xFFFFC928 : 0xFF45C95A;
        context.fill(x, y, x + 18, y + 1, color);
        context.fill(x, y + 17, x + 18, y + 18, color);
        context.fill(x, y, x + 1, y + 18, color);
        context.fill(x + 17, y, x + 18, y + 18, color);
        if (selectedIcon) {
            context.fill(x + 13, y + 2, x + 16, y + 5, 0xFFFFFFFF);
        }
    }
}
