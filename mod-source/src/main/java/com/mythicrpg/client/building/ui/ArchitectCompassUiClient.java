package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.ArchitectCompassUiStatePayload;
import net.minecraft.client.MinecraftClient;

/** Opens or refreshes the Architect's Compass configuration screen. */
public final class ArchitectCompassUiClient {
    private ArchitectCompassUiClient() {
    }

    public static void handle(ArchitectCompassUiStatePayload state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof ArchitectCompassScreen screen
                && screen.accepts(state)) {
            screen.acceptState(state);
            return;
        }
        if (state.openScreen()) {
            BuildingUiSounds.open();
            client.setScreen(new ArchitectCompassScreen(state));
        }
    }
}
