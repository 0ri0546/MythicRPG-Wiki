
package com.mythicrpg.client.fishing;

import com.mythicrpg.client.ui.MythicInventoryTabs;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.client.ui.VanillaCustomScreen;
import com.mythicrpg.fishing.FishingCodexEntry;
import com.mythicrpg.fishing.FishingFamily;
import com.mythicrpg.fishing.FishingRarity;
import com.mythicrpg.fishing.SeaMonsterProgressEntry;
import com.mythicrpg.fishing.SeaMonsterType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Fishing Codex using the same paginated card structure as the Eating Codex.
 * The 25 family/rarity cards are followed by a separate legendary-hunts page.
 */
public final class FishingCodexScreen extends VanillaCustomScreen {
    private static final int WIDTH = 286;
    private static final int HEIGHT = 190;
    private static final int PER_PAGE = 8;
    private static final int CARD_WIDTH = 60;
    private static final int CARD_HEIGHT = 61;
    private static final int CARD_GAP_X = 6;
    private static final int CARD_GAP_Y = 7;
    private static final int CARD_START_X = 14;
    private static final int CARD_START_Y = 30;
    private static final int LOCKED_FILL = 0xFF707070;

    private static final List<CodexCard> CARDS = createCards();

    private int page;
    private List<Text> pendingTooltip;

    public FishingCodexScreen() {
        super(Text.translatable("screen.mythicrpg.fishing_codex"), WIDTH, HEIGHT);
    }

