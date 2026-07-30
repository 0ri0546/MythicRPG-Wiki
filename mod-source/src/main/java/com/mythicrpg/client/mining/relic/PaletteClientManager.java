package com.mythicrpg.client.mining.relic;

import com.mythicrpg.core.ModItems;
import com.mythicrpg.mining.archaeology.relic.FossilPaletteItem;
import com.mythicrpg.mining.archaeology.relic.PaletteSelectionManager;
import com.mythicrpg.mining.archaeology.relic.PaletteSelectionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public final class PaletteClientManager {
    private static final Identifier VANILLA_HOTBAR_TEXTURE = Identifier.ofVanilla("textures/gui/sprites/hud/hotbar.png");
    private static final Identifier VANILLA_HOTBAR_SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
    private static final int VANILLA_HOTBAR_TEXTURE_WIDTH = 182;
    private static final int VANILLA_HOTBAR_TEXTURE_HEIGHT = 22;

    private PaletteClientManager() {}

    public static boolean scroll(double vertical) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return false;
        ItemStack palette = client.player.getOffHandStack();
        if (!palette.isOf(ModItems.FOSSIL_PALETTE)) {
            setSelected(client, -1);
            return false;
        }
        int extra = FossilPaletteItem.slots(palette);
        int currentExtra = PaletteSelectionManager.selected(client.player);
        int logical = currentExtra >= 0 ? 9 + currentExtra : client.player.getInventory().selectedSlot;
        int direction = vertical > 0 ? -1 : 1;
        int next = Math.floorMod(logical + direction, 9 + extra);
        if (next < 9) {
            client.player.getInventory().selectedSlot = next;
            setSelected(client, -1);
        } else {
            setSelected(client, next - 9);
        }
        return true;
    }

    private static void setSelected(MinecraftClient client, int index) {
        if (client.player == null) return;
        PaletteSelectionManager.select(client.player, index);
        ClientPlayNetworking.send(new PaletteSelectionPayload(index));
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) return;

        // L'inventaire de la Palette devient éditable dans l'écran d'inventaire.
        // On libère donc le stack virtuel avant toute manipulation de slot pour
        // éviter que le cache de la main principale réécrive un ancien contenu.
        if (client.currentScreen != null
                || !client.player.getOffHandStack().isOf(ModItems.FOSSIL_PALETTE)) {
            if (PaletteSelectionManager.selected(client.player) >= 0) {
                setSelected(client, -1);
            }
        }
    }

    public static void render(DrawContext context, net.minecraft.client.render.RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        ItemStack palette = client.player.getOffHandStack();
        if (!palette.isOf(ModItems.FOSSIL_PALETTE)) {
            return;
        }

        int slotCount = FossilPaletteItem.slots(palette);
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int hotbarRight = screenWidth / 2 + 91;
        int horizontalWidth = slotCount * 20 + 2;
        int selected = PaletteSelectionManager.selected(client.player);
        var contents = FossilPaletteItem.read(palette);

        int preferredX = hotbarRight + 6;
        boolean horizontal = preferredX + horizontalWidth <= screenWidth - 2;
        int columns = horizontal ? slotCount : 2;
        int rows = horizontal ? 1 : (slotCount + 1) / 2;
        int panelWidth = columns * 20 + 2;
        int panelHeight = rows * 20 + 2;
        int x = horizontal ? preferredX : screenWidth - panelWidth - 2;
        int y = horizontal
                ? screenHeight - 22
                : screenHeight - 22 - (rows - 1) * 20;

        if (horizontal) {
            drawHorizontalVanillaHotbar(context, x, y, slotCount);
        } else {
            // Compact fallback for very small resolutions. It keeps the same
            // dark HUD language without overlapping health or hunger.
            context.fill(x, y, x + panelWidth, y + panelHeight, 0x90000000);
            context.drawBorder(x, y, panelWidth, panelHeight, 0xFF2A2A2A);
        }

        for (int index = 0; index < slotCount; index++) {
            int column = horizontal ? index : index % 2;
            int row = horizontal ? 0 : index / 2;
            int slotX = horizontal ? x + 3 + index * 20 : x + 2 + column * 20;
            int slotY = horizontal ? y + 3 : y + 2 + row * 20;

            if (!horizontal) {
                context.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0x80202020);
            }
            if (selected == index) {
                if (horizontal) {
                    context.drawGuiTexture(
                            VANILLA_HOTBAR_SELECTION,
                            x - 1 + index * 20,
                            y - 1,
                            24,
                            23
                    );
                } else {
                    context.drawBorder(slotX - 2, slotY - 2, 20, 20, 0xFFFFFFFF);
                    context.drawBorder(slotX - 1, slotY - 1, 18, 18, 0xFF8B8B8B);
                }
            }

            ItemStack stack = contents.get(index);
            context.drawItem(stack, slotX, slotY);
            context.drawItemInSlot(client.textRenderer, stack, slotX, slotY);
        }
    }
    private static void drawHorizontalVanillaHotbar(
            DrawContext context,
            int x,
            int y,
            int slotCount
    ) {
        context.drawTexture(
                VANILLA_HOTBAR_TEXTURE,
                x,
                y,
                0.0F,
                0.0F,
                1,
                VANILLA_HOTBAR_TEXTURE_HEIGHT,
                VANILLA_HOTBAR_TEXTURE_WIDTH,
                VANILLA_HOTBAR_TEXTURE_HEIGHT
        );
        for (int index = 0; index < slotCount; index++) {
            context.drawTexture(
                    VANILLA_HOTBAR_TEXTURE,
                    x + 1 + index * 20,
                    y,
                    1.0F,
                    0.0F,
                    20,
                    VANILLA_HOTBAR_TEXTURE_HEIGHT,
                    VANILLA_HOTBAR_TEXTURE_WIDTH,
                    VANILLA_HOTBAR_TEXTURE_HEIGHT
            );
        }
        context.drawTexture(
                VANILLA_HOTBAR_TEXTURE,
                x + 1 + slotCount * 20,
                y,
                181.0F,
                0.0F,
                1,
                VANILLA_HOTBAR_TEXTURE_HEIGHT,
                VANILLA_HOTBAR_TEXTURE_WIDTH,
                VANILLA_HOTBAR_TEXTURE_HEIGHT
        );
    }

}
