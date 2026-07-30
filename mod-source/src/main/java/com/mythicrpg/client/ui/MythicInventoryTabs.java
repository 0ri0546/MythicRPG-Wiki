package com.mythicrpg.client.ui;

import com.mythicrpg.client.ClientSkillTreeState;
import com.mythicrpg.client.titles.TitleSelectionScreen;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.crafting.OpenMythicCraftingPayload;
import com.mythicrpg.traveling.OpenTravelingCompassPayload;
import com.mythicrpg.client.mining.FossilCodexScreen;
import com.mythicrpg.client.eating.EatingCodexScreen;
import com.mythicrpg.client.fishing.FishingCodexScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public final class MythicInventoryTabs {

    private static final int ACTIVE_FILL = VanillaContainerUi.BACKGROUND;
    private static final int INACTIVE_FILL = VanillaContainerUi.SHADOW;
    private static final int HOVER_OVERLAY = 0x22FFFFFF;
    private static final int ICON_ACTIVE = VanillaContainerUi.TEXT;
    private static final int ICON_INACTIVE = 0xFF666666;

    private static final int TAB_WIDTH = 30;
    private static final int TAB_HEIGHT = 22;
    private static final int TAB_GAP = 2;

    private static final int TAB_VISIBLE_WHEN_CLOSED = 7;
    private static final float TAB_ANIMATION_SPEED = 0.25F;

    private static float inventoryTabReveal;
    private static float mythicTabReveal;
    private static float compassTabReveal;
    private static float codexTabReveal;
    private static float eatingCodexTabReveal;
    private static float fishingCodexTabReveal;
    private static float titlesTabReveal;

    private MythicInventoryTabs() {
    }

    public static void renderInventoryTab(
            DrawContext context,
            int screenX,
            int screenY,
            int backgroundWidth,
            int mouseX,
            int mouseY,
            boolean active
    ) {
        boolean hovered = isOverInventoryTab(screenX, screenY, backgroundWidth, mouseX, mouseY);
        inventoryTabReveal = updateReveal(inventoryTabReveal, hovered);

        drawTab(
                context,
                screenX,
                getAnimatedTabX(screenX, backgroundWidth, inventoryTabReveal),
                getInventoryTabY(screenY),
                active,
                mouseX,
                mouseY,
                TabIcon.INVENTORY
        );
    }

    public static void renderMythicCraftingTab(
            DrawContext context,
            int screenX,
            int screenY,
            int backgroundWidth,
            int mouseX,
            int mouseY,
            boolean active
    ) {
        boolean hovered = isOverMythicCraftingTab(screenX, screenY, backgroundWidth, mouseX, mouseY);
        mythicTabReveal = updateReveal(mythicTabReveal, hovered);

        drawTab(
                context,
                screenX,
                getAnimatedTabX(screenX, backgroundWidth, mythicTabReveal),
                getMythicCraftingTabY(screenY),
                active,
                mouseX,
                mouseY,
                TabIcon.MYTHIC_CRAFTING
        );
    }

    public static void renderTravelingCompassTab(
            DrawContext context,
            int screenX,
            int screenY,
            int backgroundWidth,
            int mouseX,
            int mouseY,
            boolean active
    ) {
        boolean hovered = isOverTravelingCompassTab(
                screenX, screenY, backgroundWidth, mouseX, mouseY
        );
        compassTabReveal = updateReveal(compassTabReveal, hovered);

        drawTab(
                context,
                screenX,
                getAnimatedTabX(screenX, backgroundWidth, compassTabReveal),
                getTravelingCompassTabY(screenY),
                active,
                mouseX,
                mouseY,
                TabIcon.TRAVELING_COMPASS
        );
    }

    public static void renderFossilCodexTab(
            DrawContext context,
            int screenX,
            int screenY,
            int backgroundWidth,
            int mouseX,
            int mouseY,
            boolean active
    ) {
        boolean hovered = isOverFossilCodexTab(screenX, screenY, backgroundWidth, mouseX, mouseY);
        codexTabReveal = updateReveal(codexTabReveal, hovered);

        drawTab(
                context,
                screenX,
                getAnimatedTabX(screenX, backgroundWidth, codexTabReveal),
                getFossilCodexTabY(screenY),
                active,
                mouseX,
                mouseY,
                TabIcon.FOSSIL_CODEX
        );
    }

    public static void renderEatingCodexTab(
            DrawContext context,
            int screenX,
            int screenY,
            int backgroundWidth,
            int mouseX,
            int mouseY,
            boolean active
    ) {
        boolean hovered = isOverEatingCodexTab(screenX, screenY, backgroundWidth, mouseX, mouseY);
        eatingCodexTabReveal = updateReveal(eatingCodexTabReveal, hovered);

        drawTab(
                context,
                screenX,
                getAnimatedTabX(screenX, backgroundWidth, eatingCodexTabReveal),
                getEatingCodexTabY(screenY),
                active,
                mouseX,
                mouseY,
                TabIcon.EATING_CODEX
        );
    }


    public static void renderFishingCodexTab(
            DrawContext context,
            int screenX,
            int screenY,
            int backgroundWidth,
            int mouseX,
            int mouseY,
            boolean active
    ) {
        boolean hovered = isOverFishingCodexTab(
                screenX,
                screenY,
                backgroundWidth,
                mouseX,
                mouseY
        );
        fishingCodexTabReveal = updateReveal(fishingCodexTabReveal, hovered);

        drawTab(
                context,
                screenX,
                getAnimatedTabX(screenX, backgroundWidth, fishingCodexTabReveal),
                getFishingCodexTabY(screenY),
                active,
                mouseX,
                mouseY,
                TabIcon.FISHING_CODEX
        );
    }

    public static boolean isOverFishingCodexTab(
            int screenX,
            int screenY,
            int backgroundWidth,
            double mouseX,
            double mouseY
    ) {
        return isOverTab(
                screenX,
                screenY,
                backgroundWidth,
                getFishingCodexTabY(screenY),
                fishingCodexTabReveal,
                mouseX,
                mouseY
        );
    }

    public static void requestOpenFishingCodex() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ClientSkillTreeState.isUnlocked(SkillType.FISHING, 1)) {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.mythicrpg.fishing.codex_locked")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return;
        }
        setScreenPreservingMouse(new FishingCodexScreen());
    }

    public static void renderTitlesTab(
            DrawContext context,
            int screenX,
            int screenY,
            int backgroundWidth,
            int mouseX,
            int mouseY,
            boolean active
    ) {
        boolean hovered = isOverTitlesTab(screenX, screenY, backgroundWidth, mouseX, mouseY);
        titlesTabReveal = updateReveal(titlesTabReveal, hovered);

        drawTab(
                context,
                screenX,
                getAnimatedTabX(screenX, backgroundWidth, titlesTabReveal),
                getTitlesTabY(screenY),
                active,
                mouseX,
                mouseY,
                TabIcon.TITLES
        );
    }

    public static boolean isOverInventoryTab(
            int screenX,
            int screenY,
            int backgroundWidth,
            double mouseX,
            double mouseY
    ) {
        return isOverTab(
                screenX,
                screenY,
                backgroundWidth,
                getInventoryTabY(screenY),
                inventoryTabReveal,
                mouseX,
                mouseY
        );
    }

    public static boolean isOverMythicCraftingTab(
            int screenX,
            int screenY,
            int backgroundWidth,
            double mouseX,
            double mouseY
    ) {
        return isOverTab(
                screenX,
                screenY,
                backgroundWidth,
                getMythicCraftingTabY(screenY),
                mythicTabReveal,
                mouseX,
                mouseY
        );
    }

    public static boolean isOverTravelingCompassTab(
            int screenX,
            int screenY,
            int backgroundWidth,
            double mouseX,
            double mouseY
    ) {
        return isOverTab(
                screenX,
                screenY,
                backgroundWidth,
                getTravelingCompassTabY(screenY),
                compassTabReveal,
                mouseX,
                mouseY
        );
    }

    public static boolean isOverFossilCodexTab(
            int screenX,
            int screenY,
            int backgroundWidth,
            double mouseX,
            double mouseY
    ) {
        return isOverTab(
                screenX,
                screenY,
                backgroundWidth,
                getFossilCodexTabY(screenY),
                codexTabReveal,
                mouseX,
                mouseY
        );
    }

    public static boolean isOverEatingCodexTab(
            int screenX,
            int screenY,
            int backgroundWidth,
            double mouseX,
            double mouseY
    ) {
        return isOverTab(
                screenX,
                screenY,
                backgroundWidth,
                getEatingCodexTabY(screenY),
                eatingCodexTabReveal,
                mouseX,
                mouseY
        );
    }

    public static boolean isOverTitlesTab(
            int screenX,
            int screenY,
            int backgroundWidth,
            double mouseX,
            double mouseY
    ) {
        return isOverTab(
                screenX,
                screenY,
                backgroundWidth,
                getTitlesTabY(screenY),
                titlesTabReveal,
                mouseX,
                mouseY
        );
    }

    public static boolean hasPortableCraftingNode() {
        return ClientSkillTreeState.getUnlockedIds(SkillType.CRAFTING).contains(1);
    }

    public static boolean hasTravelingCompassNode() {
        return ClientSkillTreeState.getUnlockedIds(SkillType.TRAVELING).contains(9);
    }

    public static void requestOpenMythicCrafting() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!hasPortableCraftingNode()) {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.mythicrpg.perk_required", Text.translatable("skill_tree.mythicrpg.crafting.1.name"))
                                .formatted(Formatting.RED),
                        true
                );
            }
            return;
        }

        ClientPlayNetworking.send(new OpenMythicCraftingPayload(true));
    }

    public static void requestOpenFossilCodex() {
        setScreenPreservingMouse(new FossilCodexScreen());
    }

    public static void requestOpenEatingCodex() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ClientSkillTreeState.isUnlocked(SkillType.EATING, 1)) {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.mythicrpg.eating.codex_locked")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return;
        }
        setScreenPreservingMouse(new EatingCodexScreen());
    }

    public static void requestOpenTitles() {
        setScreenPreservingMouse(new TitleSelectionScreen());
    }

    public static void requestOpenTravelingCompass() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!hasTravelingCompassNode()) {
            if (client.player != null) {
                client.player.sendMessage(
                        Text.translatable("message.mythicrpg.compass.locked")
                                .formatted(Formatting.RED),
                        true
                );
            }
            return;
        }

        ClientPlayNetworking.send(new OpenTravelingCompassPayload());
    }

    private static boolean isOverTab(
            int screenX,
            int screenY,
            int backgroundWidth,
            int tabY,
            float reveal,
            double mouseX,
            double mouseY
    ) {
        int animatedX = getAnimatedTabX(screenX, backgroundWidth, reveal);
        int visibleMaxX = Math.min(screenX, animatedX + TAB_WIDTH);

        return mouseX >= animatedX
                && mouseX < visibleMaxX
                && mouseY >= tabY
                && mouseY < tabY + TAB_HEIGHT;
    }

    private static void drawTab(
            DrawContext context,
            int interfaceLeft,
            int x,
            int y,
            boolean active,
            int mouseX,
            int mouseY,
            TabIcon icon
    ) {
        boolean hovered = isInsideVisibleTab(x, y, interfaceLeft, mouseX, mouseY);
        int fill = active ? ACTIVE_FILL : INACTIVE_FILL;

        // Same one-pixel raised bevel as a vanilla container panel.
        context.fill(x, y, x + TAB_WIDTH, y + TAB_HEIGHT, VanillaContainerUi.OUTLINE);
        context.fill(x + 1, y + 1, x + TAB_WIDTH - 1, y + TAB_HEIGHT - 1, fill);
        context.fill(x + 1, y + 1, x + TAB_WIDTH - 2, y + 2, VanillaContainerUi.HIGHLIGHT);
        context.fill(x + 1, y + 1, x + 2, y + TAB_HEIGHT - 2, VanillaContainerUi.HIGHLIGHT);
        context.fill(x + 2, y + TAB_HEIGHT - 2, x + TAB_WIDTH - 1, y + TAB_HEIGHT - 1, VanillaContainerUi.DARK_SHADOW);
        context.fill(x + TAB_WIDTH - 2, y + 2, x + TAB_WIDTH - 1, y + TAB_HEIGHT - 1, VanillaContainerUi.DARK_SHADOW);

        if (hovered) {
            context.fill(x + 2, y + 2, x + TAB_WIDTH - 2, y + TAB_HEIGHT - 2, HOVER_OVERLAY);
        }

        drawIcon(context, x + 8, y + 5, icon, active ? ICON_ACTIVE : ICON_INACTIVE, fill);
    }

    private static void drawIcon(
            DrawContext context,
            int x,
            int y,
            TabIcon icon,
            int color,
            int cutoutColor
    ) {
        switch (icon) {
            case INVENTORY -> drawInventoryIcon(context, x, y + 1, color, cutoutColor);
            case MYTHIC_CRAFTING -> drawCraftingIcon(context, x, y + 1, color);
            case TRAVELING_COMPASS -> drawCompassIcon(context, x + 1, y, color, cutoutColor);
            case FOSSIL_CODEX -> drawCodexIcon(context, x + 1, y + 1, color, cutoutColor);
            case EATING_CODEX -> drawEatingCodexIcon(context, x + 1, y + 1, color, cutoutColor);
            case FISHING_CODEX -> drawFishingCodexIcon(context,x+1,y+1,color,cutoutColor);
            case TITLES -> drawTitlesIcon(context, x + 1, y + 1, color, cutoutColor);
        }
    }

    private static void drawInventoryIcon(DrawContext context, int x, int y, int color, int cutoutColor) {
        context.fill(x, y + 2, x + 14, y + 12, color);
        context.fill(x + 2, y, x + 12, y + 2, color);
        context.fill(x + 2, y + 4, x + 5, y + 7, cutoutColor);
        context.fill(x + 6, y + 4, x + 9, y + 7, cutoutColor);
        context.fill(x + 10, y + 4, x + 13, y + 7, cutoutColor);
    }

    private static void drawCraftingIcon(DrawContext context, int x, int y, int color) {
        context.fill(x, y, x + 5, y + 5, color);
        context.fill(x + 6, y, x + 11, y + 5, color);
        context.fill(x, y + 6, x + 5, y + 11, color);
        context.fill(x + 6, y + 6, x + 11, y + 11, color);
        context.fill(x + 14, y + 4, x + 18, y + 8, color);
    }

    private static void drawCompassIcon(DrawContext context, int x, int y, int color, int cutoutColor) {
        context.fill(x + 4, y, x + 10, y + 2, color);
        context.fill(x + 2, y + 2, x + 12, y + 4, color);
        context.fill(x, y + 4, x + 14, y + 10, color);
        context.fill(x + 2, y + 10, x + 12, y + 12, color);
        context.fill(x + 4, y + 12, x + 10, y + 14, color);
        context.fill(x + 6, y + 3, x + 8, y + 7, cutoutColor);
        context.fill(x + 8, y + 7, x + 10, y + 11, cutoutColor);
    }

    private static void drawCodexIcon(DrawContext context, int x, int y, int color, int cutoutColor) {
        context.fill(x, y + 1, x + 7, y + 13, color);
        context.fill(x + 8, y + 1, x + 15, y + 13, color);
        context.fill(x + 7, y + 2, x + 8, y + 14, cutoutColor);
        context.fill(x + 2, y + 4, x + 6, y + 5, cutoutColor);
        context.fill(x + 9, y + 4, x + 13, y + 5, cutoutColor);
    }

    private static void drawEatingCodexIcon(DrawContext context, int x, int y, int color, int cutoutColor) {
        // Small open recipe book with a bowl on the right page.
        context.fill(x, y + 1, x + 7, y + 13, color);
        context.fill(x + 8, y + 1, x + 15, y + 13, color);
        context.fill(x + 7, y + 2, x + 8, y + 14, cutoutColor);
        context.fill(x + 2, y + 4, x + 6, y + 5, cutoutColor);
        context.fill(x + 9, y + 8, x + 14, y + 10, cutoutColor);
        context.fill(x + 10, y + 6, x + 13, y + 8, cutoutColor);
    }

    private static void drawFishingCodexIcon(DrawContext context,int x,int y,int color,int cutoutColor){drawCodexIcon(context,x,y,color,cutoutColor);context.fill(x+9,y+7,x+14,y+9,cutoutColor);context.fill(x+12,y+5,x+14,y+11,cutoutColor);}

    private static void drawTitlesIcon(DrawContext context, int x, int y, int color, int cutoutColor) {
        // Compact badge/crown silhouette that remains legible at vanilla tab scale.
        context.fill(x + 1, y + 4, x + 14, y + 12, color);
        context.fill(x + 2, y + 1, x + 5, y + 5, color);
        context.fill(x + 6, y, x + 9, y + 5, color);
        context.fill(x + 10, y + 1, x + 13, y + 5, color);
        context.fill(x + 4, y + 7, x + 11, y + 9, cutoutColor);
    }

    private static boolean isInsideVisibleTab(
            int x,
            int y,
            int interfaceLeft,
            double mouseX,
            double mouseY
    ) {
        int visibleMaxX = Math.min(interfaceLeft, x + TAB_WIDTH);
        return mouseX >= x
                && mouseX < visibleMaxX
                && mouseY >= y
                && mouseY < y + TAB_HEIGHT;
    }

    private static int getClosedTabX(int screenX, int backgroundWidth) {
        return screenX - TAB_VISIBLE_WHEN_CLOSED;
    }

    private static int getOpenTabX(int screenX, int backgroundWidth) {
        return screenX - TAB_WIDTH + 2;
    }

    private static int getAnimatedTabX(int screenX, int backgroundWidth, float reveal) {
        int closedX = getClosedTabX(screenX, backgroundWidth);
        int openX = getOpenTabX(screenX, backgroundWidth);
        return Math.round(closedX + (openX - closedX) * reveal);
    }

    private static int getInventoryTabY(int screenY) {
        return screenY + 4;
    }

    private static int getMythicCraftingTabY(int screenY) {
        return getInventoryTabY(screenY) + TAB_HEIGHT + TAB_GAP;
    }

    private static int getTravelingCompassTabY(int screenY) {
        return getMythicCraftingTabY(screenY) + TAB_HEIGHT + TAB_GAP;
    }

    private static int getFossilCodexTabY(int screenY) {
        return getTravelingCompassTabY(screenY) + TAB_HEIGHT + TAB_GAP;
    }

    private static int getEatingCodexTabY(int screenY) {
        return getFossilCodexTabY(screenY) + TAB_HEIGHT + TAB_GAP;
    }

    private static int getFishingCodexTabY(int screenY) {
        return getEatingCodexTabY(screenY) + TAB_HEIGHT + TAB_GAP;
    }

    private static int getTitlesTabY(int screenY) {
        return getFishingCodexTabY(screenY) + TAB_HEIGHT + TAB_GAP;
    }

    private static float updateReveal(float current, boolean hovered) {
        float target = hovered ? 1.0F : 0.0F;

        if (current < target) {
            return Math.min(target, current + TAB_ANIMATION_SPEED);
        }
        if (current > target) {
            return Math.max(target, current - TAB_ANIMATION_SPEED);
        }
        return current;
    }

    public static void renderTooltip(
            DrawContext context,
            TextRenderer renderer,
            int screenX,
            int screenY,
            int backgroundWidth,
            int mouseX,
            int mouseY
    ) {
        Text tooltip = null;
        if (isOverInventoryTab(screenX, screenY, backgroundWidth, mouseX, mouseY)) {
            tooltip = Text.translatable("tooltip.mythicrpg.tab.inventory");
        } else if (isOverMythicCraftingTab(screenX, screenY, backgroundWidth, mouseX, mouseY)) {
            tooltip = Text.translatable("tooltip.mythicrpg.tab.crafting");
        } else if (isOverTravelingCompassTab(screenX, screenY, backgroundWidth, mouseX, mouseY)) {
            tooltip = Text.translatable("tooltip.mythicrpg.tab.compass");
        } else if (isOverFossilCodexTab(screenX, screenY, backgroundWidth, mouseX, mouseY)) {
            tooltip = Text.translatable("tooltip.mythicrpg.tab.codex");
        } else if (isOverEatingCodexTab(screenX, screenY, backgroundWidth, mouseX, mouseY)) {
            tooltip = Text.translatable("tooltip.mythicrpg.tab.eating_codex");
        } else if (isOverFishingCodexTab(screenX, screenY, backgroundWidth, mouseX, mouseY)) {
            tooltip = Text.translatable("tooltip.mythicrpg.tab.fishing_codex");
        } else if (isOverTitlesTab(screenX, screenY, backgroundWidth, mouseX, mouseY)) {
            tooltip = Text.translatable("tooltip.mythicrpg.tab.titles");
        }
        if (tooltip != null) {
            context.drawTooltip(renderer, tooltip, mouseX, mouseY);
        }
    }

    public static void setScreenPreservingMouse(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        long handle = client.getWindow().getHandle();
        double[] mouseX = new double[1];
        double[] mouseY = new double[1];

        GLFW.glfwGetCursorPos(handle, mouseX, mouseY);
        client.setScreen(screen);
        GLFW.glfwSetCursorPos(handle, mouseX[0], mouseY[0]);
    }

    private enum TabIcon {
        INVENTORY,
        MYTHIC_CRAFTING,
        TRAVELING_COMPASS,
        FOSSIL_CODEX,
        EATING_CODEX,
        FISHING_CODEX,
        TITLES
    }
}
