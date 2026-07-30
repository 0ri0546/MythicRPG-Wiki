package com.mythicrpg.client.traveling;

import com.mythicrpg.client.ClientSkillTreeState;
import com.mythicrpg.client.MythicClientPreferences;
import com.mythicrpg.core.SkillType;
import com.mythicrpg.traveling.TravelingDoubleJumpPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public final class TravelingDoubleJumpClient {

    private static final int MIN_AIR_TICKS_BEFORE_DOUBLE_JUMP = 4;

    private static boolean jumpWasPressed;
    private static int airborneTicks;

    private TravelingDoubleJumpClient() {
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        if (player == null) {
            jumpWasPressed = false;
            airborneTicks = 0;
            return;
        }

        if (!MythicClientPreferences.isDoubleJumpEnabled()) {
            // Keep the current physical key state so re-enabling the option while the key
            // is held cannot create an artificial new press.
            jumpWasPressed = client.options.jumpKey.isPressed();
            airborneTicks = 0;
            return;
        }

        boolean canRecharge = player.isOnGround()
                || player.isTouchingWater()
                || player.isClimbing()
                || player.hasVehicle()
                || player.getAbilities().flying;

        if (canRecharge) {
            airborneTicks = 0;
        } else {
            airborneTicks++;
        }

        boolean jumpPressed = client.options.jumpKey.isPressed();
        boolean newPress = jumpPressed && !jumpWasPressed;
        jumpWasPressed = jumpPressed;

        if (!newPress
                || airborneTicks < MIN_AIR_TICKS_BEFORE_DOUBLE_JUMP
                || !ClientSkillTreeState.isUnlocked(SkillType.TRAVELING, 1)
                || player.isOnGround()
                || player.isTouchingWater()
                || player.isSwimming()
                || player.isClimbing()
                || player.isFallFlying()
                || player.hasVehicle()
                || player.getAbilities().flying) {
            return;
        }

        ClientPlayNetworking.send(new TravelingDoubleJumpPayload());
    }
}
