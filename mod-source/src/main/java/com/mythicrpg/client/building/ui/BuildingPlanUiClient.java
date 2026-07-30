package com.mythicrpg.client.building.ui;

import com.mythicrpg.building.BuildingPlanUiStatePayload;
import net.minecraft.client.MinecraftClient;

/** Opens and refreshes Building structure screens from server-authoritative state. */
public final class BuildingPlanUiClient {
    private BuildingPlanUiClient() {
    }

    public static void handle(BuildingPlanUiStatePayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof AbstractBuildingPlanScreen screen) {
            if (screen.accepts(payload)) {
                screen.acceptState(payload);
                return;
            }
            if (!payload.openScreen()) {
                return;
            }
        } else if (client.currentScreen instanceof BuildingMiniatureScreen screen) {
            if (screen.accepts(payload)) {
                screen.acceptState(payload);
                return;
            }
            if (!payload.openScreen()) {
                return;
            }
        }
        if (!payload.openScreen()) {
            return;
        }
        BuildingUiSounds.open();
        if (payload.toolId() == BuildingPlanUiStatePayload.TOOL_2D) {
            client.setScreen(new BuildingPlan2DScreen(payload));
        } else if (payload.toolId() == BuildingPlanUiStatePayload.TOOL_3D) {
            client.setScreen(new BuildingPlan3DScreen(payload));
        } else if (payload.toolId() == BuildingPlanUiStatePayload.TOOL_MINIATURE) {
            client.setScreen(new BuildingMiniatureScreen(payload));
        }
    }
}
