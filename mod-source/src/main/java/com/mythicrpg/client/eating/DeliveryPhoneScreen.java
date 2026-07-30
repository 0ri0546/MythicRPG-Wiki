package com.mythicrpg.client.eating;

import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.client.ui.VanillaCustomScreen;
import com.mythicrpg.eating.DeliveryPhoneActionPayload;
import com.mythicrpg.eating.DeliveryPhoneOpenPayload;
import com.mythicrpg.eating.DeliverySource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Compact vanilla-style selector for prepared-dish delivery. */
public final class DeliveryPhoneScreen extends VanillaCustomScreen {
    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEIGHT = 132;

    private final int handId;
    private DeliverySource source;
    private int count;
    private ButtonWidget potButton;
    private ButtonWidget fridgeButton;
    private ButtonWidget countButton;

    public DeliveryPhoneScreen(DeliveryPhoneOpenPayload payload) {
        super(Text.translatable("screen.mythicrpg.delivery_phone"), PANEL_WIDTH, PANEL_HEIGHT);
        handId = payload.handId();
        source = DeliverySource.byOrdinal(payload.sourceId());
        count = payload.count();
    }

    @Override
    protected void initVanillaScreen() {
        potButton = addDrawableChild(ButtonWidget.builder(
                        sourceText(DeliverySource.COOKING_POT),
                        button -> selectSource(DeliverySource.COOKING_POT)
                )
                .dimensions(panelX + 24, panelY + 35, 88, 20)
                .build());
        fridgeButton = addDrawableChild(ButtonWidget.builder(
                        sourceText(DeliverySource.FRIDGE),
                        button -> selectSource(DeliverySource.FRIDGE)
                )
                .dimensions(panelX + 118, panelY + 35, 88, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("−"), button -> changeCount(-1))
                .dimensions(panelX + 46, panelY + 65, 34, 20)
                .build());
        countButton = addDrawableChild(ButtonWidget.builder(countText(), button -> { })
                .dimensions(panelX + 86, panelY + 65, 58, 20)
                .build());
        countButton.active = false;
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), button -> changeCount(1))
                .dimensions(panelX + 150, panelY + 65, 34, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(
                        Text.translatable("screen.mythicrpg.delivery_phone.deliver"),
                        button -> deliver()
                )
                .dimensions(panelX + 24, panelY + 101, 88, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(panelX + 118, panelY + 101, 88, 20)
                .build());
        refreshSourceButtons();
    }

    @Override
    protected void renderVanillaContent(DrawContext context, int mouseX, int mouseY, float delta) {
        drawSection(context, panelX + 16, panelY + 25, PANEL_WIDTH - 32, 68, null);
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.delivery_phone.custom_only"),
                panelX + PANEL_WIDTH / 2,
                panelY + 19,
                0xFF3F7F3F,
                false
        );
    }

    private void selectSource(DeliverySource selected) {
        source = selected;
        refreshSourceButtons();
    }

    private void refreshSourceButtons() {
        if (potButton == null || fridgeButton == null) {
            return;
        }
        potButton.setMessage(sourceText(DeliverySource.COOKING_POT));
        fridgeButton.setMessage(sourceText(DeliverySource.FRIDGE));
    }

    private Text sourceText(DeliverySource value) {
        Text label = Text.translatable("delivery_source.mythicrpg." + value.id());
        return value == source
                ? Text.literal("[ ").append(label).append(Text.literal(" ]")).formatted(Formatting.GOLD)
                : label;
    }

    private void changeCount(int delta) {
        count = Math.max(1, Math.min(9, count + delta));
        countButton.setMessage(countText());
    }

    private Text countText() {
        return Text.literal("×" + count);
    }

    private void deliver() {
        ClientPlayNetworking.send(new DeliveryPhoneActionPayload(handId, source.ordinal(), count));
        close();
    }
}