    @Override
    protected void initVanillaScreen() {
        addDrawableChild(ButtonWidget.builder(Text.literal("<"), button -> changePage(-1))
                .dimensions(panelX + 8, panelY + 4, 20, 18)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), button -> changePage(1))
                .dimensions(panelX + WIDTH - 28, panelY + 4, 20, 18)
                .build());
    }

    @Override
    protected void renderBehindPanel(DrawContext context, int mouseX, int mouseY, float delta) {
        MythicInventoryTabs.renderInventoryTab(context, panelX, panelY, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderMythicCraftingTab(context, panelX, panelY, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderTravelingCompassTab(context, panelX, panelY, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderFossilCodexTab(context, panelX, panelY, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderEatingCodexTab(context, panelX, panelY, WIDTH, mouseX, mouseY, false);
        MythicInventoryTabs.renderFishingCodexTab(context, panelX, panelY, WIDTH, mouseX, mouseY, true);
        MythicInventoryTabs.renderTitlesTab(context, panelX, panelY, WIDTH, mouseX, mouseY, false);
    }

    @Override
    protected void renderVanillaContent(DrawContext context, int mouseX, int mouseY, float delta) {
        pendingTooltip = null;
        drawSection(context, panelX + 8, panelY + 24, WIDTH - 16, HEIGHT - 38, null);

        if (page == legendaryPageIndex()) {
            drawLegendaryPage(context, mouseX, mouseY);
        } else {
            int start = page * PER_PAGE;
            for (int index = 0; index < PER_PAGE; index++) {
                int cardIndex = start + index;
                if (cardIndex >= CARDS.size()) {
                    break;
                }
                drawCard(context, CARDS.get(cardIndex), index, mouseX, mouseY);
            }
        }

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.mythicrpg.fishing_codex.page", page + 1, pageCount()),
                panelX + WIDTH / 2,
                panelY + HEIGHT - 11,
                VanillaContainerUi.TEXT
        );
    }

    @Override
    protected void renderAfterWidgets(DrawContext context, int mouseX, int mouseY, float delta) {
        if (pendingTooltip != null) {
            context.drawTooltip(textRenderer, pendingTooltip, mouseX, mouseY);
            return;
        }
        MythicInventoryTabs.renderTooltip(
                context,
                textRenderer,
                panelX,
                panelY,
                WIDTH,
                mouseX,
                mouseY
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (MythicInventoryTabs.isOverInventoryTab(panelX, panelY, WIDTH, mouseX, mouseY)) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    MythicInventoryTabs.setScreenPreservingMouse(new InventoryScreen(client.player));
                }
                return true;
            }
            if (MythicInventoryTabs.isOverMythicCraftingTab(panelX, panelY, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenMythicCrafting();
                return true;
            }
            if (MythicInventoryTabs.isOverTravelingCompassTab(panelX, panelY, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenTravelingCompass();
                return true;
            }
            if (MythicInventoryTabs.isOverFossilCodexTab(panelX, panelY, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenFossilCodex();
                return true;
            }
            if (MythicInventoryTabs.isOverEatingCodexTab(panelX, panelY, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenEatingCodex();
                return true;
            }
            if (MythicInventoryTabs.isOverTitlesTab(panelX, panelY, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenTitles();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void changePage(int delta) {
        page = Math.floorMod(page + delta, pageCount());
    }

    private int normalPageCount() {
        return Math.max(1, (CARDS.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private int legendaryPageIndex() {
        return normalPageCount();
    }

    private int pageCount() {
        return normalPageCount() + 1;
    }

    private void drawLegendaryPage(DrawContext context, int mouseX, int mouseY) {
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.mythicrpg.fishing_codex.legendary_page"),
                panelX + WIDTH / 2,
                panelY + 30,
                0xFFE1B84B
        );

        SeaMonsterType[] types = SeaMonsterType.values();
        for (int index = 0; index < types.length; index++) {
            drawLegendaryCard(context, types[index], index, mouseX, mouseY);
        }
    }

    private void drawLegendaryCard(
            DrawContext context,
            SeaMonsterType type,
            int index,
            int mouseX,
            int mouseY
    ) {
        int cardWidth = 78;
        int cardHeight = 112;
        int cardX = panelX + 18 + index * 84;
        int cardY = panelY + 43;
        SeaMonsterProgressEntry entry = ClientSeaMonsterState.get(type);
        boolean defeated = entry.victories() > 0;

        VanillaContainerUi.drawInsetPanel(context, cardX, cardY, cardWidth, cardHeight);
        context.fill(cardX + 2, cardY + 2, cardX + cardWidth - 1, cardY + cardHeight - 1,
                defeated ? VanillaContainerUi.BACKGROUND : LOCKED_FILL);
        context.fill(cardX + 4, cardY + 4, cardX + cardWidth - 4, cardY + 7, legendaryColor(type));

        context.drawItem(new ItemStack(defeated ? type.material() : Items.SLIME_BALL), cardX + 31, cardY + 11);
        drawCenteredTrimmed(context, defeated ? type.displayName() : Text.translatable("screen.mythicrpg.fishing_codex.legendary_unknown"),
                cardX + cardWidth / 2, cardY + 31, cardWidth - 8);

        int gaugeX = cardX + 7;
        int gaugeY = cardY + 48;
        int gaugeWidth = cardWidth - 14;
        context.fill(gaugeX, gaugeY, gaugeX + gaugeWidth, gaugeY + 8, 0xFF3D3D3D);
        int filled = Math.round(gaugeWidth * entry.gauge() / 1000.0F);
        if (filled > 0) {
            context.fill(gaugeX, gaugeY, gaugeX + filled, gaugeY + 8, legendaryColor(type));
        }
        context.drawBorder(gaugeX, gaugeY, gaugeWidth, 8, 0xFFB0B0B0);
        VanillaContainerUi.drawCenteredSmallText(
                context, textRenderer,
                Text.translatable("screen.mythicrpg.fishing_codex.legendary_gauge",
                        String.format(java.util.Locale.ROOT, "%.1f", entry.gauge() / 10.0D)),
                cardX + cardWidth / 2, cardY + 59, VanillaContainerUi.TEXT, false
        );

        if (defeated) {
            drawCenteredTrimmed(
                    context,
                    Text.translatable("screen.mythicrpg.fishing_codex.legendary_victories", entry.victories()),
                    cardX + cardWidth / 2, cardY + 75, cardWidth - 8
            );
            drawCenteredTrimmed(
                    context,
                    Text.translatable("screen.mythicrpg.fishing_codex.legendary_day", entry.firstVictoryDay()),
                    cardX + cardWidth / 2, cardY + 87, cardWidth - 8
            );
        } else {
            drawCenteredTrimmed(
                    context,
                    Text.translatable("screen.mythicrpg.fishing_codex.legendary_weather." + type.weatherMode().name().toLowerCase(java.util.Locale.ROOT)),
                    cardX + cardWidth / 2, cardY + 77, cardWidth - 8
            );
            VanillaContainerUi.drawCenteredSmallText(
                    context, textRenderer,
                    Text.translatable("screen.mythicrpg.fishing_codex.legendary_hunt_hint"),
                    cardX + cardWidth / 2, cardY + 91, 0xFFB9B9B9, false
            );
        }

        if (VanillaContainerUi.isPointInside(mouseX, mouseY, cardX, cardY, cardWidth, cardHeight)) {
            pendingTooltip = legendaryTooltip(type, entry);
        }
    }

    private static int legendaryColor(SeaMonsterType type) {
        return switch (type) {
            case NESSIE -> 0xFF3D9B6A;
            case MEGALODON -> 0xFFB84545;
            case WHALE -> 0xFF478BC1;
        };
    }

    private static List<Text> legendaryTooltip(SeaMonsterType type, SeaMonsterProgressEntry entry) {
        ArrayList<Text> tooltip = new ArrayList<>();
        tooltip.add(type.displayName().copy().formatted(Formatting.GOLD));
        tooltip.add(Text.translatable(
                "screen.mythicrpg.fishing_codex.legendary_gauge",
                String.format(java.util.Locale.ROOT, "%.1f", entry.gauge() / 10.0D)
        ).formatted(Formatting.AQUA));
        tooltip.add(Text.translatable(
                "screen.mythicrpg.fishing_codex.legendary_weather."
                        + type.weatherMode().name().toLowerCase(java.util.Locale.ROOT)
        ).formatted(Formatting.GRAY));
        if (entry.victories() <= 0) {
            tooltip.add(Text.translatable("screen.mythicrpg.fishing_codex.legendary_locked").formatted(Formatting.DARK_GRAY));
            return List.copyOf(tooltip);
        }
        tooltip.add(Text.translatable("screen.mythicrpg.fishing_codex.legendary_victories", entry.victories()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("screen.mythicrpg.fishing_codex.legendary_day", entry.firstVictoryDay()).formatted(Formatting.DARK_GRAY));
        if (!entry.firstVictoryDimension().isBlank()) {
            tooltip.add(Text.translatable(
                    "screen.mythicrpg.fishing_codex.legendary_dimension",
                    entry.firstVictoryDimension()
            ).formatted(Formatting.DARK_GRAY));
        }
        tooltip.add(Text.translatable("screen.mythicrpg.fishing_codex.legendary_material", new ItemStack(type.material()).getName()).formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("screen.mythicrpg.fishing_codex.legendary_charm", new ItemStack(type.charm()).getName()).formatted(Formatting.LIGHT_PURPLE));
        tooltip.add(Text.translatable("screen.mythicrpg.fishing_codex.legendary_seal").formatted(Formatting.GOLD));
        return List.copyOf(tooltip);
    }

    private void drawCard(
            DrawContext context,
            CodexCard card,
            int index,
            int mouseX,
            int mouseY
    ) {
        int column = index % 4;
        int row = index / 4;
        int cardX = panelX + CARD_START_X + column * (CARD_WIDTH + CARD_GAP_X);
        int cardY = panelY + CARD_START_Y + row * (CARD_HEIGHT + CARD_GAP_Y);
        FishingCodexEntry entry = ClientFishingCodexState.get(card.family(), card.rarity());
        boolean unlocked = entry.captures() > 0;

        VanillaContainerUi.drawInsetPanel(context, cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
        context.fill(
                cardX + 2,
                cardY + 2,
                cardX + CARD_WIDTH - 1,
                cardY + CARD_HEIGHT - 1,
                unlocked ? VanillaContainerUi.BACKGROUND : LOCKED_FILL
        );

        int rarityColor = card.rarity().formatting().getColorValue() == null
                ? VanillaContainerUi.TEXT
                : 0xFF000000 | card.rarity().formatting().getColorValue();
        context.fill(cardX + 4, cardY + 4, cardX + CARD_WIDTH - 4, cardY + 7, rarityColor);

        if (unlocked) {
            context.drawItem(familyIcon(card.family()), cardX + 22, cardY + 9);
            drawCenteredTrimmed(
                    context,
                    card.family().displayName(),
                    cardX + CARD_WIDTH / 2,
                    cardY + 29,
                    CARD_WIDTH - 6
            );
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    card.rarity().displayName(),
                    cardX + CARD_WIDTH / 2,
                    cardY + 42,
                    VanillaContainerUi.TEXT,
                    false
            );
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.translatable("screen.mythicrpg.fishing_codex.count", entry.captures()),
                    cardX + CARD_WIDTH / 2,
                    cardY + 52,
                    VanillaContainerUi.TEXT,
                    false
            );
        } else {
            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("?"),
                    cardX + CARD_WIDTH / 2,
                    cardY + 10,
                    0xFFFFFFFF
            );
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    card.rarity().displayName(),
                    cardX + CARD_WIDTH / 2,
                    cardY + 30,
                    0xFFFFFFFF,
                    true
            );
            drawCenteredTrimmed(
                    context,
                    Text.translatable("screen.mythicrpg.fishing_codex.hint." + card.family().id()),
                    cardX + CARD_WIDTH / 2,
                    cardY + 45,
                    CARD_WIDTH - 6
            );
        }

        if (VanillaContainerUi.isPointInside(
                mouseX,
                mouseY,
                cardX,
                cardY,
                CARD_WIDTH,
                CARD_HEIGHT
        )) {
            pendingTooltip = unlocked
                    ? unlockedTooltip(card, entry)
                    : List.of(
                            Text.translatable("screen.mythicrpg.fishing_codex.unknown"),
                            card.family().displayName().copy().formatted(Formatting.GRAY),
                            Text.translatable(
                                    "screen.mythicrpg.fishing_codex.hint." + card.family().id()
                            ).formatted(Formatting.DARK_GRAY)
                    );
        }
    }

    private void drawCenteredTrimmed(
            DrawContext context,
            Text text,
            int centerX,
            int y,
            int width
    ) {
        String trimmed = textRenderer.trimToWidth(text.getString(), width);
        VanillaContainerUi.drawCenteredSmallText(
                context,
                textRenderer,
                Text.literal(trimmed),
                centerX,
                y,
                VanillaContainerUi.TEXT,
                false
        );
    }

    private static ItemStack familyIcon(FishingFamily family) {
        return new ItemStack(switch (family) {
            case SALMON -> Items.SALMON;
            case CRUSTACEAN -> Items.NAUTILUS_SHELL;
            case SHARK -> Items.PRISMARINE_SHARD;
            case INFERNAL -> Items.MAGMA_CREAM;
            case VOID -> Items.ENDER_PEARL;
        });
    }

    private static List<Text> unlockedTooltip(CodexCard card, FishingCodexEntry entry) {
        ArrayList<Text> tooltip = new ArrayList<>();
        tooltip.add(card.family().displayName());
        tooltip.add(card.rarity().displayName());
        tooltip.add(Text.translatable(
                "screen.mythicrpg.fishing_codex.count",
                entry.captures()
        ).formatted(Formatting.GRAY));
        if (!entry.firstBiome().isBlank()) {
            tooltip.add(Text.translatable(
                    "screen.mythicrpg.fishing_codex.first_biome",
                    entry.firstBiome()
            ).formatted(Formatting.GRAY));
        }
        if (!entry.firstDimension().isBlank()) {
            tooltip.add(Text.translatable(
                    "screen.mythicrpg.fishing_codex.first_dimension",
                    entry.firstDimension()
            ).formatted(Formatting.GRAY));
        }
        tooltip.add(Text.translatable(
                "screen.mythicrpg.fishing_codex.first_day",
                entry.firstDiscoveryDay()
        ).formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable(
                "screen.mythicrpg.fishing_codex.source." + entry.lastSource()
        ).formatted(Formatting.DARK_AQUA));
        return List.copyOf(tooltip);
    }

    private static List<CodexCard> createCards() {
        ArrayList<CodexCard> cards = new ArrayList<>(25);
        for (FishingFamily family : FishingFamily.values()) {
            for (FishingRarity rarity : FishingRarity.values()) {
                cards.add(new CodexCard(family, rarity));
            }
        }
        return List.copyOf(cards);
    }

    private record CodexCard(FishingFamily family, FishingRarity rarity) {
    }
}
