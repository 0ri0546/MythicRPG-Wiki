package com.mythicrpg.client.mining;

import com.mythicrpg.client.ui.MythicInventoryTabs;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.mining.archaeology.FossilCodexEntry;
import com.mythicrpg.mining.archaeology.FossilContentRegistry;
import com.mythicrpg.mining.archaeology.FossilFamily;
import com.mythicrpg.mining.archaeology.FossilRarity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class FossilCodexScreen extends Screen {

    private static final int WIDTH = 256;
    private static final int HEIGHT = 180;
    private static final int PANEL = 0xFFE2D2A8;
    private static final int PAGE = 0xFFF4E8C8;
    private static final int BORDER = 0xFF5A4428;
    private static final int LOCKED = 0xFF8F8067;
    private static final int TEXT = 0xFF3C2D1D;

    private int pageIndex;
    private int x;
    private int y;

    public FossilCodexScreen() {
        super(Text.translatable("screen.mythicrpg.fossil_codex"));
    }

    @Override
    protected void init() {
        x = (width - WIDTH) / 2;
        y = (height - HEIGHT) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        MythicInventoryTabs.renderInventoryTab(context, x, y, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderMythicCraftingTab(context, x, y, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderTravelingCompassTab(context, x, y, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderFossilCodexTab(context, x, y, WIDTH, mouseX, mouseY, true);
        MythicInventoryTabs.renderEatingCodexTab(context, x, y, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderFishingCodexTab(context, x, y, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderTitlesTab(context, x, y, WIDTH, mouseX, mouseY, false);
        context.fill(x, y, x + WIDTH, y + HEIGHT, BORDER);
        context.fill(x + 2, y + 2, x + WIDTH - 2, y + HEIGHT - 2, PANEL);
        context.fill(x + 10, y + 24, x + WIDTH - 10, y + HEIGHT - 12, PAGE);

        FossilFamily family = FossilFamily.values()[pageIndex];
        context.drawCenteredTextWithShadow(textRenderer, title, x + WIDTH / 2, y + 7, TEXT);
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                family.displayName(),
                x + WIDTH / 2,
                y + 28,
                TEXT,
                true
        );

        for (int index = 0; index < FossilRarity.values().length; index++) {
            drawCard(context, family, FossilRarity.values()[index], index, mouseX, mouseY);
        }

        context.drawTextWithShadow(textRenderer, Text.literal("<"), x + 17, y + 8, TEXT);
        context.drawTextWithShadow(textRenderer, Text.literal(">"), x + WIDTH - 23, y + 8, TEXT);
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.translatable("screen.mythicrpg.fossil_codex.page", pageIndex + 1, FossilFamily.values().length),
                x + WIDTH / 2,
                y + HEIGHT - 10,
                TEXT,
                true
        );
        MythicInventoryTabs.renderTooltip(context, textRenderer, x, y, WIDTH, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (mouseY >= y + 4 && mouseY < y + 22 && mouseX >= x + 8 && mouseX < x + 34) {
                pageIndex = Math.floorMod(pageIndex - 1, FossilFamily.values().length);
                return true;
            }
            if (mouseY >= y + 4 && mouseY < y + 22 && mouseX >= x + WIDTH - 34 && mouseX < x + WIDTH - 8) {
                pageIndex = (pageIndex + 1) % FossilFamily.values().length;
                return true;
            }
            if (MythicInventoryTabs.isOverInventoryTab(x, y, WIDTH, mouseX, mouseY)) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    MythicInventoryTabs.setScreenPreservingMouse(new net.minecraft.client.gui.screen.ingame.InventoryScreen(client.player));
                }
                return true;
            }
            if (MythicInventoryTabs.isOverMythicCraftingTab(x, y, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenMythicCrafting();
                return true;
            }
            if (MythicInventoryTabs.isOverTravelingCompassTab(x, y, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenTravelingCompass();
                return true;
            }
            if (MythicInventoryTabs.isOverEatingCodexTab(x, y, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenEatingCodex();
                return true;
            }
            if (MythicInventoryTabs.isOverFishingCodexTab(x, y, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenFishingCodex();
                return true;
            }
            if (MythicInventoryTabs.isOverTitlesTab(x, y, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenTitles();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void drawCard(
            DrawContext context,
            FossilFamily family,
            FossilRarity rarity,
            int index,
            int mouseX,
            int mouseY
    ) {
        int cardWidth = 42;
        int cardHeight = 92;
        int gap = 4;
        int startX = x + 14;
        int cardX = startX + index * (cardWidth + gap);
        int cardY = y + 47;
        FossilCodexEntry entry = ClientFossilCodexState.get(family, rarity);
        boolean unlocked = entry.reconstructedCount() > 0;
        int rarityColor = rarity.formatting().getColorValue() == null
                ? 0xFFFFFFFF
                : 0xFF000000 | rarity.formatting().getColorValue();

        context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, BORDER);
        context.fill(cardX + 2, cardY + 2, cardX + cardWidth - 2, cardY + cardHeight - 2, unlocked ? PAGE : LOCKED);
        context.fill(cardX + 4, cardY + 5, cardX + cardWidth - 4, cardY + 8, rarityColor);

        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                unlocked ? rarity.displayName() : Text.literal("?"),
                cardX + cardWidth / 2,
                cardY + 14,
                unlocked ? TEXT : 0xFFFFFFFF,
                true
        );
        if (unlocked) {
            FossilContentRegistry.skeletonItem(family, rarity).ifPresent(item ->
                    context.drawItem(new ItemStack(item), cardX + (cardWidth - 16) / 2, cardY + 32)
            );
        } else {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("?"),
                    cardX + cardWidth / 2,
                    cardY + 39,
                    0xFFFFFFFF
            );
        }
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                unlocked
                        ? Text.translatable("screen.mythicrpg.fossil_codex.count", entry.reconstructedCount())
                        : Text.translatable("screen.mythicrpg.fossil_codex.unknown"),
                cardX + cardWidth / 2,
                cardY + 63,
                unlocked ? TEXT : 0xFFFFFFFF,
                true
        );
        if (unlocked) {
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.translatable("screen.mythicrpg.fossil_codex.first_day", entry.firstReconstructedDay()),
                    cardX + cardWidth / 2,
                    cardY + 76,
                    TEXT,
                    true
            );
        }

        if (mouseX >= cardX && mouseX < cardX + cardWidth && mouseY >= cardY && mouseY < cardY + cardHeight) {
            if (unlocked) {
                context.drawTooltip(textRenderer, java.util.List.of(
                        family.displayName(),
                        rarity.displayName(),
                        Text.translatable("screen.mythicrpg.fossil_codex.reconstructed", entry.reconstructedCount()),
                        Text.translatable("screen.mythicrpg.fossil_codex.analyzed", entry.analyzedCount()),
                        Text.translatable("screen.mythicrpg.fossil_codex.first_day_full", entry.firstReconstructedDay())
                ), mouseX, mouseY);
            } else {
                context.drawTooltip(textRenderer, Text.translatable("screen.mythicrpg.fossil_codex.locked"), mouseX, mouseY);
            }
        }
    }
}
