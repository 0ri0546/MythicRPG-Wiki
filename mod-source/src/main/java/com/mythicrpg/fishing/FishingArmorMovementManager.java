package com.mythicrpg.fishing;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class FishingArmorMovementManager {
    private FishingArmorMovementManager() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                tick(player);
            }
        });
    }

    private static void tick(ServerPlayerEntity player) {
        int sum = 0;
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (player.getEquippedStack(slot).getItem() instanceof FishingScaleArmorItem item) {
                sum += item.rarity().rank() + 1;
                count++;
            }
        }
        if (count != 4 || (!player.isTouchingWater() && !player.isInLava())) {
            return;
        }

        int level = Math.max(1, Math.min(5, sum / 4));
        double multiplier = 1.0D + level * 0.20D;
        Vec3d velocity = player.getVelocity();
        double horizontal = Math.hypot(velocity.x, velocity.z);
        if (horizontal < 0.005D) {
            return;
        }

        // Small bounded acceleration toward a rarity-dependent cap. This avoids exponential velocity growth.
        double baseCap = player.isInLava() ? 0.24D : 0.34D;
        double targetCap = baseCap * multiplier;
        if (horizontal >= targetCap) {
            return;
        }
        double factor = Math.min(1.035D + level * 0.005D, targetCap / horizontal);
        player.setVelocity(velocity.x * factor, velocity.y, velocity.z * factor);
        player.velocityModified = true;
    }
}
