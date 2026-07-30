package com.mythicrpg.client.eating;

import com.mythicrpg.client.ui.MythicInventoryTabs;
import com.mythicrpg.client.ui.VanillaContainerUi;
import com.mythicrpg.client.ui.VanillaCustomScreen;
import com.mythicrpg.core.ModItems;
import com.mythicrpg.eating.CookingRecipe;
import com.mythicrpg.eating.CookingRecipeRegistry;
import com.mythicrpg.eating.DishRarity;
import com.mythicrpg.eating.EatingCodexEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Culinary codex presented as a native MythicRPG inventory tab. */
public final class EatingCodexScreen extends VanillaCustomScreen {
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

    private final List<CookingRecipe> recipes = CookingRecipeRegistry.allCodexRecipes();
    private int page;
    private List<Text> pendingTooltip;

    public EatingCodexScreen() {
        super(Text.translatable("screen.mythicrpg.eating_codex"), WIDTH, HEIGHT);
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
        MythicInventoryTabs.renderEatingCodexTab(context, panelX, panelY, WIDTH, mouseX, mouseY, true);
        MythicInventoryTabs.renderFishingCodexTab(context, panelX, panelY, WIDTH, mouseX, mouseY, false
        );
        MythicInventoryTabs.renderTitlesTab(context, panelX, panelY, WIDTH, mouseX, mouseY, false);
    }

    @Override
    protected void renderVanillaContent(DrawContext context, int mouseX, int mouseY, float delta) {
        pendingTooltip = null;
        drawSection(context, panelX + 8, panelY + 24, WIDTH - 16, HEIGHT - 38, null);

        int start = page * PER_PAGE;
        for (int index = 0; index < PER_PAGE; index++) {
            int recipeIndex = start + index;
            if (recipeIndex >= recipes.size()) {
                break;
            }
            drawRecipeCard(context, recipes.get(recipeIndex), index, mouseX, mouseY);
        }

        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.translatable("screen.mythicrpg.eating_codex.page", page + 1, pageCount()),
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
            if (MythicInventoryTabs.isOverFishingCodexTab(panelX, panelY, WIDTH, mouseX, mouseY)) {
                MythicInventoryTabs.requestOpenFishingCodex();
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

    private int pageCount() {
        return Math.max(1, (recipes.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private void drawRecipeCard(
            DrawContext context,
            CookingRecipe recipe,
            int index,
            int mouseX,
            int mouseY
    ) {
        int column = index % 4;
        int row = index / 4;
        int cardX = panelX + CARD_START_X + column * (CARD_WIDTH + CARD_GAP_X);
        int cardY = panelY + CARD_START_Y + row * (CARD_HEIGHT + CARD_GAP_Y);
        EatingCodexEntry entry = ClientEatingCodexState.get(recipe.id());
        boolean unlocked = entry.preparations() > 0;

        VanillaContainerUi.drawInsetPanel(context, cardX, cardY, CARD_WIDTH, CARD_HEIGHT);
        context.fill(
                cardX + 2,
                cardY + 2,
                cardX + CARD_WIDTH - 1,
                cardY + CARD_HEIGHT - 1,
                unlocked ? VanillaContainerUi.BACKGROUND : LOCKED_FILL
        );

        if (unlocked) {
            DishRarity rarity = DishRarity.byRank(entry.bestRarityRank());
            int rarityColor = rarity.formatting().getColorValue() == null
                    ? VanillaContainerUi.TEXT
                    : 0xFF000000 | rarity.formatting().getColorValue();
            context.fill(cardX + 4, cardY + 4, cardX + CARD_WIDTH - 4, cardY + 7, rarityColor);
            context.drawItem(new ItemStack(ModItems.PREPARED_DISH), cardX + 22, cardY + 9);
            drawCenteredTrimmed(context, recipe.displayName(), cardX + CARD_WIDTH / 2, cardY + 29, CARD_WIDTH - 6);
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    rarity.displayName(),
                    cardX + CARD_WIDTH / 2,
                    cardY + 42,
                    VanillaContainerUi.TEXT,
                    false
            );
            VanillaContainerUi.drawCenteredSmallText(
                    context,
                    textRenderer,
                    Text.translatable("screen.mythicrpg.eating_codex.count", entry.preparations()),
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
                    Text.translatable("screen.mythicrpg.eating_codex.unknown"),
                    cardX + CARD_WIDTH / 2,
                    cardY + 29,
                    0xFFFFFFFF,
                    true
            );
            drawCenteredTrimmed(context, hintText(recipe), cardX + CARD_WIDTH / 2, cardY + 45, CARD_WIDTH - 6);
        }

        if (VanillaContainerUi.isPointInside(mouseX, mouseY, cardX, cardY, CARD_WIDTH, CARD_HEIGHT)) {
            pendingTooltip = unlocked ? unlockedTooltip(recipe, entry) : unknownTooltip(recipe);
        }
    }

    private void drawCenteredTrimmed(DrawContext context, Text text, int centerX, int y, int width) {
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

    private List<Text> unlockedTooltip(CookingRecipe recipe, EatingCodexEntry entry) {
        ArrayList<Text> tooltip = new ArrayList<>();
        tooltip.add(recipe.displayName());
        tooltip.add(recipe.category().displayName().copy().formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(
                "screen.mythicrpg.eating_codex.best_rarity",
                DishRarity.byRank(entry.bestRarityRank()).displayName()
        ));
        tooltip.add(Text.translatable(
                "screen.mythicrpg.eating_codex.portions",
                Math.max(1, entry.lastPortions())
        ).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(
                "screen.mythicrpg.eating_codex.shelf_life",
                Math.max(1, entry.lastShelfLifeDays())
        ).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable(
                "screen.mythicrpg.eating_codex.saturation",
                String.format(Locale.ROOT, "%.1f", DishRarity.byRank(entry.bestRarityRank()).saturation())
        ).formatted(Formatting.GOLD));
        tooltip.add(Text.translatable("screen.mythicrpg.eating_codex.ingredients")
                .formatted(Formatting.DARK_AQUA));
        for (CookingRecipe.IngredientRequirement ingredient : recipe.ingredients()) {
            tooltip.add(Text.literal("• ").append(
                    ingredient.item() == net.minecraft.item.Items.AIR
                            ? hintName(ingredient.hintId())
                            : ingredient.item().getDefaultStack().getName()
            ).formatted(Formatting.GRAY));
        }
        return List.copyOf(tooltip);
    }

    private List<Text> unknownTooltip(CookingRecipe recipe) {
        return List.of(
                Text.translatable("screen.mythicrpg.eating_codex.unknown"),
                hintText(recipe).copy().formatted(Formatting.GRAY)
        );
    }

    private Text hintText(CookingRecipe recipe) {
        MutableText result = Text.empty();
        List<String> hints = recipe.categoryHints();
        for (int index = 0; index < hints.size(); index++) {
            if (index > 0) {
                result.append(Text.literal(" + "));
            }
            result.append(hintName(hints.get(index)));
        }
        return result;
    }

    private static Text hintName(String hintId) {
        return Text.translatable("food_category.mythicrpg." + hintId);
    }
}
