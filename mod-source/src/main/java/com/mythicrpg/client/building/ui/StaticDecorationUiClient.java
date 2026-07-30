package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.StaticDecorationUiStatePayload;
import net.minecraft.client.MinecraftClient;

/** Opens or refreshes the particle generator configuration screen. */
public final class StaticDecorationUiClient {
    private StaticDecorationUiClient() {
    }

    public static void handle(StaticDecorationUiStatePayload state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof StaticDecorationScreen screen
                && screen.accepts(state)) {
            screen.acceptState(state);
            return;
        }
        if (state.openScreen()) {
            BuildingUiSounds.open();
            client.setScreen(new StaticDecorationScreen(state));
        }
    }
}
