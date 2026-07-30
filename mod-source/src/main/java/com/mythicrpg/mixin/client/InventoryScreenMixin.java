package com.mythicrpg.mixin.client;

import com.mythicrpg.client.ui.MythicInventoryTabs;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.mining.archaeology.relic.FossilPaletteItem;
import com.mythicrpg.mining.archaeology.relic.FossilPaletteSlot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void mythicrpg$positionPaletteSlots(CallbackInfo ci) {
        InventoryScreen screen = (InventoryScreen) (Object) this;
        HandledScreenAccessor accessor = (HandledScreenAccessor) this;
        int screenWidth = MinecraftClient.getInstance().getWindow().getScaledWidth();

        // Local slot coordinate that leaves a small margin against the actual
        // right edge of the screen, independently of GUI scale or potion effects.
        int slotX = screenWidth - accessor.mythicrpg$getX() - 22;
        int slotY = 8;
        int paletteIndex = 0;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!(slot instanceof FossilPaletteSlot)) {
                continue;
            }
            SlotPositionAccessor position = (SlotPositionAccessor) slot;
            position.mythicrpg$setX(slotX);
            position.mythicrpg$setY(slotY + paletteIndex * 18);
            paletteIndex++;
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void mythicrpg$renderMythicTabs(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        HandledScreenAccessor accessor = (HandledScreenAccessor) this;

        MythicInventoryTabs.renderInventoryTab(
                context,
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY,
                true
        );
        MythicInventoryTabs.renderMythicCraftingTab(
                context,
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY,
                false
        );
        MythicInventoryTabs.renderTravelingCompassTab(
                context,
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY,
                false
        );
        MythicInventoryTabs.renderFossilCodexTab(
                context,
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY,
                false
        );
        MythicInventoryTabs.renderEatingCodexTab(
                context,
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY,
                false
        );
        MythicInventoryTabs.renderFishingCodexTab(
                context, accessor.mythicrpg$getX(), accessor.mythicrpg$getY(), accessor.mythicrpg$getBackgroundWidth(), mouseX, mouseY, false
        );
        MythicInventoryTabs.renderTitlesTab(
                context,
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY,
                false
        );
    }

    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void mythicrpg$drawPalettePanel(
            DrawContext context,
            float delta,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        renderPaletteSlots(context, (HandledScreenAccessor) this);
    }

    private void renderPaletteSlots(DrawContext context, HandledScreenAccessor accessor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        ItemStack palette = client.player.getOffHandStack();
        if (!palette.isOf(ModItems.FOSSIL_PALETTE)) {
            return;
        }

        int capacity = FossilPaletteItem.slots(palette);
        List<Slot> activeSlots = new ArrayList<>(capacity);
        InventoryScreen screen = (InventoryScreen) (Object) this;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (slot instanceof FossilPaletteSlot && activeSlots.size() < capacity) {
                activeSlots.add(slot);
            }
        }
        if (activeSlots.isEmpty()) {
            return;
        }

        int minX = activeSlots.stream().mapToInt(slot -> slot.x).min().orElse(0);
        int minY = activeSlots.stream().mapToInt(slot -> slot.y).min().orElse(0);
        int maxX = activeSlots.stream().mapToInt(slot -> slot.x + 17).max().orElse(minX + 17);
        int maxY = activeSlots.stream().mapToInt(slot -> slot.y + 17).max().orElse(minY + 17);
        int minFrameX = minX - 1;
        int minFrameY = minY - 1;
        int panelX = accessor.mythicrpg$getX() + minFrameX - 4;
        int panelY = accessor.mythicrpg$getY() + minFrameY - 4;
        int panelWidth = maxX - minFrameX + 8;
        int panelHeight = maxY - minFrameY + 8;

        VanillaContainerUi.drawPanel(context, panelX, panelY, panelWidth, panelHeight);
        for (Slot slot : activeSlots) {
            int slotX = accessor.mythicrpg$getX() + slot.x - 1;
            int slotY = accessor.mythicrpg$getY() + slot.y - 1;
            VanillaContainerUi.drawSlot(context, slotX, slotY);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void mythicrpg$renderMythicTooltips(
            DrawContext context,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        HandledScreenAccessor accessor = (HandledScreenAccessor) this;
        MythicInventoryTabs.renderTooltip(
                context,
                MinecraftClient.getInstance().textRenderer,
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY
        );

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || !client.player.getOffHandStack().isOf(ModItems.FOSSIL_PALETTE)) {
            return;
        }

        int capacity = FossilPaletteItem.slots(client.player.getOffHandStack());
        int seen = 0;
        InventoryScreen screen = (InventoryScreen) (Object) this;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!(slot instanceof FossilPaletteSlot) || seen++ >= capacity) {
                continue;
            }
            if (!slot.getStack().isEmpty()) {
                continue;
            }
            int slotX = accessor.mythicrpg$getX() + slot.x - 1;
            int slotY = accessor.mythicrpg$getY() + slot.y - 1;
            if (VanillaContainerUi.isPointInside(mouseX, mouseY, slotX, slotY, 18, 18)) {
                context.drawTooltip(
                        client.textRenderer,
                        Text.translatable("tooltip.mythicrpg.palette.block_slot"),
                        mouseX,
                        mouseY
                );
                return;
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mythicrpg$clickMythicTabs(
            double mouseX,
            double mouseY,
            int button,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (button != 0) {
            return;
        }

        HandledScreenAccessor accessor = (HandledScreenAccessor) this;
        if (MythicInventoryTabs.isOverMythicCraftingTab(
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY
        )) {
            MythicInventoryTabs.requestOpenMythicCrafting();
            cir.setReturnValue(true);
            return;
        }
        if (MythicInventoryTabs.isOverTravelingCompassTab(
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY
        )) {
            MythicInventoryTabs.requestOpenTravelingCompass();
            cir.setReturnValue(true);
            return;
        }
        if (MythicInventoryTabs.isOverFossilCodexTab(
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY
        )) {
            MythicInventoryTabs.requestOpenFossilCodex();
            cir.setReturnValue(true);
            return;
        }
        if (MythicInventoryTabs.isOverFishingCodexTab(
                accessor.mythicrpg$getX(), accessor.mythicrpg$getY(), accessor.mythicrpg$getBackgroundWidth(), mouseX, mouseY
        )) { MythicInventoryTabs.requestOpenFishingCodex(); cir.setReturnValue(true); return; }
        if (MythicInventoryTabs.isOverEatingCodexTab(
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY
        )) {
            MythicInventoryTabs.requestOpenEatingCodex();
            cir.setReturnValue(true);
            return;
        }
        if (MythicInventoryTabs.isOverTitlesTab(
                accessor.mythicrpg$getX(),
                accessor.mythicrpg$getY(),
                accessor.mythicrpg$getBackgroundWidth(),
                mouseX,
                mouseY
        )) {
            MythicInventoryTabs.requestOpenTitles();
            cir.setReturnValue(true);
        }
    }
}
